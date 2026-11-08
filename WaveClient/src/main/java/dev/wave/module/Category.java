package dev.wave.module;

public enum Category {
    VISUALS("Visuals", "✦"),
    MOVEMENT("Movement", "➤"),
    PLAYER("Player", "☺"),
    HUD("HUD", "▣"),
    CLIENT("Client", "⚙");

    public final String displayName;
    public final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
}
