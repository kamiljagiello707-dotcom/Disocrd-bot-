package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.NumberSetting;

/** Podświetla upuszczone przedmioty na ziemi. */
public class ItemEsp extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFC34D));
    public final NumberSetting fill = add(new NumberSetting("Fill alpha", 35, 0, 255, 5));
    public final NumberSetting width = add(new NumberSetting("Line width", 1.5, 1.0, 4.0, 0.5));

    public ItemEsp() { super("ItemESP", "Podświetla przedmioty leżące na ziemi.", Category.RENDER); }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || ctx.matrixStack() == null || mc.world == null) return;
        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos().negate();
        int c = color.get();
        Render3D.begin();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity)) continue;
            Vec3d lerp = e.getLerpedPos(delta);
            Box box = e.getBoundingBox().offset(lerp.subtract(e.getPos())).expand(0.08).offset(cam);
            if (fill.get() > 0) Render3D.boxFill(ctx.matrixStack(), box, (fill.get().intValue() << 24) | (c & 0xFFFFFF));
            Render3D.boxLines(ctx.matrixStack(), box, 0xFF000000 | c, width.get().floatValue());
        }
        Render3D.end();
    }
}
