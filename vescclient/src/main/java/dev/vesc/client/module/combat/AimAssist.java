package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AimAssist – Gently helps your aim.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AimAssist extends Module {
    public AimAssist() {
        super("AimAssist", Category.COMBAT, "i_scaneye", "Delikatnie pomaga celować", "Gently helps your aim");
        place(Category.COMBAT, "Assist");
        add(
                slider("Speed", 4.0, 0.5, 15.0, 0.5),
                slider("FOV", 90, 10, 360, 5),
                slider("Range", 4.5, 3.0, 8.0, 0.1),
                choice("Target Part", "Head", "Body", "Legs", "Closest"),
                toggle("Only On Click", true),
                toggle("Vertical", false),
                toggle("Through Walls", false),
                toggle("Ignore Friends", true)
        );
    }
}
