package tech.fuzir.client.event.events;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Posted after the world has been rendered. The matrix stack already contains the camera rotation;
 * world positions must be translated by {@code -camera.getPos()}.
 */
public record Render3DEvent(MatrixStack matrices, Camera camera, float tickDelta) {
}
