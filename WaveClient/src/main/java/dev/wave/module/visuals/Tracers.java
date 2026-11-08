package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.NumberSetting;
import dev.wave.util.Render3D;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Tracers extends Module {
    public final BooleanSetting useEspTargets = new BooleanSetting("Use ESP filters", "Use the entity filters & colours from ESP", true);
    public final NumberSetting maxDistance = new NumberSetting("Distance", "", 128, 16, 512, 8, "m");
    public final NumberSetting opacity = new NumberSetting("Opacity", "", 0.85, 0.1, 1, 0.05, "");

    public Tracers() {
        super("Tracers", "Draws lines from your crosshair to entities", Category.VISUALS);
        addSettings(useEspTargets, maxDistance, opacity);
    }

    public void render(WorldRenderContext ctx, ESP esp) {
        if (nullCheck()) return;
        Vec3d cam = ctx.camera().getPos();
        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d look = Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(1.5);
        List<Render3D.Line> lines = new ArrayList<>();
        int alpha = (int) (opacity.get() * 255);
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (!esp.shouldOutline(e)) continue;
            if (!useEspTargets.isOn() && !(e instanceof net.minecraft.entity.player.PlayerEntity)) continue;
            if (e.distanceTo(mc.player) > maxDistance.get()) continue;
            double x = e.prevX + (e.getX() - e.prevX) * delta - cam.x;
            double y = e.prevY + (e.getY() - e.prevY) * delta - cam.y + e.getHeight() / 2;
            double z = e.prevZ + (e.getZ() - e.prevZ) * delta - cam.z;
            int color = (alpha << 24) | esp.getColor(e);
            lines.add(new Render3D.Line(look, new Vec3d(x, y, z), color));
        }
        Render3D.drawLines(ctx, lines);
    }
}
