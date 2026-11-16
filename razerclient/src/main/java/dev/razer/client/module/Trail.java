package dev.razer.client.module;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.razer.client.render.R2D;
import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.ColorSetting;
import dev.razer.client.setting.ModeSetting;
import dev.razer.client.setting.NumberSetting;
import dev.razer.client.ui.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayDeque;

/** Trail – gradientowa smuga za Twoją postacią (z testem głębi, tylko kosmetyka). */
public class Trail extends Module {
    public final NumberSetting length = new NumberSetting("Długość", "Length", 20, 5, 60, 1);
    public final NumberSetting height = new NumberSetting("Wysokość", "Height", 1.6, 0.1, 2.0, 0.05);
    public final NumberSetting opacity = new NumberSetting("Krycie", "Opacity", 55, 10, 100, 5);
    public final BoolSetting glow = new BoolSetting("Poświata", "Glow", true);
    public final BoolSetting thirdOnly = new BoolSetting("Tylko 3. osoba", "Only Third Person", true);
    public final ModeSetting colorMode = new ModeSetting("Kolor", "Color", "Accent", "Rainbow", "Custom");
    public final ColorSetting customColor = new ColorSetting("Własny kolor", "Custom Color", 0xFF4D8BFF);

    private final ArrayDeque<Vec3d> points = new ArrayDeque<>();

    public Trail() {
        super("Trail", Category.COSMETICS, "footprints", "Gradientowa smuga za postacią", "Gradient trail behind you");
        add(length, height, opacity, glow, thirdOnly, colorMode, customColor);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
    }

    private void tick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null) { points.clear(); return; }
        Vec3d p = mc.player.getPos();
        if (points.isEmpty() || points.peekLast().squaredDistanceTo(p) > 0.0004) points.addLast(p);
        else if (!points.isEmpty()) points.pollFirst(); // stoisz – smuga powoli znika
        while (points.size() > length.get().intValue()) points.pollFirst();
    }

    private int color(float t, float time) {
        return switch (colorMode.get()) {
            case "Rainbow" -> 0xFF000000 | (Color.HSBtoRGB((t * 0.6f + time * 0.15f) % 1f, 0.65f, 1f) & 0xFFFFFF);
            case "Custom" -> customColor.get();
            default -> Theme.accentWave(t * 3 + time * 2);
        };
    }

    private void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null || points.size() < 2) return;
        if (thirdOnly.get() && mc.options.getPerspective().isFirstPerson()) return;
        Vec3d cam = ctx.camera().getPos();
        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d head = new Vec3d(MathHelper.lerp(delta, mc.player.prevX, mc.player.getX()),
                MathHelper.lerp(delta, mc.player.prevY, mc.player.getY()), MathHelper.lerp(delta, mc.player.prevZ, mc.player.getZ()));
        Matrix4f m = ctx.matrixStack() != null ? ctx.matrixStack().peek().getPositionMatrix() : new Matrix4f();
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        float h = height.get().floatValue(), op = (float) (opacity.get() / 100.0);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        for (int pass = 0; pass < (glow.get() ? 2 : 1); pass++) {
            if (pass == 1) RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            else RenderSystem.defaultBlendFunc();
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            int n = points.size(), i = 0;
            for (Vec3d p : points) {
                float t = (float) i / n;             // 0 = koniec smugi, 1 = przy graczu
                float a = t * t * op * (pass == 1 ? 0.35f : 1f);
                int c = color(t, time);
                float x = (float) (p.x - cam.x), y = (float) (p.y - cam.y), z = (float) (p.z - cam.z);
                b.vertex(m, x, y + 0.02f, z).color(R2D.withAlpha(c, a));
                b.vertex(m, x, y + h, z).color(R2D.withAlpha(c, a * 0.15f));
                i++;
            }
            float x = (float) (head.x - cam.x), y = (float) (head.y - cam.y), z = (float) (head.z - cam.z);
            int c = color(1, time);
            b.vertex(m, x, y + 0.02f, z).color(R2D.withAlpha(c, op * (pass == 1 ? 0.35f : 1f)));
            b.vertex(m, x, y + h, z).color(R2D.withAlpha(c, op * 0.15f * (pass == 1 ? 0.35f : 1f)));
            BufferRenderer.drawWithGlobalProgram(b.end());
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
