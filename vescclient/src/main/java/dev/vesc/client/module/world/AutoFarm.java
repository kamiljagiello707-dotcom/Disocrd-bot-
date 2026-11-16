package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoFarm – Harvests and replants crops.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AutoFarm extends Module {
    public AutoFarm() {
        super("AutoFarm", Category.WORLD, "i_sprout", "Zbiera i sadzi rośliny", "Harvests and replants crops");
        place(Category.WORLD, "Automation");
        add(
                slider("Range", 4, 1, 6, 1),
                slider("Delay", 2, 0, 10, 1),
                toggle("Harvest", true),
                toggle("Replant", true),
                toggle("Bonemeal", false),
                toggle("Rotate", true)
        );
    }
}
