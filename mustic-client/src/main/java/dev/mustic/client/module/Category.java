package dev.mustic.client.module;

public enum Category {
    COMBAT("Combat", "⚔"),
    MOVEMENT("Movement", "➤"),
    RENDER("Render", "☀"),
    PLAYER("Player", "☺"),
    MISC("Misc", "✦"),
    CLIENT("Client", "⚙");

    public final String displayName;
    public final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
}
