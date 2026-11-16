package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * NoSlow – No slowdown.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class NoSlow extends Module {
    public NoSlow() {
        super("NoSlow", Category.PLAYER, "snail", "Brak spowolnienia", "No slowdown");
        place(Category.PLAYER, "Actions");
        add(
                choice("Mode", "Vanilla", "Packet", "Strict"),
                toggle("Food", true),
                toggle("Bow", true),
                toggle("Shield", true),
                toggle("Soul Sand", false),
                toggle("Cobwebs", false),
                toggle("Sneak", false)
        );
    }
}
