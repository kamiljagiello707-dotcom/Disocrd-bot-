package dev.vesc.client.module;

import dev.vesc.client.VescClient;
import dev.vesc.client.lang.Lang;
import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;
import dev.vesc.client.setting.Setting;
import dev.vesc.client.ui.Anim;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Moduł klienta. Moduły kosmetyczne / legit mają logikę (patrz {@link Legit} i podklasy),
 * moduły typu cheat są wyłącznie przełącznikami w GUI.
 */
public class Module {
    private final String name;
    private final String descPl, descEn;
    private Category category;
    /** podgrupa w kategorii (np. "Entities") */
    private String group = "";
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

    // krótkie fabryki ustawień dla klas modułów (nazwa = nazwa angielska)
    protected static BoolSetting toggle(String name, boolean v) { return new BoolSetting(name, name, v); }
    protected static NumberSetting slider(String name, double v, double min, double max, double step) { return new NumberSetting(name, name, v, min, max, step); }
    protected static ModeSetting choice(String name, String... modes) { return new ModeSetting(name, name, modes); }

    public void toggle() { setEnabled(!enabled, false); }

    public void setEnabled(boolean state, boolean byKey) {
        if (state == enabled) return;
        enabled = state;
        if (enabled) onEnable(); else onDisable();
        if (VescClient.get().modules != null) VescClient.get().modules.noteToggle(this);
        VescClient.get().notifications.pushModule(this, byKey);
    }

    /** ustawia stan bez powiadomienia (wczytywanie configu) */
    public void setEnabledSilent(boolean state) { enabled = state; }

    protected void onEnable() {}
    protected void onDisable() {}

    public String getName() { return name; }
    public String getDescription() { return Lang.pl() ? descPl : descEn; }
    public Category getCategory() { return category; }
    public String getGroup() { return group; }
    /** przenosi moduł do kategorii i podgrupy */
    public Module place(Category c, String g) { this.category = c; this.group = g; return this; }
    public String getIcon() { return icon; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean hasKey() { return key != GLFW.GLFW_KEY_UNKNOWN; }
    public List<Setting<?>> getSettings() { return settings; }

    /** ustawienie po angielskiej nazwie */
    public Setting<?> setting(String en) {
        String id = en.toLowerCase().replace(' ', '_');
        for (Setting<?> s : settings) if (s.getId().equals(id)) return s;
        throw new IllegalArgumentException(name + ": no setting " + en);
    }
    public double num(String en) { return ((Number) setting(en).get()).doubleValue(); }
    public boolean bool(String en) { return (Boolean) setting(en).get(); }
    public String mode(String en) { return (String) setting(en).get(); }
}
