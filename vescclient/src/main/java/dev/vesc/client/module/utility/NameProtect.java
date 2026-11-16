package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * NameProtect – Hides your name on screen.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class NameProtect extends Module {
    public NameProtect() {
        super("NameProtect", Category.UTILITY, "i_mask", "Ukrywa Twój nick na ekranie", "Hides your name on screen");
        place(Category.UTILITY, "Privacy");
        add(
                choice("Replace With", "You", "Player", "Stars"),
                toggle("Friends", false)
        );
    }
}
