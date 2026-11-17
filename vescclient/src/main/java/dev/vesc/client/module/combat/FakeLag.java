package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * FakeLag – Holds back your movement to look laggy.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class FakeLag extends Module {
    public FakeLag() {
        super("FakeLag", Category.COMBAT, "wifi", "Udaje opóźnienie połączenia", "Simulates a laggy connection");
        place(Category.COMBAT, "Tweaks");
        add(
                choice("Mode", "Dynamic", "Constant", "Pulse", "Smart"),
                slider("Delay", 300, 50, 1000, 10),
                slider("Jitter", 50, 0, 300, 10),
                slider("Range", 6.0, 2.0, 10.0, 0.5),
                choice("Packets", "Movement", "All"),
                toggle("Only When Targeting", true),
                toggle("Flush On Attack", true),
                toggle("Flush On Damage", true),
                toggle("Show Server Position", true)
        );
    }
}
