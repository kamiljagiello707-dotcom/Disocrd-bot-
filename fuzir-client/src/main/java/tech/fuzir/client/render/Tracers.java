package tech.fuzir.client.module.impl.render;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.impl.BoolSetting;
import tech.fuzir.client.setting.impl.NumberSetting;

public class Tracers extends Module {

    public NumberSetting thickness = new NumberSetting("Thickness", 1.0, 0.5, 5.0, 0.5);
    public BoolSetting onlyPlayers = new BoolSetting("Only Players", true);
    public BoolSetting linesToCenter = new BoolSetting("To Center", false);

    public Tracers() {
        super("Tracers", "Draws lines pointing to entities.", Category.RENDER);
        this.addSettings(thickness, onlyPlayers, linesToCenter);
    }
}
