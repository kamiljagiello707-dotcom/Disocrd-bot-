package pl.mysticclient.module.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.util.Render3D;

public class Tracers extends Module {
    public Tracers() {
        super("Tracers", "Linie od celownika do celów (używa filtrów z ESP).", Category.RENDER);
    }

    @Override
    public void onRender3D(WorldRenderContext ctx) {
        if (nullCheck()) return;
        ESP esp = ModuleManager.get(ESP.class);
        float td = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = Render3D.matrix(ctx);
        Vec3d start = cam.add(Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(2));
        BufferBuilder b = Render3D.beginLines();
        for (Entity e : mc.world.getEntities()) {
            if (!esp.shouldRender(e)) continue;
            Vec3d target = e.getLerpedPos(td).add(0, e.getHeight() / 2, 0);
            Render3D.line(b, m, cam, start, target, esp.colorFor(e));
        }
        Render3D.draw(b);
    }
}
