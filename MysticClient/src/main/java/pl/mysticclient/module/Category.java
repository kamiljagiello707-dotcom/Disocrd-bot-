package pl.mysticclient.module;

public enum Category {
    COMBAT("Combat", "⚔"),
    MOVEMENT("Movement", "➶"),
    RENDER("Render", "◉"),
    PLAYER("Player", "☻"),
    WORLD("World", "☀"),
    MISC("Misc", "✿");

    public final String displayName;
    public final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
}
