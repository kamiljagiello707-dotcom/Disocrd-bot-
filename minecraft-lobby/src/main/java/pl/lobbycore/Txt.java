package pl.lobbycore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

/** Tekst z kodami &x i &#RRGGBB -> Component. */
final class Txt {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private Txt() {}

    static Component c(String s) {
        return LEGACY.deserialize(s == null ? "" : s).decoration(TextDecoration.ITALIC, false);
    }

    static String ph(String s, Map<String, String> values) {
        if (s == null) return "";
        for (var e : values.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue());
        return s;
    }
}
