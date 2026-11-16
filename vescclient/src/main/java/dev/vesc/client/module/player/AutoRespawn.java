package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoRespawn – Respawns automatically.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", Category.PLAYER, "i_skull", "Automatyczne odrodzenie", "Respawns automatically");
        place(Category.PLAYER, "Survival");
        add(
                slider("Delay", 0, 0, 20, 1),
                toggle("Show Death Coords", true),
                toggle("Coords In Chat", false)
        );
    }
}
