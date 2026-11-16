package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * KillAura – Attacks targets in range.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class KillAura extends Module {
    public KillAura() {
        super("KillAura", Category.COMBAT, "target", "Atakuje cele w zasięgu", "Attacks targets in range");
        place(Category.COMBAT, "Aura");
        add(
                choice("Mode", "Single", "Switch", "Multi"),
                choice("Rotations", "Silent", "Legit", "Snap", "None"),
                choice("Priority", "Distance", "Health", "Angle", "Armor", "Hurt Time"),
                slider("Range", 3.5, 3.0, 6.0, 0.05),
                slider("Search Range", 5.0, 3.0, 8.0, 0.1),
                slider("Rotation Speed", 180, 10, 360, 5),
                slider("FOV", 180, 30, 360, 5),
                slider("Min CPS", 9, 1, 20, 1),
                slider("Max CPS", 13, 1, 20, 1),
                slider("Switch Delay", 250, 0, 1000, 10),
                toggle("Rage Mode", false),
                toggle("Through Walls", false),
                toggle("Raytrace", true),
                toggle("Keep Sprint", true),
                choice("Swing", "Client", "Server", "None"),
                choice("Auto Block", "Off", "Fake", "Real"),
                toggle("Players", true),
                toggle("Mobs", false),
                toggle("Animals", false),
                toggle("Invisibles", false),
                toggle("Ignore Friends", true),
                toggle("Only Weapon", false),
                toggle("Pause On Eat", true)
        );
    }
}
