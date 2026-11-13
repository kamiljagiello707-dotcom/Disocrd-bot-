package pl.npcmenu;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public final class NpcListener implements Listener {

    private final NpcMenuPlugin plugin;

    public NpcListener(NpcMenuPlugin plugin) {
        this.plugin = plugin;
    }

    static boolean isNpc(NpcMenuPlugin plugin, Entity entity) {
        return entity.getPersistentDataContainer().has(plugin.npcKey(), PersistentDataType.BYTE);
    }

    @EventHandler
    public void onClickNpc(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isNpc(plugin, event.getRightClicked())) return;
        event.setCancelled(true); // blokuje okno handlu wieśniaka
        Player player = event.getPlayer();
        player.openInventory(new ProfileMenu(plugin, player).getInventory());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (isNpc(plugin, event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ProfileMenu)) return;
        event.setCancelled(true);
        if (event.getRawSlot() == ProfileMenu.SLOT_CLOSE
                && event.getCurrentItem() != null
                && event.getCurrentItem().getType() == Material.BARRIER) {
            event.getWhoClicked().closeInventory();
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ProfileMenu) event.setCancelled(true);
    }
}
