package pl.firen.client.module.modules;

import net.minecraft.entity.attribute.EntityAttributes;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class Step extends Module {
    public final NumberSetting height = add(new NumberSetting("Height", 1.0, 0.6, 2.5, 0.1));

    public Step() { super("Step", "Wchodzi na wyższe bloki bez skakania.", Category.MOVEMENT); }

    @Override
    public void onTick() {
        var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr != null) attr.setBaseValue(mc.player.isSneaking() ? 0.6 : height.get());
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr != null) attr.setBaseValue(0.6);
    }
}
