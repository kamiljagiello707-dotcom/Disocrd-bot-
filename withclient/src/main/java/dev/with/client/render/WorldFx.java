package dev.with.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.module.Setting;
import dev.with.client.ui.Anim;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.util.*;

/**
 * Efekty w świecie (kosmetyka): Target ESP – świecący znacznik na przeciwniku, którego bijesz
 * (7 stylów z tekstur) i Jump Circles – kręgi pod nogami przy skoku (4 style).
 * Rysowane z testem głębi, więc nic nie jest widoczne przez ściany.
 */
public final class WorldFx {
    private WorldFx() {}

    private static final Map<String, Identifier> ESP = new LinkedHashMap<>();
    private static final Map<String, Identifier> JC = new LinkedHashMap<>();
    static {
        for (String[] e : new String[][]{{"Diamond", "tesp_diamond"}, {"Triad", "tesp_triad"}, {"Shuriken", "tesp_shuriken"},
                {"Brackets", "tesp_brackets"}, {"Bold", "tesp_bold"}, {"Soft", "tesp_soft"}, {"Thin", "tesp_thin"}})
            ESP.put(e[0], R2D.id("textures/fx/" + e[1] + ".png"));
        for (String[] e : new String[][]{{"Ring", "jc_ring"}, {"Glow", "jc_glow"}, {"Soft", "jc_soft"}, {"Swirl", "jc_swirl"}})
            JC.put(e[0], R2D.id("textures/fx/" + e[1] + ".png"));
    }
    private static final Set<Identifier> MIPPED = new HashSet<>();

    private static LivingEntity attacked, shown;
    private static long attackTime, lockTime;
    private static final Anim appear = new Anim(0, 9);
    private record Circle(double x, double y, double z, long t) {}
    private static final List<Circle> CIRCLES = new ArrayList<>();
    private static boolean wasGround = true;

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) { attacked = le; attackTime = System.currentTimeMillis(); }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) { CIRCLES.clear(); return; }
            boolean ground = mc.player.isOnGround();
            Module jc = Modules.get("JumpCircles");
            if (wasGround && !ground && mc.player.getVelocity().y > 0.15 && jc != null && jc.isEnabled())
                CIRCLES.add(new Circle(mc.player.getX(), mc.player.getY(), mc.player.getZ(), System.currentTimeMillis()));
            wasGround = ground;
            while (CIRCLES.size() > 12) CIRCLES.remove(0);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(WorldFx::render);
    }

    // ------------------------------------------------------------------ ustawienia

    private static float num(Module m, String s, float d) { return m.setting(s) instanceof Setting.Num n ? n.get().floatValue() : d; }
    private static boolean bo(Module m, String s) { return m.setting(s) instanceof Setting.Bool b && b.get(); }
    private static String mode(Module m, String s, String d) { return m.setting(s) instanceof Setting.Mode o ? o.get() : d; }
    private static int color(Module m, String s, int d) { return m.setting(s) instanceof Setting.Color c ? c.get() : d; }

    /** kolor tęczy/gradientu w czasie (dla trybów "Rainbow") */
    public static int rainbow(float speed, float offset) {
        float h = ((System.currentTimeMillis() % 100000L) / 1000f * 0.15f * speed + offset) % 1f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(h, 0.55f, 1f) & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ render

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        MatrixStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new MatrixStack();
        Camera cam = ctx.camera();
        float delta = ctx.tickCounter().getTickDelta(true);
        Module esp = Modules.get("TargetESP"), jc = Modules.get("JumpCircles");
        boolean any = (esp != null && esp.isEnabled()) || (jc != null && jc.isEnabled() && !CIRCLES.isEmpty());
        if (!any) { appear.snap(0); shown = null; return; }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        try {
            if (jc != null && jc.isEnabled()) renderCircles(ms, cam, jc);
            if (esp != null && esp.isEnabled()) renderEsp(ms, cam, esp, delta);
            else { appear.snap(0); shown = null; }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static LivingEntity pick(Module m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean players = bo(m, "Tylko gracze");
        float range = num(m, "Zasięg", 12);
        if (attacked != null && attacked.isAlive() && attacked != mc.player && System.currentTimeMillis() - attackTime < 4000
                && (!players || attacked instanceof PlayerEntity) && attacked.distanceTo(mc.player) < range) return attacked;
        if (bo(m, "Też celowany") && mc.targetedEntity instanceof LivingEntity le && le.isAlive() && (!players || le instanceof PlayerEntity)) return le;
        return null;
    }

    private static void renderEsp(MatrixStack ms, Camera cam, Module m, float delta) {
        LivingEntity t = pick(m);
        long now = System.currentTimeMillis();
        if (t != null && t != shown) { shown = t; lockTime = now; }
        float a = appear.animate(t != null ? 1 : 0);
        if (shown == null || a < 0.01f) { if (a < 0.01f) shown = null; return; }
        if (!shown.isAlive()) { shown = null; return; }
        LivingEntity e = shown;
        Vec3d cp = cam.getPos();
        double x = MathHelper.lerp(delta, e.prevX, e.getX()) - cp.x;
        double y = MathHelper.lerp(delta, e.prevY, e.getY()) - cp.y + e.getHeight() * 0.5;
        double z = MathHelper.lerp(delta, e.prevZ, e.getZ()) - cp.z;
        String style = mode(m, "Styl", "Diamond");
        boolean brackets = style.equals("Brackets") || style.equals("Bold") || style.equals("Soft") || style.equals("Thin");
        float time = (now % 1_000_000L) / 1000f;
        float speed = num(m, "Prędkość obrotu", 1);
        float rot = switch (style) { case "Triad" -> time * 90; case "Shuriken" -> time * 160; case "Diamond" -> time * 40; default -> 0; } * speed;
        float lock = Anim.enter((now - lockTime) / 420f);
        float pulse = bo(m, "Pulsowanie") ? 1 + 0.05f * (float) Math.sin(time * 5) : 1;
        float hurt = bo(m, "Czerwony przy trafieniu") && e.hurtTime > 0 ? e.hurtTime / 10f : 0;
        float base = brackets ? Math.max(e.getHeight() * 0.62f, e.getWidth() * 1.05f) : Math.max(e.getHeight() * 0.5f, e.getWidth() * 0.9f);
        float size = base * num(m, "Rozmiar", 1) * pulse * (1.45f - 0.45f * lock) * (1 + 0.08f * hurt);
        String cm = mode(m, "Kolor tryb", "Static");
        int col = cm.equals("Rainbow") ? rainbow(1, 0) : color(m, "Kolor", 0xFFFFFFFF);
        col = R2D.blend(col, 0xFFFF3B3B, hurt);
        float alpha = a * Math.min(1, lock * 1.4f) * num(m, "Jasność", 100) / 100f;
        // lekko w stronę kamery, żeby znacznik nie chował się w modelu
        Vec3d toCam = new Vec3d(-x, -y, -z).normalize().multiply(e.getWidth() * 0.7);
        ms.push();
        ms.translate(x + toCam.x, y + toCam.y, z + toCam.z);
        ms.multiply(cam.getRotation());
        if (brackets && bo(m, "Obrót ramki")) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * (1 - lock)));
        else ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
        // ciemny "cień" pod spodem – znacznik widać też na jasnym niebie
        if (bo(m, "Kontrast")) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR);
            quad(ms, ESP.get(style), size * 1.06f, 0xFFFFFFFF, alpha * 0.55f);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        }
        quad(ms, ESP.get(style), size, col, alpha);
        // druga, większa i słabsza warstwa = poświata
        if (bo(m, "Poświata")) quad(ms, ESP.get(style), size * 1.1f, col, alpha * 0.2f);
        ms.pop();
    }

    private static void renderCircles(MatrixStack ms, Camera cam, Module m) {
        long now = System.currentTimeMillis();
        float dur = num(m, "Czas", 1) * 1000;
        Identifier tex = JC.getOrDefault(mode(m, "Styl", "Ring"), JC.get("Ring"));
        boolean swirl = mode(m, "Styl", "Ring").equals("Swirl");
        float maxR = num(m, "Rozmiar", 1) * 1.1f;
        String cm = mode(m, "Kolor tryb", "Static");
        Vec3d cp = cam.getPos();
        CIRCLES.removeIf(c -> now - c.t() > dur);
        int i = 0;
        for (Circle c : CIRCLES) {
            float age = (now - c.t()) / dur;
            float e = Anim.easeOutCubic(age);
            float r = maxR * (0.25f + 0.75f * e);
            float alpha = (float) Math.pow(1 - age, 1.4) * Math.min(1, age * 8);
            int col = cm.equals("Rainbow") ? rainbow(1.5f, i * 0.12f) : color(m, "Kolor", 0xFFFFFFFF);
            ms.push();
            ms.translate(c.x() - cp.x, c.y() - cp.y + 0.02, c.z() - cp.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            if (swirl || bo(m, "Obrót")) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 160));
            quad(ms, tex, r, col, alpha);
            ms.pop();
            i++;
        }
    }

    /** kwadrat -s..s w płaszczyźnie XY bieżącej macierzy, mieszanie addytywne (jasność tekstury × kolor × alfa) */
    private static void quad(MatrixStack ms, Identifier tex, float s, int col, float alpha) {
        if (alpha <= 0.003f) return;
        bind(tex);
        float r = ((col >> 16) & 0xFF) / 255f * alpha, g = ((col >> 8) & 0xFF) / 255f * alpha, b = (col & 0xFF) / 255f * alpha;
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bb.vertex(mat, -s, -s, 0).texture(0, 1).color(r, g, b, 1f);
        bb.vertex(mat, s, -s, 0).texture(1, 1).color(r, g, b, 1f);
        bb.vertex(mat, s, s, 0).texture(1, 0).color(r, g, b, 1f);
        bb.vertex(mat, -s, s, 0).texture(0, 0).color(r, g, b, 1f);
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    /** wiąże teksturę; za pierwszym razem dorabia mipmapy (gładkie z daleka) */
    private static void bind(Identifier id) {
        RenderSystem.setShaderTexture(0, id);
        if (!MIPPED.add(id)) return;
        try {
            AbstractTexture t = MinecraftClient.getInstance().getTextureManager().getTexture(id);
            GlStateManager._bindTexture(t.getGlId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 8);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 8f);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        } catch (Exception ignored) {}
    }
}
