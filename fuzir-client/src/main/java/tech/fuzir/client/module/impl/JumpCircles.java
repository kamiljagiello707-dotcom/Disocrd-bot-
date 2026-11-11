package tech.fuzir.client.module.impl;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.render.WorldDraw;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Icons;
import tech.fuzir.client.util.Theme;

import java.util.ArrayList;
import java.util.List;

/** Rozchodzące się kręgi pod nogami przy skoku. */
public class JumpCircles extends Module {
    private final NumberSetting size = add(new NumberSetting("Rozmiar", 1.1, 0.5, 2.5, 0.1, ""));
    private final NumberSetting time = add(new NumberSetting("Czas", 1.2, 0.4, 3, 0.1, " s"));
    private final ModeSetting color = add(new ModeSetting("Kolor", "Motyw", "Motyw", "Biały", "Tęcza"));
    private final BoolSetting landing = add(new BoolSetting("Przy lądowaniu", true));

    private record Circle(double x, double y, double z, long start, float hue) {
    }

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround = true;
    private float hue;

    public JumpCircles() {
        super("JumpCircles", "Kręgi pod nogami przy skoku", Category.VISUALS, Icons.CIRCLE_DASHED);
    }

    @Override
    public void onTick() {
        boolean onGround = mc.player.isOnGround();
        if (wasOnGround && !onGround && mc.player.getVelocity().y > 0.1) spawn(mc.player.getPos().add(0, -mc.player.getVelocity().y, 0));
        if (!wasOnGround && onGround && landing.on() && !mc.player.getAbilities().flying) spawn(mc.player.getPos());
        wasOnGround = onGround;
    }

    private void spawn(Vec3d p) {
        hue = (hue + 0.13f) % 1f;
        circles.add(new Circle(p.x, p.y + 0.02, p.z, System.currentTimeMillis(), hue));
        while (circles.size() > 24) circles.remove(0);
    }

    @Override
    public void onDisable() {
        circles.clear();
    }

    private int base(Circle c) {
        return switch (color.get()) {
            case "Biały" -> 0xFFFFFFFF;
            case "Tęcza" -> 0xFF000000 | java.awt.Color.HSBtoRGB(c.hue, 0.55f, 1f);
            default -> Theme.current().accent() | 0xFF000000;
        };
    }

    public void render(WorldRenderContext ctx) {
        if (circles.isEmpty()) return;
        Vec3d cam = ctx.camera().getPos();
        // obrót kamery jest już w macierzy widoku gry, więc tu tylko przesunięcie względem kamery
        Matrix4f m = new Matrix4f();
        long now = System.currentTimeMillis();
        long life = (long) (time.get() * 1000);
        circles.removeIf(c -> now - c.start > life);
        WorldDraw.begin();
        for (Circle c : circles) {
            float p = (now - c.start) / (float) life;
            float e = Anim.easeOutCubic(p);
            float r = size.f() * (0.25f + 0.75f * e);
            float a = 1f - p;
            int col = base(c);
            float x = (float) (c.x - cam.x), y = (float) (c.y - cam.y), z = (float) (c.z - cam.z);
            // miękki środek, wyraźna krawędź i delikatna poświata na zewnątrz
            WorldDraw.ring(m, x, y, z, r * 0.55f, r * 0.93f, Canvas.mulAlpha(col, 0), Canvas.mulAlpha(col, 0.35f * a));
            WorldDraw.ring(m, x, y, z, r * 0.93f, r, Canvas.mulAlpha(col, 0.95f * a), Canvas.mulAlpha(col, 0.95f * a));
            WorldDraw.ring(m, x, y, z, r, r * 1.12f, Canvas.mulAlpha(col, 0.35f * a), Canvas.mulAlpha(col, 0));
        }
        WorldDraw.end();
    }
}
