package pl.npcmenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public final class NpcCommand implements TabExecutor {

    private final NpcMenuPlugin plugin;

    public NpcCommand(NpcMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracz może użyć tej komendy.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(NpcMenuPlugin.color("&e/npc stworz [nazwa] &7- tworzy NPC w twoim miejscu"));
            player.sendMessage(NpcMenuPlugin.color("&e/npc usun &7- usuwa najbliższego NPC (do 5 kratek)"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stworz" -> {
                String name = args.length > 1
                        ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                        : plugin.getConfig().getString("npc.domyslna-nazwa", "&6NPC");

                player.getWorld().spawn(player.getLocation(), Villager.class, v -> {
                    v.customName(NpcMenuPlugin.color(name));
                    v.setCustomNameVisible(true);
                    v.setAI(false);
                    v.setInvulnerable(true);
                    v.setSilent(true);
                    v.setPersistent(true);
                    v.setRemoveWhenFarAway(false);
                    v.setCollidable(false);
                    v.getPersistentDataContainer().set(plugin.npcKey(), PersistentDataType.BYTE, (byte) 1);
                });
                player.sendMessage(NpcMenuPlugin.color("&aStworzono NPC: &r" + name));
            }
            case "usun" -> {
                Entity nearest = null;
                double best = Double.MAX_VALUE;
                for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                    if (!NpcListener.isNpc(plugin, e)) continue;
                    double d = e.getLocation().distanceSquared(player.getLocation());
                    if (d < best) { best = d; nearest = e; }
                }
                if (nearest == null) {
                    player.sendMessage(NpcMenuPlugin.color("&cBrak NPC w pobliżu."));
                } else {
                    nearest.remove();
                    player.sendMessage(NpcMenuPlugin.color("&aUsunięto NPC."));
                }
            }
            default -> player.sendMessage(NpcMenuPlugin.color("&cUżycie: /npc <stworz [nazwa]|usun>"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("stworz", "usun");
        return List.of();
    }
}
