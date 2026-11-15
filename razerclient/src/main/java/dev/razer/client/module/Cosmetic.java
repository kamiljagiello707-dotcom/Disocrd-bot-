package dev.razer.client.module;

import dev.razer.client.setting.ModeSetting;
import net.minecraft.util.Identifier;

/** Cosmetic – własna peleryna z logo RazerClient (widoczna u Ciebie). */
public class Cosmetic extends Module {
    public final ModeSetting cape = new ModeSetting("Peleryna", "Cape", "Midnight", "Czarna", "Fiolet");

    public Cosmetic() {
        super("Cosmetic", Category.MISC, "shirt", "Peleryna z logo RazerClient", "Cape with the RazerClient logo");
        add(cape);
    }

    public Identifier capeTexture() {
        String f = switch (cape.get()) {
            case "Czarna" -> "cape_black";
            case "Fiolet" -> "cape_purple";
            default -> "cape_midnight";
        };
        return Identifier.of("razerclient", "textures/ui/" + f + ".png");
    }
}
