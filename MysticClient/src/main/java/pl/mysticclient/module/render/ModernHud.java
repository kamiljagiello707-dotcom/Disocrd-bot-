package pl.mysticclient.module.render;

import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.misc.ClientSettings;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;

/** Wspólne elementy HUD. */
public final class ModernHud {
    private ModernHud() {}

    public static float scale() {
        ClientSettings cs = ModuleManager.get(ClientSettings.class);
        float s = cs == null ? 1f : cs.hudScale.getFloat();
        return UI.autoScale() * 1.15f * s;
    }

    /** Karta "szklana" z cieniem i cienką ramką. */
    public static void card(float x, float y, float w, float h, float r) {
        UI.shadow(x, y + 2, w, h, r, 10, UI.withAlpha(0, Theme.light ? 30 : 90));
        UI.round(x, y, w, h, r, Theme.glass());
        UI.outline(x, y, w, h, r, 1, Theme.glassBorder());
    }
}
