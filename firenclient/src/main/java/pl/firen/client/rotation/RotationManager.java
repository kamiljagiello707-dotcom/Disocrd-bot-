package pl.firen.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Rotacje "server-side": moduł prosi o obrót, a do serwera trafia on w pakiecie ruchu,
 * podczas gdy kamera gracza zostaje tam, gdzie ją ustawił. Po puszczeniu celu
 * rotacja płynnie wraca do kamery.
 */
public final class RotationManager {
    private RotationManager() {}

    private static float yaw, pitch;
    private static boolean active, requested, moveFix;
    private static float returnSpeed = 30f;

    /** Rotacja faktycznie wysłana do serwera w ostatnim pakiecie ruchu. */
    public static float sentYaw, sentPitch;

    public static boolean isActive() { return active; }
    public static boolean moveFix() { return active && moveFix; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    /** Aktualna rotacja z perspektywy serwera (punkt startowy dla płynnego obracania). */
    public static float currentYaw() {
        var p = MinecraftClient.getInstance().player;
        return active ? yaw : p == null ? 0 : p.getYaw();
    }

    public static float currentPitch() {
        var p = MinecraftClient.getInstance().player;
        return active ? pitch : p == null ? 0 : p.getPitch();
    }

    public static void beginTick() {
        requested = false;
    }

    public static void request(float newYaw, float newPitch, boolean fixMovement, float backSpeed) {
        yaw = newYaw;
        pitch = MathHelper.clamp(newPitch, -90f, 90f);
        moveFix = fixMovement;
        returnSpeed = backSpeed;
        active = true;
        requested = true;
    }

    /** Gdy nikt nie prosi o rotację, wracamy płynnie do kamery. */
    public static void endTick() {
        var p = MinecraftClient.getInstance().player;
        if (p == null) { active = false; return; }
        if (requested || !active) return;
        float dy = MathHelper.wrapDegrees(p.getYaw() - yaw);
        float dp = p.getPitch() - pitch;
        if (Math.abs(dy) < returnSpeed && Math.abs(dp) < returnSpeed) {
            active = false;
            return;
        }
        yaw += MathHelper.clamp(dy, -returnSpeed, returnSpeed);
        pitch += MathHelper.clamp(dp, -returnSpeed, returnSpeed);
    }
}
