package dev.vesca.client.ui;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista znajomych (strona "Znajomi"), zapisywana w config/vescaclient/friends.txt. */
public final class Friends {
    private Friends() {}

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vescaclient").resolve("friends.txt");
    private static List<String> list;

    public static List<String> all() { load(); return list; }

    public static boolean add(String nick) {
        load();
        nick = nick.trim();
        if (nick.isEmpty() || nick.length() > 16 || !nick.matches("[A-Za-z0-9_]+")) return false;
        String n = nick;
        if (list.stream().anyMatch(f -> f.equalsIgnoreCase(n))) return false;
        list.add(nick);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        save();
        return true;
    }

    public static void remove(String nick) { load(); list.removeIf(f -> f.equalsIgnoreCase(nick)); save(); }

    public static boolean is(String nick) { load(); return list.stream().anyMatch(f -> f.toLowerCase(Locale.ROOT).equals(nick.toLowerCase(Locale.ROOT))); }

    private static void load() {
        if (list != null) return;
        list = new ArrayList<>();
        try { if (Files.exists(FILE)) for (String l : Files.readAllLines(FILE, StandardCharsets.UTF_8)) if (!l.isBlank()) list.add(l.trim()); } catch (Exception ignored) {}
    }

    private static void save() {
        try { Files.createDirectories(FILE.getParent()); Files.write(FILE, list, StandardCharsets.UTF_8); } catch (Exception ignored) {}
    }
}
