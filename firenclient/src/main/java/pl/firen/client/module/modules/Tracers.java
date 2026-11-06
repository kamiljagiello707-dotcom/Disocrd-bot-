package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Targets;

public class Tracers extends Module {
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final ModeSetting colorMode = add(new ModeSetting("Color", "Distance", "Distance", "Static"));
    public final ColorSetting color = add(new ColorSetting("Line color", 0xFFFFFFFF));
    public final ModeSetting target = add(new ModeSetting("Target", "Body", "Feet", "Body", "Head"));
    public final NumberSetting width = add(new NumberSetting("Width", 1.5, 0.5, 5.0, 0.5));
    public final NumberSetting alpha = add(new NumberSetting("Alpha", 200, 30, 255, 5));

    public Tracers() {
        super("Tracers", "Linie od celownika do graczy i mobów.", Category.RENDER);
        color.visibleWhen(() -> colorMode.is("Static"));
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || ctx.matrixStack() == null) return;
        float delta = ctx.tickCounter().getTickDelta(true);
        var camera = ctx.camera();
        Vec3d cam = camera.getPos();
        Vec3d start = Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).multiply(2);
        Render3D.begin();
        for (Entity e : mc.world.getEntities()) {
            if (!Targets.matches(e, players.get(), mobs.get(), animals.get())) continue;
            Vec3d pos = e.getLerpedPos(delta).subtract(cam);
            double yOff = switch (target.get()) {
                case "Feet" -> 0;
                case "Head" -> e.getHeight() * 0.9;
                default -> e.getHeight() * 0.5;
            };
            Vec3d end = pos.add(0, yOff, 0);
            int a = alpha.get().intValue();
            int c = colorMode.is("Static") ? (a << 24 | (color.get() & 0xFFFFFF))
                    : (e instanceof PlayerEntity ? Targets.distanceColor(mc.player.distanceTo(e), a) : (a << 24 | 0xFFC34D));
            Render3D.line(ctx.matrixStack(), start, end, c, width.get().floatValue());
        }
        Render3D.end();
    }
}
