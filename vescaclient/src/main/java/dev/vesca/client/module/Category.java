package dev.vesca.client.module;

import dev.vesca.client.ui.Ui;

/** Kategorie modułów (nazwa PL/EN + ikona). */
public enum Category {
    COMBAT("Walka", "Combat", "g_combat"),
    MOVEMENT("Ruch", "Movement", "g_movement"),
    PLAYER("Gracz", "Player", "g_player"),
    RENDER("Wygląd", "Render", "g_render"),
    WORLD("Świat", "World", "g_world"),
    MISC("Inne", "Misc", "g_misc");

    private final String pl, en;
    public final String icon;

    Category(String pl, String en, String icon) { this.pl = pl; this.en = en; this.icon = icon; }

    public String title() { return Ui.t(pl, en); }
}
