package pl.npcmenu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcMenuPlugin extends JavaPlugin {

    private NamespacedKey npcKey;
    private Wallet wallet;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        npcKey = new NamespacedKey(this, "npc");
        wallet = new Wallet(this);

        NpcCommand npcCommand = new NpcCommand(this);
        getCommand("npc").setExecutor(npcCommand);
        getCommand("npc").setTabCompleter(npcCommand);

        WalletCommand walletCommand = new WalletCommand(this);
        getCommand("portfel").setExecutor(walletCommand);
        getCommand("portfel").setTabCompleter(walletCommand);

        getServer().getPluginManager().registerEvents(new NpcListener(this), this);
    }

    @Override
    public void onDisable() {
        if (wallet != null) wallet.save();
    }

    public NamespacedKey npcKey() { return npcKey; }
    public Wallet wallet() { return wallet; }

    /** Zamienia tekst z kodami &x na komponent (bez domyślnej kursywy w itemach). */
    public static Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text)
                .decoration(TextDecoration.ITALIC, false);
    }
}
