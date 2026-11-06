package pl.firen.client.render;

import java.util.HashMap;
import java.util.Map;

/** Encje z poświatą (vanilla outline) i ich kolory – wypełniane co tick przez ESP / Target ESP. */
public final class OutlineManager {
    private OutlineManager() {}

    private static final Map<Integer, Integer> GLOW = new HashMap<>();

    public static void clear() { GLOW.clear(); }
    public static void add(int entityId, int rgb) { GLOW.put(entityId, rgb & 0xFFFFFF); }
    public static boolean has(int entityId) { return GLOW.containsKey(entityId); }
    public static int color(int entityId) { return GLOW.getOrDefault(entityId, 0xFFFFFF); }
}
