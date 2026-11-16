package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * HitBoxes – Bigger target hitboxes.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class HitBoxes extends Module {
    public HitBoxes() {
        super("HitBoxes", Category.COMBAT, "box", "Większe hitboxy celów", "Bigger target hitboxes");
        place(Category.COMBAT, "Tweaks");
        add(
                slider("Expand", 0.2, 0.0, 1.0, 0.05),
                toggle("Players", true),
                toggle("Mobs", false),
                toggle("Render", false),
                toggle("Ignore Friends", true)
        );
    }
}
