package pl.firen.client.module.modules;

import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Anim;

/** Przybliżenie widoku (ustaw bind, np. C). */
public class Zoom extends Module {
    public final NumberSetting factor = add(new NumberSetting("Factor", 4, 1.5, 10, 0.5));
    private static float anim = 1f;
    private static long last = System.nanoTime();

    public Zoom() { super("Zoom", "Przybliża widok (jak luneta).", Category.RENDER); }

    public static float divisor() {
        var m = FirenClient.modules();
        float target = m != null && m.get(Zoom.class).isEnabled() ? m.get(Zoom.class).factor.get().floatValue() : 1f;
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        anim = Anim.approach(anim, target, dt, 14f);
        return anim;
    }
}
