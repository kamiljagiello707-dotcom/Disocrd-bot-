package dev.vesc.client.module.cosmetics;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import dev.vesc.client.setting.ModeSetting;
import net.minecraft.util.Identifier;

/** Cosmetic – własna peleryna z logo VescClient (widoczna u Ciebie). */
public class Cosmetic extends Module {
    public final ModeSetting cape = new ModeSetting("Peleryna", "Cape", "Midnight", "Black", "Purple");

    public Cosmetic() {
        super("Cosmetic", Category.UTILITY, "shirt", "Peleryna z logo VescClient", "Cape with the VescClient logo");
        place(Category.COSMETICS, "Wearables");
        add(cape);
    }

    public Identifier capeTexture() {
        String f = switch (cape.get()) {
            case "Black" -> "cape_black";
            case "Purple" -> "cape_purple";
            default -> "cape_midnight";
        };
        return Identifier.of("vescclient", "textures/ui/" + f + ".png");
    }
}
