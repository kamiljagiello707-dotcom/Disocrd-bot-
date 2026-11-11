package tech.fuzir.client.module.impl;

import net.minecraft.util.Identifier;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.util.Icons;

import java.util.Locale;

/** Własna peleryna widoczna u Ciebie w grze (tylko po stronie klienta). */
public class Cape extends Module {
    public final ModeSetting style = add(new ModeSetting("Wzór", "Fuzir", "Fuzir", "Monochrom", "Północ", "Zachód"));
    public final BoolSetting elytra = add(new BoolSetting("Wzór na elytrze", true));

    public Cape() {
        super("Peleryna", "Własna peleryna z logo Fuzir", Category.VISUALS, Icons.SHIRT);
    }

    public Identifier texture() {
        String file = style.get().toLowerCase(Locale.ROOT).replace("ó", "o").replace("ł", "l");
        return Identifier.of("fuzir-client", "textures/cape/" + file + ".png");
    }

    @Override
    public String suffix() {
        return style.get();
    }
}
