package dev.vesca.client.module;

import dev.vesca.client.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Moduł widoczny w ClickGUI: nazwa, kategoria, ikona, opis, stan, klawisz i ustawienia.
 * Na razie same przełączniki i ustawienia – bez działania w grze.
 */
public class Module {
    public final String name, icon;
    public final Category category;
    private final String descPl, descEn;
    public final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    /** klawisz GLFW, -1 = brak */
    public int key = -1;
    public boolean favorite;

    public Module(String name, Category category, String icon, String descPl, String descEn) {
        this.name = name; this.category = category; this.icon = icon; this.descPl = descPl; this.descEn = descEn;
    }

    public String description() { return Ui.t(descPl, descEn); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean on) { enabled = on; }
    public void toggle() { setEnabled(!enabled); }

    public Module add(Setting<?>... s) { settings.addAll(List.of(s)); return this; }

    public void reset() {
        for (Setting<?> s : settings) s.reset();
        key = -1;
    }
}
