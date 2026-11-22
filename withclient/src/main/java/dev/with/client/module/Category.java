package dev.with.client.module;

import dev.with.client.ui.Ui;

/** Kategorie modułów (nazwa PL/EN + ikona Lucide). */
public enum Category {
    COMBAT("Walka", "Combat", "l_swords"),
    MOVEMENT("Ruch", "Movement", "l_footprints"),
    PLAYER("Gracz", "Player", "l_user_round"),
    VISUALS("Wizualne", "Visuals", "l_eye"),
    WORLD("Świat", "World", "l_globe"),
    HUD("HUD", "HUD", "l_layout_dashboard"),
    COSMETICS("Kosmetyki", "Cosmetics", "l_sparkles"),
    UTILITY("Narzędzia", "Utility", "l_wrench"),
    EXPLOITS("Exploity", "Exploits", "l_bug"),
    CLIENT("Klient", "Client", "l_monitor_cog");

    private final String pl, en;
    public final String icon;

    Category(String pl, String en, String icon) { this.pl = pl; this.en = en; this.icon = icon; }

    public String title() { return Ui.t(pl, en); }
}
