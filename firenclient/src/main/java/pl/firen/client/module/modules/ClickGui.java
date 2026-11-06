package pl.firen.client.module.modules;

import pl.firen.client.gui.ClickGuiScreen;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.ModeSetting;

public class ClickGui extends Module {
    public final ModeSetting theme = add(new ModeSetting("Theme", "Mono", "Mono", "Ocean", "Rose", "Mint", "Sunset", "Violet", "Custom"));
    public final ColorSetting custom = add(new ColorSetting("Accent", 0xFF4DA3FF));
    public final BooleanSetting blur = add(new BooleanSetting("Blur", true));

    public ClickGui() {
        super("ClickGUI", "Motyw i kolor akcentu. Klawisz: Opcje → Sterowanie.", Category.RENDER);
        custom.visibleWhen(() -> theme.is("Custom"));
    }

    public int accent() {
        return switch (theme.get()) {
            case "Ocean" -> 0xFF4DA3FF;
            case "Rose" -> 0xFFFF5C8A;
            case "Mint" -> 0xFF3DDC97;
            case "Sunset" -> 0xFFFF8A3D;
            case "Violet" -> 0xFF9D7CFF;
            case "Custom" -> custom.get() | 0xFF000000;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    public void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
