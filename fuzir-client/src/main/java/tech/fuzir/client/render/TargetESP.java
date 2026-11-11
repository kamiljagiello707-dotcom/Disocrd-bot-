package tech.fuzir.client.module.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.impl.BoolSetting;
import tech.fuzir.client.setting.impl.ModeSetting;
import tech.fuzir.client.setting.impl.NumberSetting;

public class TargetESP extends Module {

    public ModeSetting mode = new ModeSetting("Mode", "Box", "Box", "Outline", "Jello");
    public NumberSetting red = new NumberSetting("Red", 0, 255, 0, 255);
    public NumberSetting green = new NumberSetting("Green", 255, 0, 255, 255);
    public NumberSetting blue = new NumberSetting("Blue", 120, 0, 255, 255);
    public BoolSetting healthBar = new BoolSetting("Health Bar", true);

    private Entity target;

    public TargetESP() {
        super("TargetESP", "Displays an ESP around your current target.", Category.RENDER);
        this.addSettings(mode, red, green, blue, healthBar);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
    }

    // Wywołanie wsadzane np. w WorldRender lub Event 3D
    public void onRender3D(MatrixStack matrixStack, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        // Proste pobranie entity na które patrzysz lub targetu z KillAury
        target = mc.targetedEntity;

        if (target instanceof LivingEntity && target != mc.player) {
            // Tutaj logika renderowania w przestrzeni świata (WorldDraw / Vector projection)
            // Integracja z Twoim systemem WorldDraw
        }
    }
}
