package dev.vesc.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista znajomych (zapisywana w client.json). */
public class Friends {
    private final List<String> names = new ArrayList<>();

    public List<String> all() { return names; }
    public boolean is(String name) { return names.stream().anyMatch(n -> n.equalsIgnoreCase(name)); }

    public boolean add(String name) {
        name = name.trim();
        if (name.isEmpty() || name.length() > 16 || is(name)) return false;
        names.add(name);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return true;
    }

    public void remove(String name) { names.removeIf(n -> n.equalsIgnoreCase(name)); }

    public static boolean validName(String s) { return s.toLowerCase(Locale.ROOT).matches("[a-z0-9_]{1,16}"); }
}
