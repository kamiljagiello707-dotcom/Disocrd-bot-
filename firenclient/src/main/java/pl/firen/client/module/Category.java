package pl.firen.client.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    WORLD("World");

    public final String display;

    Category(String display) { this.display = display; }
}
