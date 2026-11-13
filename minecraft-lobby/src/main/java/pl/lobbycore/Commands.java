package pl.lobbycore;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class Commands implements TabExecutor {
    private final LobbyCore plugin;

    Commands(LobbyCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();
        if (name.equals("lobbyadmin")) return admin(sender, args);
        if (!(sender instanceof Player p)) { sender.sendMessage("Tylko dla graczy."); return true; }
        switch (name) {
            case "lobby" -> plugin.modes.send(p, Mode.LOBBY);
            case "anarchia" -> plugin.modes.send(p, Mode.ANARCHIA);
            case "ffa" -> plugin.modes.send(p, Mode.FFA);
        }
        return true;
    }

    private boolean admin(CommandSender s, String[] a) {
        if (a.length == 0) {
            s.sendMessage(Txt.c("&6&lLobbyCore &7– komendy:"));
            s.sendMessage(Txt.c("&e/la zbuduj lobby &7– (prze)buduj lobby"));
            s.sendMessage(Txt.c("&e/la zbuduj ffa &7– (prze)buduj arenę FFA"));
            s.sendMessage(Txt.c("&e/la ustawspawn &7– spawn lobby w twoim miejscu"));
            s.sendMessage(Txt.c("&e/la dodajspawnffa &7| &e/la czyscspawnyffa"));
            s.sendMessage(Txt.c("&e/la buduj &7– tryb budowania (bez ochrony)"));
            s.sendMessage(Txt.c("&e/la reload &7– przeładuj config"));
            return true;
        }
        Player p = s instanceof Player pl ? pl : null;
        switch (a[0].toLowerCase()) {
            case "zbuduj" -> {
                if (a.length < 2) { s.sendMessage(Txt.c("&cUżycie: /la zbuduj <lobby|ffa>")); return true; }
                long t = System.currentTimeMillis();
                if (a[1].equalsIgnoreCase("lobby")) plugin.buildLobby();
                else if (a[1].equalsIgnoreCase("ffa")) plugin.buildArena();
                else { s.sendMessage(Txt.c("&cUżycie: /la zbuduj <lobby|ffa>")); return true; }
                s.sendMessage(Txt.c("&aZbudowano &f" + a[1] + " &7(" + (System.currentTimeMillis() - t) + " ms)"));
            }
            case "ustawspawn" -> {
                if (p == null || plugin.modes.of(p) != Mode.LOBBY) { s.sendMessage(Txt.c("&cStań w świecie lobby.")); return true; }
                plugin.data.setLobbySpawn(p.getLocation());
                s.sendMessage(Txt.c("&aUstawiono spawn lobby."));
            }
            case "dodajspawnffa" -> {
                if (p == null || plugin.modes.of(p) != Mode.FFA) { s.sendMessage(Txt.c("&cStań na arenie FFA.")); return true; }
                List<Location> list = new ArrayList<>(plugin.data.ffaSpawns());
                list.add(p.getLocation());
                plugin.data.setFfaSpawns(list);
                s.sendMessage(Txt.c("&aDodano spawn FFA &7(" + list.size() + ")"));
            }
            case "czyscspawnyffa" -> { plugin.data.setFfaSpawns(List.of()); s.sendMessage(Txt.c("&aWyczyszczono spawny FFA.")); }
            case "buduj" -> {
                if (p == null) return true;
                if (plugin.builders.remove(p.getUniqueId())) {
                    s.sendMessage(Txt.c("&7Tryb budowania: &cwyłączony"));
                    if (plugin.modes.of(p) == Mode.LOBBY) plugin.lobby.setup(p, false);
                    else if (plugin.modes.of(p) == Mode.FFA) plugin.ffa.prepare(p, false);
                } else {
                    plugin.builders.add(p.getUniqueId());
                    p.setGameMode(GameMode.CREATIVE);
                    s.sendMessage(Txt.c("&7Tryb budowania: &awłączony &8(ochrona lobby/FFA wyłączona dla ciebie)"));
                }
            }
            case "reload" -> { plugin.reloadConfig(); plugin.boards.updateAll(); s.sendMessage(Txt.c("&aPrzeładowano config.")); }
            default -> s.sendMessage(Txt.c("&cNieznana komenda. Wpisz /la"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (!cmd.getName().equalsIgnoreCase("lobbyadmin")) return List.of();
        if (a.length == 1) return filter(List.of("zbuduj", "ustawspawn", "dodajspawnffa", "czyscspawnyffa", "buduj", "reload"), a[0]);
        if (a.length == 2 && a[0].equalsIgnoreCase("zbuduj")) return filter(List.of("lobby", "ffa"), a[1]);
        return List.of();
    }

    private static List<String> filter(List<String> opts, String start) {
        return opts.stream().filter(o -> o.startsWith(start.toLowerCase())).toList();
    }
}
