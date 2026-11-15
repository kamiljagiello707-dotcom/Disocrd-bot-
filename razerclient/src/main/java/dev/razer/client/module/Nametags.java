package dev.razer.client.module;

import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.ModeSetting;
import dev.razer.client.setting.NumberSetting;

/** Nametags – nowoczesne, zaokrąglone nicki nad graczami (zamiast vanilla). Z testem głębi – nie przez ściany. */
public class Nametags extends Module {
    public final NumberSetting scale = new NumberSetting("Skala", "Scale", 1.0, 0.5, 2.5, 0.05);
    public final ModeSetting background = new ModeSetting("Tło", "Background", "Dark", "Glass", "None");
    public final BoolSetting head = new BoolSetting("Głowa", "Head", true);
    public final BoolSetting health = new BoolSetting("Zdrowie", "Health", true);
    public final BoolSetting healthBar = new BoolSetting("Pasek zdrowia", "Health Bar", true);
    public final BoolSetting armor = new BoolSetting("Pancerz", "Armor", true);
    public final BoolSetting ping = new BoolSetting("Ping", "Ping", true);
    public final BoolSetting distance = new BoolSetting("Dystans", "Distance", false);
    public final BoolSetting self = new BoolSetting("Własny (F5)", "Self (F5)", false);

    public Nametags() {
        super("Nametags", Category.RENDER, "tag", "Nowoczesne nicki nad graczami", "Modern nametags above players");
        add(scale, background, head, health, healthBar, armor, ping, distance, self);
    }
}
