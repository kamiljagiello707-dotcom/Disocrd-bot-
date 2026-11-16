package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoArmor – Equips the best armor.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AutoArmor extends Module {
    public AutoArmor() {
        super("AutoArmor", Category.PLAYER, "shield", "Zakłada najlepszą zbroję", "Equips the best armor");
        place(Category.PLAYER, "Inventory");
        add(
                slider("Delay", 2, 0, 10, 1),
                choice("Priority", "Protection", "Blast", "Projectile"),
                toggle("Open Inventory", false),
                toggle("Elytra Priority", false),
                toggle("Ignore Curse", true)
        );
    }
}
