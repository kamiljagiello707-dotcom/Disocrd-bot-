package tech.fuzir.client.module;

public enum Category {
    MOVEMENT("Ruch", "R"),
    RENDER("Render", "V"),
    HUD("HUD", "H"),
    CLIENT("Klient", "K");

    public final String title;
    public final String icon;

    Category(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }
}
