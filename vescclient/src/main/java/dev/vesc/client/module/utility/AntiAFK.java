package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AntiAFK – Prevents AFK kicks.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AntiAFK extends Module {
    public AntiAFK() {
        super("AntiAFK", Category.UTILITY, "coffee", "Zapobiega wyrzuceniu za AFK", "Prevents AFK kicks");
        place(Category.UTILITY, "Server");
        add(
                choice("Mode", "Jump", "Rotate", "Walk", "Swing"),
                slider("Interval", 30, 5, 300, 5),
                toggle("Random", true)
        );
    }
}
