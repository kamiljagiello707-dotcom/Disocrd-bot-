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
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Icons;
import tech.fuzir.client.util.Theme;

/** Półprzezroczysty stożek nad głową Twojej postaci. */
public class ChinaHat extends Module {
    private final NumberSetting radius = add(new NumberSetting("Szerokość", 0.65, 0.3, 1.2, 0.05, ""));
    private final NumberSetting height = add(new NumberSetting("Wysokość", 0.3, 0.1, 0.8, 0.05, ""));
    private final ModeSetting color = add(new ModeSetting("Kolor", "Motyw", "Motyw", "Biały", "Tęcza"));
    private final BoolSetting firstPerson = add(new BoolSetting("Widoczny z pierwszej osoby", false));

    public ChinaHat() {
        super("China Hat", "Stożkowy kapelusz nad głową postaci", Category.VISUALS, Icons.TRIANGLE);
    }

    private int base() {
        return switch (color.get()) {
            case "Biały" -> 0xFFFFFFFF;
            case "Tęcza" -> 0xFF000000 | java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000) / 4000f, 0.5f, 1f);
            default -> Theme.current().accent() | 0xFF000000;
        };
    }

    public void render(WorldRenderContext ctx) {
        if (mc.player == null || mc.player.isInvisible()) return;
        if (!ctx.camera().isThirdPerson() && !firstPerson.on()) return;
        float td = ctx.tickCounter().getTickDelta(true);
        Vec3d pos = mc.player.getLerpedPos(td).subtract(ctx.camera().getPos());
        // obrót kamery jest już w macierzy widoku gry, więc tu tylko przesunięcie względem kamery
        Matrix4f m = new Matrix4f();
        float x = (float) pos.x, y = (float) pos.y + mc.player.getHeight() + 0.02f, z = (float) pos.z;
        int col = base();
        WorldDraw.begin();
        WorldDraw.cone(m, x, y, z, radius.f(), height.f(), Canvas.mulAlpha(col, 0.85f), Canvas.mulAlpha(col, 0.28f));
        // jasny brzeg kapelusza
        WorldDraw.ring(m, x, y, z, radius.f() - 0.03f, radius.f(), Canvas.mulAlpha(col, 0.9f), Canvas.mulAlpha(col, 0.9f));
        WorldDraw.end();
    }
}
