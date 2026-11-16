package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * NoFall – No fall damage.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class NoFall extends Module {
    public NoFall() {
        super("NoFall", Category.MOVEMENT, "nofall", "Brak obrażeń od upadku", "No fall damage");
        place(Category.MOVEMENT, "Ground");
        add(
                choice("Mode", "Packet", "Ground", "Bucket", "Elytra"),
                slider("Min Fall", 3, 2, 20, 1),
                toggle("Only Damage", true),
                toggle("Pause In Creative", true)
        );
    }
}
