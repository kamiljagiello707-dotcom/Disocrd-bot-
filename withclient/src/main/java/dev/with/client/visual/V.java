package dev.with.client.visual;

import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.module.Setting;

/** Krótki dostęp do ustawień modułów z mixinów i efektów (bez rzucania wyjątków przy braku ustawienia). */
public final class V {
    private V() {}

    public static boolean on(String mod) { Module m = Modules.get(mod); return m != null && m.isEnabled(); }
    public static float num(String mod, String s, float d) { Module m = Modules.get(mod); return m != null && m.setting(s) instanceof Setting.Num n ? n.get().floatValue() : d; }
    public static boolean bo(String mod, String s) { Module m = Modules.get(mod); return m != null && m.setting(s) instanceof Setting.Bool b && b.get(); }
    public static String mode(String mod, String s, String d) { Module m = Modules.get(mod); return m != null && m.setting(s) instanceof Setting.Mode o ? o.get() : d; }
    public static int color(String mod, String s, int d) { Module m = Modules.get(mod); return m != null && m.setting(s) instanceof Setting.Color c ? c.get() : d; }
    public static boolean has(String mod, String s, String opt) { Module m = Modules.get(mod); return m != null && m.setting(s) instanceof Setting.Multi mu && mu.has(opt); }
    public static String text(String mod, String s, String d) { Module m = Modules.get(mod); return m != null && m.setting(s) instanceof Setting.Text t ? t.get() : d; }

    /** włączony moduł + zaznaczona opcja w liście wielokrotnego wyboru */
    public static boolean hide(String mod, String opt) { return on(mod) && has(mod, "Ukryj", opt); }

    /** kolor tęczy (ARGB) */
    public static int rainbow(float speed, float offset) {
        float h = ((System.currentTimeMillis() % 1_000_000L) / 1000f * 0.15f * speed + offset) % 1f;
        if (h < 0) h += 1;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(h, 0.55f, 1f) & 0xFFFFFF);
    }

    /** kolor z ustawień z trybem "Kolor tryb" = Rainbow */
    public static int col(String mod, int d, float offset) {
        return mode(mod, "Kolor tryb", "Static").equals("Rainbow") ? rainbow(1, offset) : color(mod, "Kolor", d);
    }

    public static float r(int c) { return ((c >> 16) & 0xFF) / 255f; }
    public static float g(int c) { return ((c >> 8) & 0xFF) / 255f; }
    public static float b(int c) { return (c & 0xFF) / 255f; }
    public static float a(int c) { return ((c >>> 24) & 0xFF) / 255f; }
}
