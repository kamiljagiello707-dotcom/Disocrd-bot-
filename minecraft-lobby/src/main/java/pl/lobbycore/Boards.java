package pl.lobbycore;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sidebar bez migania (linie przez prefiksy drużyn) + nagłówek/stopka tablisty. */
final class Boards {
    private static final String[] ENTRIES = new String[15];
    static {
        String codes = "0123456789abcde";
        for (int i = 0; i < 15; i++) ENTRIES[i] = "§" + codes.charAt(i) + "§r";
    }

    private final LobbyCore plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    Boards(LobbyCore plugin) { this.plugin = plugin; }

    void create(Player p) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o = sb.registerNewObjective("lobbycore", Criteria.DUMMY, Txt.c(""));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        o.numberFormat(NumberFormat.blank());
        for (int i = 0; i < 15; i++) sb.registerNewTeam("l" + i).addEntry(ENTRIES[i]);
        boards.put(p.getUniqueId(), sb);
        p.setScoreboard(sb);
        update(p);
    }

    void remove(Player p) { boards.remove(p.getUniqueId()); }

    void update(Player p) {
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null) return;
        var c = plugin.getConfig();
        Objective o = sb.getObjective("lobbycore");
        o.displayName(Txt.c(plugin.ph(c.getString("scoreboard.tytul"), p)));

        String key = switch (plugin.modes.of(p)) {
            case FFA -> "ffa";
            case ANARCHIA -> "anarchia";
            default -> "lobby";
        };
        List<String> lines = c.getStringList("scoreboard." + key);
        int n = Math.min(15, lines.size());
        for (int i = 0; i < 15; i++) {
            if (i < n) {
                Team t = sb.getTeam("l" + i);
                t.prefix(Txt.c(plugin.ph(lines.get(i), p)));
                o.getScore(ENTRIES[i]).setScore(n - i);
            } else sb.resetScores(ENTRIES[i]);
        }

        p.sendPlayerListHeaderAndFooter(Txt.c(plugin.ph(c.getString("tab.naglowek"), p)),
                Txt.c(plugin.ph(c.getString("tab.stopka"), p)));
    }

    void updateAll() { for (Player p : Bukkit.getOnlinePlayers()) update(p); }
}
