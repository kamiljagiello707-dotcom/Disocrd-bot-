package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoGG – Says GG after a win.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class AutoGG extends Module {
    public AutoGG() {
        super("AutoGG", Category.UTILITY, "i_trophy", "Pisze GG po wygranej", "Says GG after a win");
        place(Category.UTILITY, "Chat");
        add(
                choice("Message", "gg", "GG", "good game", "gg wp"),
                slider("Delay", 1, 0, 10, 1),
                toggle("Only Win", true)
        );
    }
}
