package pl.lobbycore;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Proste menu GUI: slot -> akcja. Nic nie da się wyjąć. */
final class Menu implements InventoryHolder {
    private final Inventory inv;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

    Menu(int rows, Component title) {
        inv = Bukkit.createInventory(this, rows * 9, title);
        ItemStack pane = Items.of(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack accent = Items.of(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            int r = i / 9, c = i % 9;
            boolean border = r == 0 || r == rows - 1 || c == 0 || c == 8;
            inv.setItem(i, border ? pane : accent);
        }
    }

    Menu set(int slot, ItemStack item, Consumer<Player> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
        return this;
    }

    @Override public Inventory getInventory() { return inv; }

    static final class Listen implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof Menu menu)) return;
            e.setCancelled(true);
            if (e.getRawSlot() < 0 || e.getRawSlot() >= menu.inv.getSize()) return;
            Consumer<Player> a = menu.actions.get(e.getRawSlot());
            if (a != null && e.getWhoClicked() instanceof Player p) a.accept(p);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
        }
    }
}
