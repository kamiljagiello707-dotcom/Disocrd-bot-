package dev.testclient.module;

public enum Category {
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player");

    public final String displayName;

    Category(String displayName) { this.displayName = displayName; }
}
