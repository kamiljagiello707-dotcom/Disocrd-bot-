package pl.mysticclient.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class FriendManager {
    private static final Set<String> friends = new LinkedHashSet<>();

    private FriendManager() {}

    public static boolean isFriend(String name) { return friends.contains(name.toLowerCase(Locale.ROOT)); }

    /** @return true jeśli dodano, false jeśli usunięto */
    public static boolean toggle(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (friends.remove(key)) return false;
        friends.add(key);
        return true;
    }

    public static Collection<String> all() { return friends; }
    public static int count() { return friends.size(); }

    public static void set(Collection<String> names) {
        friends.clear();
        names.forEach(n -> friends.add(n.toLowerCase(Locale.ROOT)));
    }
}
