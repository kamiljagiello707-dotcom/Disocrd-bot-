package tech.fuzir.client.module.impl.render;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.impl.BoolSetting;
import tech.fuzir.client.setting.impl.NumberSetting;

public class NameTags extends Module {

    public BoolSetting background = new BoolSetting("Background", true);
    public BoolSetting armor = new BoolSetting("Armor", true);
    public BoolSetting health = new BoolSetting("Health", true);
    public NumberSetting scale = new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.1);

    public NameTags() {
        super("NameTags", "Enhances player name tags in the world.", Category.RENDER);
        this.addSettings(background, armor, health, scale);
    }
}
