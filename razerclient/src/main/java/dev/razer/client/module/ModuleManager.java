package dev.razer.client.module;

import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.ModeSetting;
import dev.razer.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/** Lista modułów – same nazwy, ikony i ustawienia (bez funkcji). */
public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // COMBAT
        reg(new Module("KillAura", Category.COMBAT, "target", "Atakuje graczy w zasięgu", "Attacks players in range")
                .add(new ModeSetting("Tryb", "Mode", "Single", "Switch", "Multi"),
                     new NumberSetting("Zasięg", "Range", 3.5, 3.0, 6.0, 0.1),
                     new BoolSetting("Rotacje", "Rotations", true),
                     new BoolSetting("Tylko gracze", "Players only", false)));
        reg(new Module("Velocity", Category.COMBAT, "shield", "Zmniejsza odrzut", "Reduces knockback")
                .add(new NumberSetting("Poziomo", "Horizontal", 0, 0, 100, 1), new NumberSetting("Pionowo", "Vertical", 0, 0, 100, 1)));
        reg(new Module("Criticals", Category.COMBAT, "bolt", "Uderzenia krytyczne", "Critical hits")
                .add(new ModeSetting("Tryb", "Mode", "Packet", "Jump")));
        reg(new Module("AutoTotem", Category.COMBAT, "heart", "Totem w drugiej ręce", "Totem in offhand")
                .add(new NumberSetting("Opóźnienie", "Delay", 0, 0, 10, 1)));
        reg(new Module("TriggerBot", Category.COMBAT, "click", "Atak po najechaniu", "Attack on crosshair")
                .add(new NumberSetting("CPS", "CPS", 10, 1, 20, 1)));
        // MOVEMENT
        reg(new Module("Sprint", Category.MOVEMENT, "footprints", "Automatyczny sprint", "Automatic sprint")
                .add(new BoolSetting("Wielokierunkowy", "Omni", false)));
        reg(new Module("Speed", Category.MOVEMENT, "gauge", "Szybsze poruszanie", "Move faster")
                .add(new ModeSetting("Tryb", "Mode", "Strafe", "Vanilla", "Ground"), new NumberSetting("Szybkość", "Speed", 1.2, 1.0, 3.0, 0.1)));
        reg(new Module("Flight", Category.MOVEMENT, "plane", "Latanie", "Fly")
                .add(new NumberSetting("Szybkość", "Speed", 1.0, 0.1, 5.0, 0.1)));
        reg(new Module("NoFall", Category.MOVEMENT, "nofall", "Brak obrażeń od upadku", "No fall damage"));
        reg(new Module("Step", Category.MOVEMENT, "step", "Wchodzenie na bloki", "Step up blocks")
                .add(new NumberSetting("Wysokość", "Height", 1.0, 0.5, 2.5, 0.5)));
        // RENDER
        reg(new Module("ESP", Category.RENDER, "scan", "Obrysy graczy", "Player outlines")
                .add(new ModeSetting("Tryb", "Mode", "Box", "Outline", "Glow"), new BoolSetting("Gracze", "Players", true), new BoolSetting("Moby", "Mobs", false)));
        reg(new Module("Fullbright", Category.RENDER, "sun", "Pełna jasność", "Full brightness"));
        reg(new Module("Tracers", Category.RENDER, "radar", "Linie do graczy", "Lines to players"));
        reg(new Module("Nametags", Category.RENDER, "tag", "Większe nicki", "Bigger nametags")
                .add(new NumberSetting("Skala", "Scale", 1.5, 1.0, 3.0, 0.1), new BoolSetting("Zdrowie", "Health", true)));
        reg(new Module("ChestESP", Category.RENDER, "package", "Podświetlanie skrzyń", "Highlights chests"));
        // PLAYER
        reg(new Module("AutoArmor", Category.PLAYER, "shirt", "Zakłada najlepszą zbroję", "Equips best armor"));
        reg(new Module("FastPlace", Category.PLAYER, "blocks", "Szybkie stawianie", "Place faster"));
        reg(new Module("NoSlow", Category.PLAYER, "snail", "Brak spowolnienia", "No slowdown"));
        reg(new Module("InvManager", Category.PLAYER, "backpack", "Porządkuje ekwipunek", "Sorts inventory")
                .add(new NumberSetting("Opóźnienie", "Delay", 2, 0, 10, 1)));
        // WORLD
        reg(new Module("Nuker", Category.WORLD, "bomb", "Niszczy bloki wokół", "Breaks blocks around")
                .add(new NumberSetting("Zasięg", "Range", 4, 1, 6, 1)));
        reg(new Module("Scaffold", Category.WORLD, "construction", "Stawia bloki pod nogami", "Places blocks below")
                .add(new BoolSetting("Wieża", "Tower", true)));
        reg(new Module("FastBreak", Category.WORLD, "pickaxe", "Szybsze kopanie", "Break faster"));
        reg(new Module("Timer", Category.WORLD, "timer", "Zmienia prędkość gry", "Changes game speed")
                .add(new NumberSetting("Prędkość", "Speed", 1.0, 0.1, 5.0, 0.1)));
        // MISC
        reg(new Module("AntiAFK", Category.MISC, "coffee", "Zapobiega wyrzuceniu za AFK", "Prevents AFK kick"));
        reg(new Module("AutoReconnect", Category.MISC, "refresh", "Automatyczne łączenie", "Auto reconnect")
                .add(new NumberSetting("Opóźnienie", "Delay", 5, 1, 30, 1)));
        reg(new Module("MiddleClick", Category.MISC, "mouse", "Znajomi pod kółkiem myszy", "Friends on middle click"));
        reg(new Module("DiscordRPC", Category.MISC, "message", "Status w Discordzie", "Discord rich presence"));
    }

    private void reg(Module m) { modules.add(m); }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) out.add(m);
        return out;
    }

    public Module get(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public long enabledCount() { return modules.stream().filter(Module::isEnabled).count(); }
}
