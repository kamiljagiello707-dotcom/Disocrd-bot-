package pl.mystic.client.module.modules;

import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class AutoSprint extends Module {
    public AutoSprint() { super("AutoSprint", "Automatyczny sprint przy chodzeniu do przodu", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p.forwardSpeed > 0 && !p.isSneaking() && !p.horizontalCollision
                && (p.getHungerManager().getFoodLevel() > 6 || p.getAbilities().allowFlying)) {
            p.setSprinting(true);
        }
    }
}
