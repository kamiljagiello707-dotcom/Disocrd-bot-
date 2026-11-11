package tech.fuzir.client.module.impl.render;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.impl.BoolSetting;
import tech.fuzir.client.setting.impl.ModeSetting;

public class ESP extends Module {

    public ModeSetting type = new ModeSetting("Type", "Box", "Box", "Corners", "2D");
    public BoolSetting players = new BoolSetting("Players", true);
    public BoolSetting monsters = new BoolSetting("Monsters", false);
    public BoolSetting items = new BoolSetting("Items", false);

    public ESP() {
        super("ESP", "Highlights entities through walls.", Category.RENDER);
        this.addSettings(type, players, monsters, items);
    }
}
