package dev.razer.client.module;

import dev.razer.client.lang.Lang;

public enum Category {
    COMBAT("cat.combat", "combat"),
    MOVEMENT("cat.movement", "movement"),
    RENDER("cat.render", "render"),
    PLAYER("cat.player", "player"),
    WORLD("cat.world", "world"),
    MISC("cat.misc", "misc");

    private final String key;
    public final String icon;
    Category(String key, String icon) { this.key = key; this.icon = icon; }
    public String getName() { return Lang.get(key); }
}
