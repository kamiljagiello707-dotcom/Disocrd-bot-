package dev.razer.client.ui;

import dev.razer.client.lang.Lang;
import dev.razer.client.render.R2D;

/** Biało-czarne motywy z płynnym przejściem kolorów. */
public final class Theme {
    public enum Preset {
        DARK("theme.dark",   0xFF0E0E11, 0xFF09090B, 0xFF16161A, 0xFF1F1F24, 0xFF26262C, 0xFFF4F4F6, 0xFF9C9CA5, 0xFF5D5D66, 0xFFFFFFFF, 0xFF0A0A0B, 0xFF2B2B31, 0xE60E0E11, 0xA0000000),
        LIGHT("theme.light", 0xFFF2F2F4, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFEAEAEE, 0xFFDEDEE3, 0xFF0E0E11, 0xFF63636C, 0xFFA3A3AC, 0xFF0E0E11, 0xFFFFFFFF, 0xFFD6D6DC, 0xEFFFFFFF, 0x50000000),
        AMOLED("theme.amoled", 0xFF000000, 0xFF000000, 0xFF0B0B0B, 0xFF151515, 0xFF1C1C1C, 0xFFFFFFFF, 0xFF8E8E8E, 0xFF4A4A4A, 0xFFFFFFFF, 0xFF000000, 0xFF222222, 0xEB000000, 0xB0000000);

        final int[] c;
        private final String key;
        Preset(String key, int... c) { this.key = key; this.c = c; }
        public String getName() { return Lang.get(key); }
        public int color(int i) { return c[i]; }
    }

    private static final int BG = 0, SIDEBAR = 1, CARD = 2, CARD_HOVER = 3, STROKE = 4, TEXT = 5, TEXT_DIM = 6,
            TEXT_FAINT = 7, ACCENT = 8, ACCENT_TEXT = 9, TOGGLE_OFF = 10, HUD_BG = 11, SHADOW = 12;

    /** kolor akcentu (MONO = biało-czarny z motywu), każdy ma 2 kolory do gradientu */
    public enum Accent {
        MONO("accent.mono", 0, 0),
        PURPLE("accent.purple", 0xFFA855F7, 0xFFEC4899),
        PINK("accent.pink", 0xFFEC4899, 0xFFF9A8D4),
        BLUE("accent.blue", 0xFF3B82F6, 0xFF22D3EE),
        CYAN("accent.cyan", 0xFF22D3EE, 0xFF34D399),
        GREEN("accent.green", 0xFF22C55E, 0xFFA3E635),
        RED("accent.red", 0xFFEF4444, 0xFFF97316),
        GOLD("accent.gold", 0xFFF59E0B, 0xFFFDE047);
        public final int a, b;
        private final String key;
        Accent(String key, int a, int b) { this.key = key; this.a = a; this.b = b; }
        public String getName() { return Lang.get(key); }
        public int first(Preset p) { return this == MONO ? p.c[ACCENT] : a; }
        public int second(Preset p) { return this == MONO ? p.c[TEXT_DIM] : b; }
    }

    private static Preset preset = Preset.DARK, from = Preset.DARK;
    private static Accent accent = Accent.MONO, fromAccent = Accent.MONO;
    private static long switchTime = 0, accentTime = 0;

    public static Accent accentPreset() { return accent; }

    public static void setAccent(Accent a) {
        if (a == accent) return;
        fromAccent = accent; accent = a; accentTime = System.currentTimeMillis();
    }

    public static void setAccentInstant(Accent a) { accent = a; fromAccent = a; }

    private static float accentT() {
        return Anim.easeOutCubic((System.currentTimeMillis() - accentTime) / (450f / Anim.globalSpeed));
    }

    /** drugi kolor gradientu */
    public static int accent2() {
        return R2D.blend(fromAccent.second(preset), accent.second(preset), accentT());
    }

    /** kolor akcentu "płynący" w czasie – do gradientów w HUD */
    public static int accentWave(float phase) {
        float t = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 600.0 + phase);
        return R2D.blend(accent(), accent2(), t);
    }

    private static float luminance(int c) {
        return (0.299f * ((c >> 16) & 0xFF) + 0.587f * ((c >> 8) & 0xFF) + 0.114f * (c & 0xFF)) / 255f;
    }

    private Theme() {}

    public static Preset preset() { return preset; }

    public static void set(Preset p) {
        if (p == preset) return;
        from = preset; preset = p; switchTime = System.currentTimeMillis();
    }

    public static void setInstant(Preset p) { preset = p; from = p; }

    private static int c(int i) {
        float t = Anim.easeOutCubic((System.currentTimeMillis() - switchTime) / (450f / Anim.globalSpeed));
        return R2D.blend(from.c[i], preset.c[i], t);
    }

    public static int bg() { return c(BG); }
    public static int sidebar() { return c(SIDEBAR); }
    public static int card() { return c(CARD); }
    public static int cardHover() { return c(CARD_HOVER); }
    public static int stroke() { return c(STROKE); }
    public static int text() { return c(TEXT); }
    public static int textDim() { return c(TEXT_DIM); }
    public static int textFaint() { return c(TEXT_FAINT); }
    public static int accent() {
        int base = c(ACCENT);
        int fromC = fromAccent == Accent.MONO ? base : fromAccent.a;
        int toC = accent == Accent.MONO ? base : accent.a;
        return R2D.blend(fromC, toC, accentT());
    }
    public static int accentText() { return luminance(accent()) > 0.6f ? 0xFF0A0A0B : 0xFFFFFFFF; }
    public static int toggleOff() { return c(TOGGLE_OFF); }
    public static int hudBg() { return c(HUD_BG); }
    public static int shadow() { return c(SHADOW); }
}
