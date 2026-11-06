package pl.firen.client.module.modules;

import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;

/** Brak spowolnienia przy jedzeniu, piciu, blokowaniu tarczą i naciąganiu łuku. */
public class NoSlow extends Module {
    public NoSlow() { super("NoSlow", "Brak spowolnienia przy używaniu przedmiotów.", Category.MOVEMENT); }

    public static boolean active() {
        var m = FirenClient.modules();
        return m != null && m.get(NoSlow.class).isEnabled();
    }
}
