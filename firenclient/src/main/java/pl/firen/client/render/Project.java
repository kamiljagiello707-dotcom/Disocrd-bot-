package pl.firen.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Rzutowanie pozycji ze świata na ekran (do 2D ESP, Nametags). Macierze z ostatniej klatki świata. */
public final class Project {
    private Project() {}

    private static final Matrix4f view = new Matrix4f(), proj = new Matrix4f();
    private static Vec3d cam = Vec3d.ZERO;

    public static void capture(WorldRenderContext ctx) {
        view.set(ctx.positionMatrix());
        proj.set(ctx.projectionMatrix());
        cam = ctx.camera().getPos();
    }

    /** Zwraca {x, y} w skalowanych współrzędnych GUI albo null, gdy punkt jest za kamerą. */
    public static float[] toScreen(Vec3d world) {
        Vector4f v = new Vector4f((float) (world.x - cam.x), (float) (world.y - cam.y), (float) (world.z - cam.z), 1f);
        view.transform(v);
        proj.transform(v);
        if (v.w <= 0.05f) return null;
        float nx = v.x / v.w, ny = v.y / v.w;
        var win = MinecraftClient.getInstance().getWindow();
        return new float[]{(nx * 0.5f + 0.5f) * win.getScaledWidth(), (0.5f - ny * 0.5f) * win.getScaledHeight()};
    }
}
