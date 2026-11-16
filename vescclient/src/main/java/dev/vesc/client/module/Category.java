package dev.vesc.client.module;

import dev.vesc.client.lang.Lang;

import java.util.List;

/** Kategorie modułów (VescClient 4.0) – każda z opisem, kolorem i podgrupami. */
public enum Category {
    COMBAT("cat.combat", "combat", 0xFFFF5C7A, "Aura", "Assist", "Tweaks", "Defense"),
    MOVEMENT("cat.movement", "movement", 0xFF38D9C0, "Ground", "Air", "Terrain"),
    PLAYER("cat.player", "player", 0xFFFFA94D, "Inventory", "Actions", "Survival"),
    VISUALS("cat.visuals", "render", 0xFF7C8CFF, "Entities", "Blocks", "Overlays"),
    CAMERA("cat.camera", "i_camera", 0xFF4DABF7, "View", "Hand", "Effects"),
    WORLD("cat.world", "world", 0xFF51CF66, "Atmosphere", "Building", "Automation"),
    COSMETICS("cat.cosmetics", "shirt", 0xFFF783AC, "Wearables", "Effects", "Items", "Sounds"),
    UTILITY("cat.utility", "i_wrench", 0xFFB197FC, "Chat", "Social", "Server", "Privacy", "Tools");

    private final String key;
    public final String icon;
    /** kolor kategorii (kafelek ikony, nagłówek strony) */
    public final int color;
    /** podgrupy w kolejności wyświetlania */
    public final List<String> groups;

    Category(String key, String icon, int color, String... groups) {
        this.key = key; this.icon = icon; this.color = color; this.groups = List.of(groups);
    }

    public String getName() { return Lang.get(key); }
    public String getDescription() { return Lang.get(key + ".desc"); }
    public static String groupName(String g) { return Lang.get("group." + g.toLowerCase()); }
}
