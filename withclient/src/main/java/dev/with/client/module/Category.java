package dev.with.client.module;

import dev.with.client.ui.Ui;

/** Kategorie modułów (nazwa PL/EN + ikona). */
public enum Category {
    COMBAT("Walka", "Combat", "g_combat"),
    MOVEMENT("Ruch", "Movement", "g_movement"),
    PLAYER("Gracz", "Player", "g_player"),
    VISUALS("Wizualne", "Visuals", "g_render"),
    WORLD("Świat", "World", "g_world"),
    EXPLOITS("Exploity", "Exploits", "g_misc"),
    CLIENT("Klient", "Client", "g_gear");

    private final String pl, en;
    public final String icon;

    Category(String pl, String en, String icon) { this.pl = pl; this.en = en; this.icon = icon; }

    public String title() { return Ui.t(pl, en); }
}
