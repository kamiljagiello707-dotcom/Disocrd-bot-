package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Criticals – Critical hits.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Criticals extends Module {
    public Criticals() {
        super("Criticals", Category.COMBAT, "bolt", "Uderzenia krytyczne", "Critical hits");
        place(Category.COMBAT, "Tweaks");
        add(
                choice("Mode", "Packet", "Jump", "Mini Jump", "Strict"),
                slider("Hurt Time", 10, 0, 10, 1),
                toggle("Only KillAura", false),
                toggle("Only Weapon", true),
                toggle("Ignore In Air", true)
        );
    }
}
