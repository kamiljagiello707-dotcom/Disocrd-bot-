package pl.npcmenu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/** Menu otwierane po kliknięciu NPC: Nazwa + Portfel. */
public final class ProfileMenu implements InventoryHolder {

    static final int SLOT_NAME = 11;
    static final int SLOT_WALLET = 15;
    static final int SLOT_CLOSE = 22;

    private final Inventory inventory;

    public ProfileMenu(NpcMenuPlugin plugin, Player player) {
        var cfg = plugin.getConfig();
        this.inventory = Bukkit.createInventory(this, 27,
                NpcMenuPlugin.color(cfg.getString("menu.tytul", "&8Profil")));

        String balance = plugin.wallet().format(plugin.wallet().get(player.getUniqueId()));
        String currency = cfg.getString("waluta", "zł");

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), null, null, null);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        // Nazwa – głowa gracza
        ItemStack head = item(Material.PLAYER_HEAD,
                cfg.getString("menu.nazwa.tytul", "&eNazwa"),
                cfg.getStringList("menu.nazwa.opis"), player.getName(), balance, currency);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(player);
        head.setItemMeta(skull);
        inventory.setItem(SLOT_NAME, head);

        // Portfel – złoto
        inventory.setItem(SLOT_WALLET, item(Material.GOLD_INGOT,
                cfg.getString("menu.portfel.tytul", "&6Portfel"),
                cfg.getStringList("menu.portfel.opis"), player.getName(), balance, currency));

        inventory.setItem(SLOT_CLOSE, item(Material.BARRIER,
                cfg.getString("menu.zamknij", "&cZamknij"), List.of(), null, null, null));
    }

    private static ItemStack item(Material material, String name, List<String> lore,
                                  String player, String balance, String currency) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(NpcMenuPlugin.color(apply(name, player, balance, currency)));
        List<Component> lines = lore.stream()
                .map(l -> NpcMenuPlugin.color(apply(l, player, balance, currency)))
                .toList();
        meta.lore(lines);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String apply(String text, String player, String balance, String currency) {
        if (player != null) text = text.replace("{gracz}", player);
        if (balance != null) text = text.replace("{saldo}", balance);
        if (currency != null) text = text.replace("{waluta}", currency);
        return text;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
