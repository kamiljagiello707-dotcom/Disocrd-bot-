package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * MiddleClick – Add friends with middle click.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class MiddleClick extends Module {
    public MiddleClick() {
        super("MiddleClick", Category.UTILITY, "player", "Znajomi pod kółkiem myszy", "Add friends with middle click");
        place(Category.UTILITY, "Social");
        add(
                toggle("Notify", true),
                toggle("Only Players", true)
        );
    }
}
