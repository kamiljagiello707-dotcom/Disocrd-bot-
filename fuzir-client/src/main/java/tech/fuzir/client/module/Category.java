package tech.fuzir.client.module;

import tech.fuzir.client.util.Icons;

public enum Category {
    MOVEMENT("Ruch", "Poruszanie się postaci", Icons.FOOTPRINTS),
    PLAYER("Gracz", "Automatyzacja i wygoda", Icons.USER),
    VISUALS("Wizualne", "Wygląd świata i kamery", Icons.EYE),
    HUD("HUD", "Elementy na ekranie gry", Icons.DASHBOARD),
    OTHER("Inne", "Ustawienia menu i klienta", Icons.SLIDERS);

    public final String title;
    public final String subtitle;
    public final char icon;

    Category(String title, String subtitle, char icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }
}
