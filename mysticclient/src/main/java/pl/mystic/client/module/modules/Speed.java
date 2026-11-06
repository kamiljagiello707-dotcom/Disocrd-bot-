package pl.mystic.client.module.modules;

import net.minecraft.entity.attribute.EntityAttributes;
import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Speed extends Module {
    private static final double NORMAL = 0.1;
    private static final double FAST = 0.16;

    public Speed() { super("Speed", "Szybsze chodzenie", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN); }

    @Override
    public void onTick() { set(FAST); }

    @Override
    protected void onDisable() { set(NORMAL); }

    private void set(double value) {
        var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attr != null) attr.setBaseValue(value);
    }
}
