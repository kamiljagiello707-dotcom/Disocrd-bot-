package pl.mystic.client.module.modules;

import net.minecraft.entity.attribute.EntityAttributes;
import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Step extends Module {
    public Step() { super("Step", "Wchodzenie na pełne bloki bez skakania", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN); }

    @Override
    public void onTick() { set(1.0); }

    @Override
    protected void onDisable() { set(0.6); }

    private void set(double value) {
        var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (attr != null) attr.setBaseValue(value);
    }
}
