package dev.with.client.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.with.client.render.R2D;
import dev.with.client.render.WorldFx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/** CustomSky: własne niebo (Night, Aurora, Galaxy, Sunset, Mono) rysowane zamiast nieba z gry w Overworldzie. */
public final class Sky {
    private Sky() {}

    private static final Identifier DOT = R2D.id("textures/fx/dot.png");
    private static final float R = 100;
    private static final int STARS = 1400, BAND = 1600, NEB = 46;
    private static final float[][] star = new float[STARS + BAND][];   // x,y,z, ux,uy,uz, vx,vy,vz, size, phase, bright
    private static final float[][] neb = new float[NEB][];
    static {
        Random r = new Random(1337);
        for (int i = 0; i < STARS; i++) star[i] = make(rndDir(r), r, 0.12f + r.nextFloat() * r.nextFloat() * 0.45f);
        // pasmo "drogi mlecznej" – płaszczyzna pochylona względem horyzontu
        Vector3f a = new Vector3f(1, 0.15f, 0.2f).normalize(), n = new Vector3f(0.25f, 0.55f, -0.8f).normalize();
        Vector3f b = new Vector3f(n).cross(a).normalize();
        for (int i = 0; i < BAND; i++) {
            double t = r.nextDouble() * Math.PI * 2;
            float spread = (float) (r.nextGaussian() * 0.09);
            Vector3f d = new Vector3f(a).mul((float) Math.cos(t)).add(new Vector3f(b).mul((float) Math.sin(t))).add(new Vector3f(n).mul(spread)).normalize();
            star[STARS + i] = make(d, r, 0.08f + r.nextFloat() * 0.22f);
        }
        for (int i = 0; i < NEB; i++) {
            double t = r.nextDouble() * Math.PI * 2;
            float spread = (float) (r.nextGaussian() * 0.07);
            Vector3f d = new Vector3f(a).mul((float) Math.cos(t)).add(new Vector3f(b).mul((float) Math.sin(t))).add(new Vector3f(n).mul(spread)).normalize();
            float[] s = make(d, r, 10 + r.nextFloat() * 18);
            s[11] = r.nextInt(3);
            neb[i] = s;
        }
    }

    private static Vector3f rndDir(Random r) {
        double z = r.nextDouble() * 2 - 1, t = r.nextDouble() * Math.PI * 2, q = Math.sqrt(1 - z * z);
        return new Vector3f((float) (q * Math.cos(t)), (float) z, (float) (q * Math.sin(t)));
    }

    private static float[] make(Vector3f d, Random r, float size) {
        Vector3f up = Math.abs(d.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(d).cross(up).normalize(), v = new Vector3f(d).cross(u).normalize();
        return new float[]{d.x * R, d.y * R, d.z * R, u.x, u.y, u.z, v.x, v.y, v.z, size, r.nextFloat() * 6.28f, 0.45f + r.nextFloat() * 0.55f};
    }

    // ------------------------------------------------------------------ paleta

    /** {góra, środek, horyzont} */
    private static int[] palette(String s) {
        return switch (s) {
            case "Aurora" -> new int[]{0x02050B, 0x041620, 0x0B3440};
            case "Galaxy" -> new int[]{0x060210, 0x170931, 0x35134D};
            case "Sunset" -> new int[]{0x1A1546, 0x7E3A78, 0xFF8A5C};
            case "Mono" -> new int[]{0x040405, 0x111114, 0x34343A};
            default -> new int[]{0x03040C, 0x0A0F2C, 0x1C2858};
        };
    }

    private static String style() { return V.mode("CustomSky", "Niebo", "Galaxy"); }

    public static boolean active(ClientWorld w) {
        return w != null && V.on("CustomSky") && w.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.NORMAL;
    }

    public static float[] horizon(ClientWorld w, float td) {
        int c = palette(style())[2];
        return new float[]{((c >> 16) & 0xFF) / 255f * 0.9f, ((c >> 8) & 0xFF) / 255f * 0.9f, (c & 0xFF) / 255f * 0.9f};
    }

    public static boolean hideClouds() { return active(MinecraftClient.getInstance().world) && !V.bo("CustomSky", "Chmury"); }

    // ------------------------------------------------------------------ render

    public static boolean render(Matrix4f view, float td, Camera cam, boolean thickFog) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!active(mc.world) || thickFog) return false;
        CameraSubmersionType sub = cam.getSubmersionType();
        if (sub == CameraSubmersionType.POWDER_SNOW || sub == CameraSubmersionType.LAVA) return false;
        if (cam.getFocusedEntity() instanceof LivingEntity le && (le.hasStatusEffect(StatusEffects.BLINDNESS) || le.hasStatusEffect(StatusEffects.DARKNESS))) return false;

        String st = style();
        int[] pal = palette(st);
        boolean anim = V.bo("CustomSky", "Animacja");
        float time = anim ? (System.currentTimeMillis() % 3_600_000L) / 1000f : 0;
        MatrixStack ms = new MatrixStack();
        ms.multiplyPositionMatrix(view);

        BackgroundRenderer.clearFog();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            dome(ms.peek().getPositionMatrix(), pal);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
            ms.push();
            if (anim) ms.multiply(RotationAxis.of(new Vector3f(0.3f, 1, 0.2f).normalize()).rotationDegrees(time * 0.6f));
            Matrix4f m = ms.peek().getPositionMatrix();
            if (st.equals("Galaxy")) nebulae(m, time);
            stars(m, st, time);
            ms.pop();
            if (st.equals("Aurora")) aurora(ms.peek().getPositionMatrix(), time);
            if (st.equals("Sunset")) sun(ms, 0xFFFF9A5A, -1, 0.07f, 34, 0.55f);
            if (st.equals("Night") || st.equals("Mono")) sun(ms, st.equals("Mono") ? 0xFFFFFFFF : 0xFFDCE4FF, 0.4f, 0.75f, 7, 1f);
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        return true;
    }

    private static void dome(Matrix4f m, int[] pal) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int lat = 18, lon = 36;
        for (int i = 0; i < lat; i++) {
            float e0 = -90 + 180f * i / lat, e1 = -90 + 180f * (i + 1) / lat;
            int c0 = gradient(pal, e0), c1 = gradient(pal, e1);
            for (int j = 0; j < lon; j++) {
                float a0 = 360f * j / lon, a1 = 360f * (j + 1) / lon;
                v(bb, m, a0, e0, c0); v(bb, m, a1, e0, c0); v(bb, m, a1, e1, c1); v(bb, m, a0, e1, c1);
            }
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private static int gradient(int[] pal, float elev) {
        if (elev < 0) return R2D.blend(pal[2] | 0xFF000000, 0xFF000000 | dim(pal[0]), Math.min(1, -elev / 25f));
        float t = (float) Math.pow(elev / 90f, 0.55);
        return t < 0.45f ? R2D.blend(pal[2] | 0xFF000000, pal[1] | 0xFF000000, t / 0.45f) : R2D.blend(pal[1] | 0xFF000000, pal[0] | 0xFF000000, (t - 0.45f) / 0.55f);
    }

    private static int dim(int c) { return (((c >> 16) & 0xFF) / 2 << 16) | (((c >> 8) & 0xFF) / 2 << 8) | ((c & 0xFF) / 2); }

    private static void v(BufferBuilder bb, Matrix4f m, float azDeg, float elDeg, int c) {
        float az = azDeg * MathHelper.RADIANS_PER_DEGREE, el = elDeg * MathHelper.RADIANS_PER_DEGREE;
        float ce = MathHelper.cos(el);
        bb.vertex(m, MathHelper.sin(az) * ce * R, MathHelper.sin(el) * R, -MathHelper.cos(az) * ce * R)
                .color(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, 1f);
    }

    private static void stars(Matrix4f m, String st, float time) {
        WorldFx.bind(DOT);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int n = switch (st) { case "Galaxy" -> STARS + BAND; case "Sunset" -> 500; case "Mono" -> 700; default -> STARS; };
        float tint = st.equals("Galaxy") ? 0.85f : 1f;
        for (int i = 0; i < n; i++) {
            float[] s = star[i];
            float tw = 0.65f + 0.35f * MathHelper.sin(time * (1.2f + s[10]) + s[10] * 3);
            float a = s[11] * tw;
            if (st.equals("Sunset")) a *= MathHelper.clamp(s[1] / R * 2.2f - 0.3f, 0, 1);
            if (a < 0.02f) continue;
            float r = a, g = a * (i % 7 == 0 ? 0.85f : 1f), b = a * (i % 5 == 0 ? 0.8f * tint + 0.2f : 1f);
            if (st.equals("Galaxy") && i >= STARS) { r *= 0.9f; b = Math.min(1, b * 1.15f); }
            quad(bb, m, s, s[9] * 2.4f, r, g, b);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private static void nebulae(Matrix4f m, float time) {
        WorldFx.bind(DOT);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int[] cols = {0xFF6A3CFF, 0xFFFF4FB8, 0xFF3C8CFF};
        for (float[] s : neb) {
            int c = cols[(int) s[11]];
            float a = 0.16f + 0.04f * MathHelper.sin(time * 0.3f + s[10]);
            quad(bb, m, s, s[9], V.r(c) * a, V.g(c) * a, V.b(c) * a);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private static void quad(BufferBuilder bb, Matrix4f m, float[] s, float size, float r, float g, float b) {
        float ux = s[3] * size, uy = s[4] * size, uz = s[5] * size, vx = s[6] * size, vy = s[7] * size, vz = s[8] * size;
        bb.vertex(m, s[0] - ux - vx, s[1] - uy - vy, s[2] - uz - vz).texture(0, 0).color(r, g, b, 1f);
        bb.vertex(m, s[0] + ux - vx, s[1] + uy - vy, s[2] + uz - vz).texture(1, 0).color(r, g, b, 1f);
        bb.vertex(m, s[0] + ux + vx, s[1] + uy + vy, s[2] + uz + vz).texture(1, 1).color(r, g, b, 1f);
        bb.vertex(m, s[0] - ux + vx, s[1] - uy + vy, s[2] - uz + vz).texture(0, 1).color(r, g, b, 1f);
    }

    /** zorza: trzy falujące wstęgi na północy (jasny dół, przezroczysta góra) */
    private static void aurora(Matrix4f m, float time) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int[] cols = {0xFF3CFFA0, 0xFF46C8FF, 0xFFAA6EFF};
        int seg = 72;
        for (int k = 0; k < 3; k++) {
            int c = cols[k];
            float[] px = new float[seg + 1], py = new float[seg + 1], pz = new float[seg + 1], tx = new float[seg + 1], ty = new float[seg + 1], tz = new float[seg + 1], al = new float[seg + 1];
            for (int j = 0; j <= seg; j++) {
                float t = j / (float) seg;
                float az = (-85 + 170 * t + 6 * MathHelper.sin(time * 0.35f + t * 9 + k)) * MathHelper.RADIANS_PER_DEGREE;
                float e0 = (10 + k * 5 + 4 * MathHelper.sin(t * 12 + time * 0.6f + k * 1.3f)) * MathHelper.RADIANS_PER_DEGREE;
                float h = (20 + 9 * MathHelper.sin(t * 7 - time * 0.45f + k * 2.1f)) * MathHelper.RADIANS_PER_DEGREE;
                float rr = R * (0.97f - k * 0.01f);
                px[j] = MathHelper.sin(az) * MathHelper.cos(e0) * rr; py[j] = MathHelper.sin(e0) * rr; pz[j] = -MathHelper.cos(az) * MathHelper.cos(e0) * rr;
                float e1 = e0 + h;
                tx[j] = MathHelper.sin(az) * MathHelper.cos(e1) * rr; ty[j] = MathHelper.sin(e1) * rr; tz[j] = -MathHelper.cos(az) * MathHelper.cos(e1) * rr;
                al[j] = MathHelper.sin(t * MathHelper.PI) * (0.38f + 0.18f * MathHelper.sin(t * 20 + time * 1.4f + k));
            }
            for (int j = 0; j < seg; j++) {
                bb.vertex(m, px[j], py[j], pz[j]).color(V.r(c) * al[j], V.g(c) * al[j], V.b(c) * al[j], 1f);
                bb.vertex(m, px[j + 1], py[j + 1], pz[j + 1]).color(V.r(c) * al[j + 1], V.g(c) * al[j + 1], V.b(c) * al[j + 1], 1f);
                bb.vertex(m, tx[j + 1], ty[j + 1], tz[j + 1]).color(0f, 0f, 0f, 1f);
                bb.vertex(m, tx[j], ty[j], tz[j]).color(0f, 0f, 0f, 1f);
            }
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    /** słońce przy horyzoncie / księżyc: miękka poświata + jasny środek */
    private static void sun(MatrixStack ms, int col, float x, float y, float size, float glow) {
        Vector3f d = new Vector3f(x, y, 0.35f).normalize();
        float[] s = make(d, new Random(7), size);
        WorldFx.bind(DOT);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        quad(bb, m, s, size * 3.2f, V.r(col) * 0.22f * glow, V.g(col) * 0.22f * glow, V.b(col) * 0.22f * glow);
        quad(bb, m, s, size, V.r(col), V.g(col), V.b(col));
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }
}
