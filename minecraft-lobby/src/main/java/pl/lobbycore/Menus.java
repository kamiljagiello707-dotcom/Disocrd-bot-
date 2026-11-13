package pl.lobbycore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

final class Menus {
    private Menus() {}

    static void selector(LobbyCore pl, Player p) {
        var id = p.getUniqueId();
        Menu m = new Menu(3, Txt.c("&8» &lWybierz tryb gry"));
        m.set(11, Items.of(Material.TNT, "&c&lANARCHIA",
                "&7Survival bez żadnych zasad.",
                "&7Griefuj, walcz, buduj, przetrwaj.",
                "&7Twój ekwipunek zostaje na Anarchii.",
                "",
                "&8» &7Online: &a" + pl.modes.online(Mode.ANARCHIA),
                "",
                "&a▶ Kliknij, aby dołączyć"), pp -> go(pl, pp, Mode.ANARCHIA));
        m.set(13, Items.glow(Items.of(Material.NETHER_STAR, "&e&lLOBBY",
                "&7Wróć na środek lobby.",
                "",
                "&8» &7Online w lobby: &a" + pl.modes.online(Mode.LOBBY),
                "",
                "&a▶ Kliknij, aby się przenieść")), pp -> go(pl, pp, Mode.LOBBY));
        m.set(15, Items.glow(Items.of(Material.DIAMOND_SWORD, "&b&lFFA",
                "&7Każdy na każdego na arenie.",
                "&7Dostajesz gotowy zestaw.",
                "",
                "&8» &7Online: &a" + pl.modes.online(Mode.FFA),
                "&8» &7Twoje zabójstwa: &f" + pl.stats.kills(id),
                "",
                "&a▶ Kliknij, aby dołączyć")), pp -> go(pl, pp, Mode.FFA));
        p.openInventory(m.getInventory());
        p.playSound(p, Sound.BLOCK_CHEST_OPEN, 0.5f, 1.4f);
    }

    private static void go(LobbyCore pl, Player p, Mode mode) {
        p.closeInventory();
        p.playSound(p, Sound.UI_BUTTON_CLICK, 1, 1);
        pl.modes.send(p, mode);
    }

    static void profile(LobbyCore pl, Player p) {
        var id = p.getUniqueId();
        Menu m = new Menu(3, Txt.c("&8» &lTwój profil"));
        String first = new SimpleDateFormat("dd.MM.yyyy").format(new Date(p.getFirstPlayed()));
        m.set(10, Items.head(p, "&e&lNazwa", "&7Gracz: &f" + p.getName(), "&7Pierwsze wejście: &f" + first), null);

        m.set(12, Items.of(Material.DIAMOND_SWORD, "&b&lStatystyki FFA",
                "&7Zabójstwa: &a" + pl.stats.kills(id),
                "&7Śmierci: &c" + pl.stats.deaths(id),
                "&7K/D: &e" + pl.stats.kd(id),
                "&7Najlepsza seria: &6" + pl.stats.best(id)), null);

        long minutes = p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
        m.set(14, Items.of(Material.CLOCK, "&a&lCzas gry",
                "&7Łącznie: &f" + (minutes / 60) + "h " + (minutes % 60) + "min"), null);

        String wallet = wallet(p);
        m.set(16, Items.of(Material.GOLD_INGOT, "&6&lPortfel",
                wallet == null ? "&7Brak pluginu NpcMenu." : "&7Saldo: &a" + wallet), null);

        m.set(22, Items.of(Material.BARRIER, "&c&lZamknij"), Player::closeInventory);
        p.openInventory(m.getInventory());
        p.playSound(p, Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
    }

    /** Saldo z pluginu NpcMenu (plugins/NpcMenu/portfel.yml), jeśli jest zainstalowany. */
    private static String wallet(Player p) {
        var npcMenu = Bukkit.getPluginManager().getPlugin("NpcMenu");
        if (npcMenu == null) return null;
        File f = new File(npcMenu.getDataFolder(), "portfel.yml");
        double start = npcMenu.getConfig().getDouble("saldo-startowe", 0);
        double v = f.exists() ? YamlConfiguration.loadConfiguration(f).getDouble(p.getUniqueId().toString(), start) : start;
        String s = v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.2f", v);
        return s + " " + npcMenu.getConfig().getString("waluta", "zł");
    }
}
