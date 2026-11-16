package dev.vesc.client.module.camera;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.NumberSetting;

/** ViewModel – pozycja, skala i animacja przedmiotu w ręce (tylko wygląd z pierwszej osoby). */
public class ViewModel extends Module {
    public final NumberSetting x = new NumberSetting("Pozycja X", "X", 0, -1, 1, 0.02);
    public final NumberSetting y = new NumberSetting("Pozycja Y", "Y", 0, -1, 1, 0.02);
    public final NumberSetting z = new NumberSetting("Pozycja Z", "Z", 0, -1, 1, 0.02);
    public final NumberSetting scale = new NumberSetting("Skala", "Scale", 1, 0.3, 1.5, 0.05);
    public final BoolSetting offhand = new BoolSetting("Druga ręka", "Offhand", true);
    public final BoolSetting noEquip = new BoolSetting("Bez animacji wyjmowania", "No Equip Animation", false);

    public ViewModel() {
        super("ViewModel", Category.CAMERA, "i_hand", "Pozycja i skala przedmiotu w ręce", "Position and scale of held items");
        place(Category.CAMERA, "Hand");
        add(x, y, z, scale, offhand, noEquip);
    }
}
