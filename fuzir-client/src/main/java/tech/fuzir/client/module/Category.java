package tech.fuzir.client.module;

/**
 * Module categories. Each one gets its own draggable panel in the ClickGUI.
 * Icons are BMP symbols resolved through the vanilla unifont fallback of the client font.
 */
public enum Category {
    COMBAT("Combat", "⚔"),
    MOVEMENT("Movement", "➤"),
    RENDER("Render", "◈"),
    PLAYER("Player", "☺"),
    WORLD("World", "◎"),
    MISC("Misc", "✿"),
    HUD("Hud", "▣");

    private final String displayName;
    private final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
