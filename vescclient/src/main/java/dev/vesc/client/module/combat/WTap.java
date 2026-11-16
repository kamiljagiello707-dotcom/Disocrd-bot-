package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * WTap – Resets sprint after each hit.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class WTap extends Module {
    public WTap() {
        super("WTap", Category.COMBAT, "footprints", "Resetuje sprint po uderzeniu", "Resets sprint after each hit");
        place(Category.COMBAT, "Tweaks");
        add(
                choice("Mode", "Packet", "Legit", "SneakTap"),
                slider("Delay", 2, 0, 10, 1),
                slider("Duration", 1, 1, 5, 1),
                slider("Chance", 100, 0, 100, 1),
                toggle("Only Players", true)
        );
    }
}
