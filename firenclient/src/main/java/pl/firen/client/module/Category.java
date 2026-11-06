package pl.firen.client.module;

public enum Category {
    COMBAT("Combat", "\uf05b"),
    MOVEMENT("Movement", "\uf70c"),
    RENDER("Render", "\uf06e"),
    PLAYER("Player", "\uf007"),
    WORLD("World", "\uf57d");

    public final String display;
    /** Ikona Font Awesome. */
    public final String icon;

    Category(String display, String icon) {
        this.display = display;
        this.icon = icon;
    }
}
