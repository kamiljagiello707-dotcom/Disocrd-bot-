package pl.firen.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;

public class Velocity extends Module {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 0, 0, 100, 1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical %", 0, 0, 100, 1));
    public final BooleanSetting explosions = add(new BooleanSetting("Explosions", true));
    public final BooleanSetting inWater = add(new BooleanSetting("In water", false));

    public Velocity() { super("Velocity", "Zmniejsza odrzut po ciosie i eksplozji.", Category.COMBAT); }

    @Override public String getSuffix() { return horizontal.display() + "% " + vertical.display() + "%"; }

    /** Wołane z mixina: before = prędkość przed pakietem, aktualna = po pakiecie. */
    public static void adjust(Vec3d before) {
        Velocity v = FirenClient.modules().get(Velocity.class);
        var p = MinecraftClient.getInstance().player;
        if (!v.isEnabled() || p == null) return;
        if (!v.inWater.get() && (p.isTouchingWater() || p.isInLava())) return;
        Vec3d after = p.getVelocity();
        double h = v.horizontal.get() / 100.0, ver = v.vertical.get() / 100.0;
        p.setVelocity(before.x + (after.x - before.x) * h, before.y + (after.y - before.y) * ver, before.z + (after.z - before.z) * h);
    }
}
