package tech.fuzir.client.module.impl;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.util.Icons;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.KeySetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.util.Anim;

public class Zoom extends Module {
    private final KeySetting key = add(new KeySetting("Klawisz zoomu", GLFW.GLFW_KEY_C));
    private final NumberSetting power = add(new NumberSetting("Przybliżenie", 4, 1.5, 10, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("Płynne wejście", true));
    private final BoolSetting cinematic = add(new BoolSetting("Kamera filmowa", true));

    private final Anim anim = new Anim(1);
    private boolean zooming;
    private boolean savedSmooth;

    public Zoom() {
        super("Zoom", "Przybliżenie widoku jak w lunecie", Category.RENDER, Icons.ZOOM);
        setEnabledQuiet(true);
    }

    @Override
    public void onTick() {
        boolean want = mc.currentScreen == null && key.isSet() && InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.get());
        if (want != zooming) {
            zooming = want;
            if (cinematic.on()) {
                if (want) {
                    savedSmooth = mc.options.smoothCameraEnabled;
                    mc.options.smoothCameraEnabled = true;
                } else {
                    mc.options.smoothCameraEnabled = savedSmooth;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (zooming && cinematic.on()) mc.options.smoothCameraEnabled = savedSmooth;
        zooming = false;
    }

    /** Dzielnik pola widzenia, wywoływany przy każdej klatce. */
    public float fovDivisor() {
        float target = zooming && isEnabled() ? power.f() : 1f;
        if (!smooth.on()) {
            anim.set(target);
            return target;
        }
        return anim.to(target, 14f);
    }

    @Override
    public String suffix() {
        return power.display();
    }
}
