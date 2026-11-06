package pl.firen.client.module.modules;

import pl.firen.client.gui.ClickGuiScreen;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;

public class ClickGui extends Module {
    public final BooleanSetting blur = add(new BooleanSetting("Blur", true));

    public ClickGui() {
        super("ClickGUI", "Otwiera menu. Klawisz zmienisz w Opcje → Sterowanie.", Category.RENDER);
    }

    @Override
    public void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
