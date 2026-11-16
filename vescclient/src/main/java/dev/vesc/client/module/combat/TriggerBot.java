package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * TriggerBot – Attacks what's under the crosshair.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class TriggerBot extends Module {
    public TriggerBot() {
        super("TriggerBot", Category.COMBAT, "click", "Atak po najechaniu", "Attacks what's under the crosshair");
        place(Category.COMBAT, "Aura");
        add(
                slider("Min CPS", 8, 1, 20, 1),
                slider("Max CPS", 12, 1, 20, 1),
                slider("Delay", 50, 0, 500, 5),
                choice("Timing", "CPS", "Cooldown", "1.8"),
                toggle("Players", true),
                toggle("Mobs", false),
                toggle("Only Weapon", false),
                toggle("Ignore Friends", true)
        );
    }
}
