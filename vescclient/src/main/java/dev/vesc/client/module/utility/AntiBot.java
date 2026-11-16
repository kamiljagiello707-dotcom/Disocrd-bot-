package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AntiBot – Ignores server bots.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AntiBot extends Module {
    public AntiBot() {
        super("AntiBot", Category.UTILITY, "i_bot", "Ignoruje boty serwera", "Ignores server bots");
        place(Category.UTILITY, "Server");
        add(
                choice("Mode", "Tab", "Ping", "Advanced"),
                toggle("Hide In Tab", false),
                toggle("Remove", false)
        );
    }
}
