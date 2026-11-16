package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * ChatSuffix – Adds a suffix to messages.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class ChatSuffix extends Module {
    public ChatSuffix() {
        super("ChatSuffix", Category.UTILITY, "i_msgplus", "Dopisek do wiadomości", "Adds a suffix to messages");
        place(Category.UTILITY, "Chat");
        add(
                choice("Suffix", "| VescClient", "» Vesc", "vesc.gg"),
                toggle("Small Font", true)
        );
    }
}
