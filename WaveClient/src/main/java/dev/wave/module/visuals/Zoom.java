package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.NumberSetting;
import dev.wave.util.Animation;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    public final NumberSetting factor = new NumberSetting("Factor", "How far to zoom in", 4, 1.5, 15, 0.5, "x");
    public final BooleanSetting smooth = new BooleanSetting("Smooth", "Animate the zoom", true);
    public final BooleanSetting scroll = new BooleanSetting("Scroll", "Mouse wheel changes the zoom", true);
    public final BooleanSetting lowerSensitivity = new BooleanSetting("Lower Sensitivity", "Scale mouse sensitivity with zoom", true);
    public final BooleanSetting cinematic = new BooleanSetting("Cinematic", "Smooth camera while zooming", false);

    private final Animation anim = new Animation(14, 1);
    private double scrollFactor = 1;
    private boolean prevCinematic;

    public Zoom() {
        super("Zoom", "OptiFine-style zoom. Hold the bind to zoom", Category.VISUALS);
        addSettings(factor, smooth, scroll, lowerSensitivity, cinematic);
        bindMode.set("Hold");
        drawn.set(false);
        setKey(GLFW.GLFW_KEY_C);
    }

    @Override
    public void onEnable() {
        scrollFactor = 1;
        prevCinematic = mc.options.smoothCameraEnabled;
        if (cinematic.isOn()) mc.options.smoothCameraEnabled = true;
    }

    @Override
    public void onDisable() {
        if (cinematic.isOn()) mc.options.smoothCameraEnabled = prevCinematic;
    }

    public double currentFactor() { return isEnabled() ? factor.get() * scrollFactor : 1; }

    public float applyFov(float fov) {
        double target = currentFactor();
        anim.setTarget(target);
        if (!smooth.isOn()) anim.set(target);
        float f = anim.update();
        return fov / Math.max(1f, f);
    }

    public boolean onScroll(double amount) {
        if (!isEnabled() || !scroll.isOn()) return false;
        scrollFactor = Math.max(0.3, Math.min(4, scrollFactor * (amount > 0 ? 1.15 : 1 / 1.15)));
        return true;
    }

    public double sensitivityScale() {
        double f = anim.get();
        return f <= 1.01 ? 1 : 1 / Math.sqrt(f);
    }
}
