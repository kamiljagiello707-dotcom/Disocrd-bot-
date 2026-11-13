package pl.npcmenu;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public final class WalletCommand implements TabExecutor {

    private final NpcMenuPlugin plugin;

    public WalletCommand(NpcMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String currency = plugin.getConfig().getString("waluta", "zł");
        Wallet wallet = plugin.wallet();

        // /portfel  lub  /portfel <gracz>
        if (args.length <= 1 && (args.length == 0 || !isAction(args[0]))) {
            OfflinePlayer target;
            if (args.length == 0) {
                if (!(sender instanceof Player p)) { sender.sendMessage("Użycie: /portfel <gracz>"); return true; }
                target = p;
            } else {
                target = Bukkit.getOfflinePlayer(args[0]);
            }
            sender.sendMessage(NpcMenuPlugin.color("&6Portfel &f" + target.getName() + "&7: &a"
                    + wallet.format(wallet.get(target.getUniqueId())) + " " + currency));
            return true;
        }

        // /portfel <dodaj|zabierz|ustaw> <gracz> <kwota>
        if (!sender.hasPermission("npcmenu.admin")) {
            sender.sendMessage(NpcMenuPlugin.color("&cBrak uprawnień."));
            return true;
        }
        if (args.length != 3) {
            sender.sendMessage(NpcMenuPlugin.color("&cUżycie: /portfel <dodaj|zabierz|ustaw> <gracz> <kwota>"));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2].replace(',', '.'));
        } catch (NumberFormatException e) {
            sender.sendMessage(NpcMenuPlugin.color("&cNieprawidłowa kwota."));
            return true;
        }
        if (amount < 0) {
            sender.sendMessage(NpcMenuPlugin.color("&cKwota nie może być ujemna."));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        switch (args[0].toLowerCase()) {
            case "dodaj" -> wallet.add(target.getUniqueId(), amount);
            case "zabierz" -> wallet.add(target.getUniqueId(), -amount);
            case "ustaw" -> wallet.set(target.getUniqueId(), amount);
        }
        sender.sendMessage(NpcMenuPlugin.color("&aNowe saldo &f" + target.getName() + "&a: "
                + wallet.format(wallet.get(target.getUniqueId())) + " " + currency));
        return true;
    }

    private static boolean isAction(String s) {
        return switch (s.toLowerCase()) { case "dodaj", "zabierz", "ustaw" -> true; default -> false; };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new java.util.ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            if (sender.hasPermission("npcmenu.admin")) out.addAll(List.of("dodaj", "zabierz", "ustaw"));
            return out;
        }
        if (args.length == 2 && isAction(args[0])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
