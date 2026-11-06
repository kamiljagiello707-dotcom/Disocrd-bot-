package pl.mystic.client.module.modules;

import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Fly extends Module {
    public Fly() { super("Fly", "Latanie jak w trybie kreatywnym", Category.MOVEMENT, GLFW.GLFW_KEY_G); }

    @Override
    public void onTick() {
        var abilities = mc.player.getAbilities();
        abilities.allowFlying = true;
        abilities.setFlySpeed(0.08f);
    }

    @Override
    protected void onDisable() {
        var player = mc.player;
        var abilities = player.getAbilities();
        abilities.setFlySpeed(0.05f);
        if (!player.isCreative() && !player.isSpectator()) {
            abilities.flying = false;
            abilities.allowFlying = false;
        }
    }
}
