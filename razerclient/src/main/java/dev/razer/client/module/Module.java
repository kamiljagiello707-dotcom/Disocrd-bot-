package dev.razer.client.module;

import dev.razer.client.RazerClient;
import dev.razer.client.lang.Lang;
import dev.razer.client.setting.Setting;
import dev.razer.client.ui.Anim;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Moduł klienta. To jest tylko szkielet GUI – moduły nie mają żadnej logiki,
 * przełączają jedynie swój stan (onEnable/onDisable są puste).
 */
public class Module {
    private final String name;
    private final String descPl, descEn;
    private final Category category;
    private final String icon;
    private boolean enabled;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private final List<Setting<?>> settings = new ArrayList<>();

    // animacje GUI / HUD
    public final Anim toggleAnim = new Anim(0, 16);
    public final Anim hoverAnim = new Anim(0, 18);
    public final Anim expandAnim = new Anim(0, 14);
    public final Anim hudAnim = new Anim(0, 12);
    public boolean expanded;
    public boolean favorite;
    public final Anim favAnim = new Anim(0, 16);

    public Module(String name, Category category, String icon, String descPl, String descEn) {
        this.name = name; this.category = category; this.icon = icon; this.descPl = descPl; this.descEn = descEn;
    }

    public Module add(Setting<?>... s) { settings.addAll(List.of(s)); return this; }

    public void toggle() { setEnabled(!enabled, false); }

    public void setEnabled(boolean state, boolean byKey) {
        if (state == enabled) return;
        enabled = state;
        if (enabled) onEnable(); else onDisable();
        RazerClient.get().notifications.pushModule(this, byKey);
    }

    /** ustawia stan bez powiadomienia (wczytywanie configu) */
    public void setEnabledSilent(boolean state) { enabled = state; }

    protected void onEnable() {}
    protected void onDisable() {}

    public String getName() { return name; }
    public String getDescription() { return Lang.pl() ? descPl : descEn; }
    public Category getCategory() { return category; }
    public String getIcon() { return icon; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean hasKey() { return key != GLFW.GLFW_KEY_UNKNOWN; }
    public List<Setting<?>> getSettings() { return settings; }
}
