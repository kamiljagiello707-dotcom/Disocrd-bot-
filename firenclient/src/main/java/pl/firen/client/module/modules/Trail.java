package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.NumberSetting;

import java.util.ArrayDeque;
import java.util.Deque;

/** Zostawia zanikający ślad za Twoją postacią. */
public class Trail extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF4DA3FF));
    public final NumberSetting length = add(new NumberSetting("Length", 60, 10, 200, 5));
    public final NumberSetting width = add(new NumberSetting("Width", 2.0, 1.0, 6.0, 0.5));

    private final Deque<Vec3d> points = new ArrayDeque<>();

    public Trail() { super("Trail", "Zanikający ślad za Twoją postacią.", Category.RENDER); }

    @Override public void onDisable() { points.clear(); }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        points.addLast(mc.player.getPos());
        while (points.size() > length.get()) points.removeFirst();
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || ctx.matrixStack() == null || points.size() < 2) return;
        Vec3d cam = ctx.camera().getPos();
        int c = color.get() & 0xFFFFFF;
        Vec3d[] arr = points.toArray(new Vec3d[0]);
        double[][] segs = new double[arr.length - 1][6];
        Render3D.begin();
        for (int i = 0; i < arr.length - 1; i++) {
            Vec3d a = arr[i].subtract(cam), b = arr[i + 1].subtract(cam);
            int alpha = (int) (220.0 * i / arr.length);
            Render3D.line(ctx.matrixStack(), a, b, (alpha << 24) | c, width.get().floatValue());
        }
        Render3D.end();
    }
}
