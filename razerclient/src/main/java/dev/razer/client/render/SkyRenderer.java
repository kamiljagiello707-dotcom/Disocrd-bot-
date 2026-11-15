package dev.razer.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.razer.client.RazerClient;
import dev.razer.client.module.Sky;
import dev.razer.client.ui.Theme;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Własne niebo: gradient, gwiazdy, mgławice, zorza polarna i spadające gwiazdy. */
public final class SkyRenderer {
    private static final Identifier GLOW = Identifier.of("razerclient", "textures/ui/glow_add.png");
    private static final Identifier NEBULA = Identifier.of("razerclient", "textures/ui/nebula_add.png");
    private static final float R = 60f;
    private static float[][] stars = new float[0][];
    private static int starsFor = -1;
    private static final List<float[]> shooting = new ArrayList<>();
    private static long nextShoot = 0;
    private static final Random RNG = new Random();

    private SkyRenderer() {}

    public static Sky module() {
        RazerClient rc = RazerClient.get();
        if (rc == null || rc.modules == null) return null;
        return rc.modules.get("Sky") instanceof Sky s && s.isEnabled() ? s : null;
    }

    /** czy podmieniamy niebo w tej chwili */
    public static boolean active(Camera camera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return module() != null && mc.world != null && mc.world.getRegistryKey() == World.OVERWORLD
                && camera.getSubmersionType() == CameraSubmersionType.NONE;
    }

    // ------------------------------------------------------------------ palety
    // {zenit, środek, horyzont, mgławica1, mgławica2, zorza dół, zorza góra}
    private static int[] palette(Sky s) {
        return switch (s.preset.get()) {
            case "Galaxy" -> new int[]{0xFF07030F, 0xFF170A2E, 0xFF2A1450, 0xFF9B4DFF, 0xFFFF4DB8, 0xFF7A5CFF, 0xFFFF5CC8};
            case "Sunset" -> new int[]{0xFF15183F, 0xFF6B3577, 0xFFFF8A4C, 0xFFFF6B9E, 0xFFFFB36B, 0xFFFF7AB8, 0xFF8A6BFF};
            case "Midnight" -> new int[]{0xFF020409, 0xFF060D1F, 0xFF0F1D38, 0xFF3A5BFF, 0xFF6B8BFF, 0xFF4DA6FF, 0xFF9B7BFF};
            case "Accent" -> {
                int a = Theme.accent(), b = Theme.accent2();
                yield new int[]{R2D.blend(0xFF000000, a, 0.06f), R2D.blend(0xFF000000, a, 0.2f), R2D.blend(0xFF000000, a, 0.42f), a, b, a, b};
            }
            default -> new int[]{0xFF040914, 0xFF0A1A30, 0xFF16344D, 0xFF2ED1B0, 0xFF6A5CFF, 0xFF3CFFB0, 0xFF8A5CFF};
        };
    }

    public static int horizonColor() {
        Sky s = module();
        if (s == null) return 0;
        return dim(R2D.blend(palette(s)[2], 0xFFFFFFFF, 0.04f), s);
    }

    private static int dim(int c, Sky s) {
        float k = s.brightness.get().floatValue() / 100f;
        int r = Math.min(255, (int) (((c >> 16) & 0xFF) * k)), g = Math.min(255, (int) (((c >> 8) & 0xFF) * k)), b = Math.min(255, (int) ((c & 0xFF) * k));
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static Vector3f dir(double az, double elevSin) {
        double c = Math.sqrt(Math.max(0, 1 - elevSin * elevSin));
        return new Vector3f((float) (Math.cos(az) * c), (float) elevSin, (float) (Math.sin(az) * c));
    }

    // ------------------------------------------------------------------ render

    public static void render(Matrix4f viewRotation) {
        Sky s = module();
        if (s == null) return;
        int[] pal = palette(s);
        for (int i = 0; i < pal.length; i++) pal[i] = dim(pal[i], s);
        float t = (System.currentTimeMillis() % 10_000_000L) / 1000f * s.speed.get().floatValue();

        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.set(viewRotation);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f id = new Matrix4f();

        drawDome(id, pal);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        if (s.preset.get().equals("Sunset")) glow(id, dir(Math.PI * 0.5, 0.06), 34, R2D.withAlpha(0xFFFFB36B, 0.55f));
        if (s.preset.get().equals("Midnight")) { glow(id, dir(-Math.PI * 0.3, 0.55), 10, R2D.withAlpha(0xFFDCE6FF, 0.9f)); glow(id, dir(-Math.PI * 0.3, 0.55), 26, R2D.withAlpha(0xFF8FA8FF, 0.25f)); }
        if (s.nebula.get()) drawNebula(id, pal, t, s.preset.get().equals("Galaxy"));
        if (s.stars.get()) drawStars(id, s, t);
        if (s.aurora.get()) drawAurora(id, pal, t);
        if (s.shootingStars.get()) drawShooting(id);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static int skyColor(int[] pal, float e) {
        if (e < 0) return R2D.blend(pal[2], R2D.blend(pal[2], 0xFF000000, 0.5f), Math.min(1, -e * 3));
        if (e < 0.35f) return R2D.blend(pal[2], pal[1], e / 0.35f);
        return R2D.blend(pal[1], pal[0], (e - 0.35f) / 0.65f);
    }

    private static void drawDome(Matrix4f m, int[] pal) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        int rings = 18, seg = 36;
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int r = 0; r < rings; r++) {
            float e1 = -1 + 2f * r / rings, e2 = -1 + 2f * (r + 1) / rings;
            int c1 = skyColor(pal, e1), c2 = skyColor(pal, e2);
            for (int i = 0; i < seg; i++) {
                double a1 = Math.PI * 2 * i / seg, a2 = Math.PI * 2 * (i + 1) / seg;
                Vector3f p1 = dir(a1, e1).mul(R), p2 = dir(a2, e1).mul(R), p3 = dir(a2, e2).mul(R), p4 = dir(a1, e2).mul(R);
                b.vertex(m, p1.x, p1.y, p1.z).color(c1);
                b.vertex(m, p2.x, p2.y, p2.z).color(c1);
                b.vertex(m, p3.x, p3.y, p3.z).color(c2);
                b.vertex(m, p4.x, p4.y, p4.z).color(c2);
            }
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    /** miękki punkt światła na sferze nieba */
    private static void glow(Matrix4f m, Vector3f d, float size, int color) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, GLOW);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        quadOnSphere(b, m, d, size, color);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    /** kolor do mieszania addytywnego (ONE, ONE): jasność w RGB, alfa = 1 – shader nie odrzuca pikseli */
    private static int add(int c) {
        float a = ((c >>> 24) & 0xFF) / 255f;
        return 0xFF000000 | ((int) (((c >> 16) & 0xFF) * a) << 16) | ((int) (((c >> 8) & 0xFF) * a) << 8) | (int) ((c & 0xFF) * a);
    }

    private static void quadOnSphere(BufferBuilder b, Matrix4f m, Vector3f d, float size, int color) {
        color = add(color);
        Vector3f c = new Vector3f(d).normalize().mul(R * 0.98f);
        Vector3f up = Math.abs(d.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(d).cross(up).normalize().mul(size / 2f);
        Vector3f v = new Vector3f(u).cross(d).normalize().mul(size / 2f);
        b.vertex(m, c.x - u.x - v.x, c.y - u.y - v.y, c.z - u.z - v.z).texture(0, 0).color(color);
        b.vertex(m, c.x + u.x - v.x, c.y + u.y - v.y, c.z + u.z - v.z).texture(1, 0).color(color);
        b.vertex(m, c.x + u.x + v.x, c.y + u.y + v.y, c.z + u.z + v.z).texture(1, 1).color(color);
        b.vertex(m, c.x - u.x + v.x, c.y - u.y + v.y, c.z - u.z + v.z).texture(0, 1).color(color);
    }

    private static void buildStars(int amount) {
        Random r = new Random(1337);
        int n = 8 * amount;
        List<float[]> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            float e = r.nextFloat() * 1.1f - 0.1f;
            l.add(new float[]{(float) (r.nextFloat() * Math.PI * 2), e, 0.18f + r.nextFloat() * r.nextFloat() * 0.55f, r.nextFloat() * 6.28f, 0});
        }
        // pas Drogi Mlecznej (tylko Galaxy – oznaczone 1)
        for (int i = 0; i < n; i++) {
            double a = r.nextFloat() * Math.PI * 2;
            Vector3f d = new Vector3f((float) Math.cos(a), (float) (r.nextGaussian() * 0.08), (float) Math.sin(a));
            // obrót pasa o 55°
            float y = d.y * (float) Math.cos(0.96) - d.z * (float) Math.sin(0.96), z = d.y * (float) Math.sin(0.96) + d.z * (float) Math.cos(0.96);
            float az = (float) Math.atan2(z, d.x);
            float len = (float) Math.sqrt(d.x * d.x + y * y + z * z);
            l.add(new float[]{az, y / len, 0.12f + r.nextFloat() * 0.3f, r.nextFloat() * 6.28f, 1});
        }
        stars = l.toArray(new float[0][]);
    }

    private static void drawStars(Matrix4f m, Sky s, float t) {
        int amount = s.starAmount.get().intValue();
        if (starsFor != amount) { buildStars(amount); starsFor = amount; }
        boolean galaxy = s.preset.get().equals("Galaxy");
        float sunsetK = s.preset.get().equals("Sunset") ? 1 : 0;
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, GLOW);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (float[] st : stars) {
            if (st[4] == 1 && !galaxy) continue;
            if (sunsetK > 0 && st[1] < 0.45f) continue;
            float tw = 0.55f + 0.45f * (float) Math.sin(t * 2.2f + st[3] * 3);
            float horizonFade = Math.min(1, Math.max(0, (st[1] + 0.05f) * 5));
            int col = st[4] == 1 ? 0xFFE6D9FF : (st[3] > 5.5f ? 0xFFBFD4FF : st[3] < 0.6f ? 0xFFFFE2C2 : 0xFFFFFFFF);
            quadOnSphere(b, m, dir(st[0], st[1]), st[2] * 1.6f, R2D.withAlpha(col, tw * horizonFade));
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static void drawNebula(Matrix4f m, int[] pal, float t, boolean galaxy) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, NEBULA);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Random r = new Random(42);
        int n = galaxy ? 26 : 10;
        for (int i = 0; i < n; i++) {
            double az; float e;
            if (galaxy) { // wzdłuż pasa galaktyki
                double a = (double) i / n * Math.PI * 2 + r.nextFloat() * 0.2;
                float y0 = (float) (r.nextGaussian() * 0.05);
                float y = y0 * (float) Math.cos(0.96) - (float) Math.sin(a) * (float) Math.sin(0.96);
                float z = y0 * (float) Math.sin(0.96) + (float) Math.sin(a) * (float) Math.cos(0.96);
                az = Math.atan2(z, Math.cos(a)); e = y / (float) Math.sqrt(Math.cos(a) * Math.cos(a) + y * y + z * z);
            } else { az = r.nextFloat() * Math.PI * 2; e = 0.3f + r.nextFloat() * 0.6f; }
            float pulse = 0.8f + 0.2f * (float) Math.sin(t * 0.4f + i);
            int c = i % 2 == 0 ? pal[3] : pal[4];
            quadOnSphere(b, m, dir(az + t * 0.004, e), (galaxy ? 22 : 30) + r.nextFloat() * 18, R2D.withAlpha(c, (galaxy ? 0.2f : 0.12f) * pulse));
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static void drawAurora(Matrix4f m, int[] pal, float t) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        float[][] ribbons = {{0.2f, 0.18f, 0.9f}, {1.4f, 0.24f, 0.7f}, {2.9f, 0.15f, 0.55f}};
        for (float[] rb : ribbons) {
            int seg = 70;
            double span = 1.9;
            for (int layer = 0; layer < 2; layer++) {
                BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
                for (int i = 0; i <= seg; i++) {
                    float k = (float) i / seg;
                    double az = rb[0] + (k - 0.5) * span + 0.12 * Math.sin(t * 0.25 + k * 6);
                    float base = rb[1] + 0.05f * (float) Math.sin(k * 9 + t * 0.6f);
                    float height = 0.28f + 0.09f * (float) Math.sin(k * 13 + t * 0.9f);
                    float shimmer = (0.55f + 0.45f * (float) Math.sin(k * 30 + t * 2.4f)) * rb[2];
                    float edge = (float) Math.sin(k * Math.PI); // zanik na końcach
                    float a = shimmer * edge;
                    float eLow = layer == 0 ? base : base + height * 0.35f;
                    float eHigh = layer == 0 ? base + height * 0.35f : base + height;
                    int cLow = layer == 0 ? R2D.withAlpha(pal[5], a * 0.25f) : R2D.withAlpha(R2D.blend(pal[5], pal[6], 0.35f), a * 0.75f);
                    int cHigh = layer == 0 ? R2D.withAlpha(R2D.blend(pal[5], pal[6], 0.35f), a * 0.75f) : R2D.withAlpha(pal[6], 0f);
                    Vector3f lo = dir(az, eLow).mul(R * 0.95f), hi = dir(az, eHigh).mul(R * 0.95f);
                    b.vertex(m, lo.x, lo.y, lo.z).color(add(cLow));
                    b.vertex(m, hi.x, hi.y, hi.z).color(add(cHigh));
                }
                BufferRenderer.drawWithGlobalProgram(b.end());
            }
        }
    }

    private static void drawShooting(Matrix4f m) {
        long now = System.currentTimeMillis();
        if (now > nextShoot) {
            double az = RNG.nextDouble() * Math.PI * 2;
            float e = 0.35f + RNG.nextFloat() * 0.4f;
            shooting.add(new float[]{(float) az, e, (RNG.nextBoolean() ? 1 : -1) * (0.5f + RNG.nextFloat() * 0.4f), -0.25f - RNG.nextFloat() * 0.2f, now});
            nextShoot = now + 2500 + RNG.nextInt(5000);
        }
        shooting.removeIf(s -> now - s[4] > 900);
        if (shooting.isEmpty()) return;
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (float[] s : shooting) {
            float k = (now - s[4]) / 900f;
            float fade = (float) Math.sin(k * Math.PI);
            Vector3f head = dir(s[0] + s[2] * k, s[1] + s[3] * k).mul(R * 0.9f);
            Vector3f tail = dir(s[0] + s[2] * (k - 0.18f), s[1] + s[3] * (k - 0.18f)).mul(R * 0.9f);
            Vector3f dirv = new Vector3f(head).sub(tail);
            Vector3f side = new Vector3f(dirv).cross(head).normalize().mul(0.12f);
            int ch = add(R2D.withAlpha(0xFFFFFFFF, fade)), ct = 0xFF000000;
            b.vertex(m, head.x - side.x, head.y - side.y, head.z - side.z).color(ch);
            b.vertex(m, head.x + side.x, head.y + side.y, head.z + side.z).color(ch);
            b.vertex(m, tail.x + side.x * 0.2f, tail.y + side.y * 0.2f, tail.z + side.z * 0.2f).color(ct);
            b.vertex(m, tail.x - side.x * 0.2f, tail.y - side.y * 0.2f, tail.z - side.z * 0.2f).color(ct);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }
}
