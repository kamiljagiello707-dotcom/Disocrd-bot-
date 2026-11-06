package pl.mystic.client.module;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), PLAYER("Player");

    public final String title;
    Category(String title) { this.title = title; }
}
