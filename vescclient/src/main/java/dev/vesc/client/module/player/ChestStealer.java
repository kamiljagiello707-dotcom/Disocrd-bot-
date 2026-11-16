package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * ChestStealer – Quickly takes items.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class ChestStealer extends Module {
    public ChestStealer() {
        super("ChestStealer", Category.PLAYER, "i_archive", "Szybko wyjmuje przedmioty", "Quickly takes items");
        place(Category.PLAYER, "Inventory");
        add(
                slider("Delay", 80, 0, 500, 10),
                toggle("Random Delay", true),
                toggle("Auto Close", true),
                toggle("Only Useful", true),
                toggle("Ignore Custom Names", true)
        );
    }
}
