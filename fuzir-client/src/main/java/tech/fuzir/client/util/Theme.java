package tech.fuzir.client.util;

import java.util.List;

/** Motyw kolorów. Zmiana motywu przechodzi płynnie przez wszystkie kolory. */
public record Theme(String name, int bg, int sidebar, int surface, int surface2, int line, int text, int muted, int dim,
                    int accent, int onAccent, int panel) {

    public static final List<Theme> ALL = List.of(
            new Theme("Monochrom", 0xF40B0B0D, 0xFF0E0E11, 0xFF141418, 0xFF1C1C21, 0x15FFFFFF, 0xFFF2F2F3, 0xFF8B8B92, 0xFF55555C, 0xFFFFFFFF, 0xFF000000, 0xD90C0C0E),
            new Theme("Północ", 0xF4111327, 0xFF14162D, 0xFF1A1D39, 0xFF232748, 0x1AFFFFFF, 0xFFEDEEFF, 0xFF8F93BC, 0xFF5B5F88, 0xFF8F80FF, 0xFFFFFFFF, 0xD9111327),
            new Theme("Wiśnia", 0xF4140E13, 0xFF181017, 0xFF21151E, 0xFF2C1C28, 0x18FFFFFF, 0xFFFFF1F6, 0xFFB792A7, 0xFF76596A, 0xFFFF8FB9, 0xFF1A0911, 0xD9140E13),
            new Theme("Ocean", 0xF40A1117, 0xFF0D151C, 0xFF121D26, 0xFF182632, 0x18FFFFFF, 0xFFEAF6FF, 0xFF86A2B6, 0xFF52697A, 0xFF5CC8FF, 0xFF041018, 0xD90A1117),
            new Theme("Las", 0xF40C110D, 0xFF0F1510, 0xFF151D16, 0xFF1C271D, 0x18FFFFFF, 0xFFEFFAF0, 0xFF8EA892, 0xFF5A6E5D, 0xFF6EE28A, 0xFF06140A, 0xD90C110D),
            new Theme("Grafit", 0xF41A1A1E, 0xFF1E1E23, 0xFF25252B, 0xFF2E2E35, 0x18FFFFFF, 0xFFF4F4F6, 0xFF9A9AA3, 0xFF64646C, 0xFFD6D6DC, 0xFF111114, 0xD91A1A1E));

    private static Theme from = ALL.get(0), to = ALL.get(0);
    private static final Anim BLEND = new Anim(1);
    private static Theme cached = ALL.get(0);
    private static long cachedAt = -1;

    public static Theme byName(String name) {
        for (Theme t : ALL) if (t.name.equals(name)) return t;
        return ALL.get(0);
    }

    /** Ustawia motyw docelowy; kolory przejdą do niego płynnie. */
    public static void select(String name) {
        Theme next = byName(name);
        if (next == to) return;
        from = current();
        to = next;
        BLEND.set(0);
    }

    /** Ustawia motyw od razu, bez animacji (wczytywanie configu). */
    public static void apply(String name) {
        from = to = cached = byName(name);
        BLEND.set(1);
    }

    public static Theme target() {
        return to;
    }

    /** Aktualne kolory (w trakcie zmiany motywu: mieszanka starego i nowego). */
    public static Theme current() {
        long frame = System.nanoTime() / 4_000_000L;
        if (frame == cachedAt) return cached;
        cachedAt = frame;
        float t = Anim.easeOutCubic(BLEND.to(1, 7));
        if (t >= 1f) return cached = to;
        cached = new Theme(to.name,
                Canvas.mix(from.bg, to.bg, t), Canvas.mix(from.sidebar, to.sidebar, t), Canvas.mix(from.surface, to.surface, t),
                Canvas.mix(from.surface2, to.surface2, t), Canvas.mix(from.line, to.line, t), Canvas.mix(from.text, to.text, t),
                Canvas.mix(from.muted, to.muted, t), Canvas.mix(from.dim, to.dim, t), Canvas.mix(from.accent, to.accent, t),
                Canvas.mix(from.onAccent, to.onAccent, t), Canvas.mix(from.panel, to.panel, t));
        return cached;
    }
}
