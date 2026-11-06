package pl.firen.client.util;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Lista znajomych – KillAura, AutoTrap i inne moduły ich nie atakują. */
public final class Friends {
    private Friends() {}

    private static final Set<String> NAMES = new TreeSet<>();

    public static boolean is(String name) { return name != null && NAMES.contains(name.toLowerCase(Locale.ROOT)); }
    public static boolean toggle(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (!NAMES.remove(n)) { NAMES.add(n); return true; }
        return false;
    }
    public static Set<String> all() { return NAMES; }
    public static void set(Iterable<String> names) {
        NAMES.clear();
        for (String n : names) NAMES.add(n.toLowerCase(Locale.ROOT));
    }
}
