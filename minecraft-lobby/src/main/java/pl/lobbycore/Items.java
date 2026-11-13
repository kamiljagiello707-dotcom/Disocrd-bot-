package pl.lobbycore;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

final class Items {
    private Items() {}

    static ItemStack of(Material m, String name, String... lore) {
        return of(m, name, Arrays.asList(lore));
    }

    static ItemStack of(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Txt.c(name));
        meta.lore(lore.stream().map(Txt::c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(meta);
        return it;
    }

    static ItemStack glow(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    static ItemStack head(OfflinePlayer owner, String name, String... lore) {
        ItemStack it = of(Material.PLAYER_HEAD, name, lore);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        meta.setOwningPlayer(owner);
        it.setItemMeta(meta);
        return it;
    }

    static ItemStack tag(ItemStack it, NamespacedKey key, String value) {
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        it.setItemMeta(meta);
        return it;
    }

    static String tagOf(ItemStack it, NamespacedKey key) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /** "GOLDEN_APPLE:3" -> ItemStack (niezniszczalny, do kitów). */
    static ItemStack parse(String spec) {
        if (spec == null || spec.isBlank()) return null;
        String[] p = spec.split(":");
        Material m = Material.matchMaterial(p[0].trim());
        if (m == null) return null;
        int amount = p.length > 1 ? Integer.parseInt(p[1].trim()) : 1;
        ItemStack it = new ItemStack(m, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null && m.getMaxDurability() > 0) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            it.setItemMeta(meta);
        }
        return it;
    }
}
