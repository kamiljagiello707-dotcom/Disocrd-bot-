package pl.mysticclient.util;

/** Monochromatyczny motyw: czerń i biel. */
public final class Theme {
    /** false = ciemny (domyślny), true = jasny */
    public static boolean light = false;

    private Theme() {}

    private static int pick(int dark, int lightC) { return light ? lightC : dark; }

    public static int bg()        { return pick(0xFF09090B, 0xFFFFFFFF); }
    public static int surface()   { return pick(0xFF0F0F11, 0xFFFAFAFA); }
    public static int surface2()  { return pick(0xFF161618, 0xFFF4F4F5); }
    public static int surface3()  { return pick(0xFF1F1F23, 0xFFE9E9EC); }
    public static int border()    { return pick(0xFF1F1F23, 0xFFE4E4E7); }
    public static int borderHi()  { return pick(0xFF3A3A40, 0xFFC4C4CA); }
    public static int text()      { return pick(0xFFFAFAFA, 0xFF09090B); }
    public static int sub()       { return pick(0xFFA1A1AA, 0xFF52525B); }
    public static int dim()       { return pick(0xFF5C5C66, 0xFFA1A1AA); }
    /** kolor akcentu = biel (ciemny) / czerń (jasny) */
    public static int accent()    { return pick(0xFFFFFFFF, 0xFF09090B); }
    /** tekst na tle akcentu */
    public static int onAccent()  { return pick(0xFF09090B, 0xFFFFFFFF); }
    public static int shadow()    { return pick(0x99000000, 0x26000000); }

    // HUD – zawsze "szkło" dopasowane do motywu
    public static int glass()       { return pick(0xD909090B, 0xE6FFFFFF); }
    public static int glassBorder() { return pick(0x1FFFFFFF, 0x1A000000); }
}
