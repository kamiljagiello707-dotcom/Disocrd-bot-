package pl.mysticclient.module;

import pl.mysticclient.module.combat.AutoTotem;
import pl.mysticclient.module.combat.TriggerBot;
import pl.mysticclient.module.misc.*;
import pl.mysticclient.module.movement.AutoWalk;
import pl.mysticclient.module.movement.SafeWalk;
import pl.mysticclient.module.movement.Sprint;
import pl.mysticclient.module.player.AutoEat;
import pl.mysticclient.module.player.AutoRespawn;
import pl.mysticclient.module.player.FastPlace;
import pl.mysticclient.module.render.*;
import pl.mysticclient.module.world.NoWeather;
import pl.mysticclient.module.world.TimeChanger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static final Map<Class<?>, Module> byClass = new HashMap<>();

    private ModuleManager() {}

    public static void init() {
        // Combat
        register(new TriggerBot());
        register(new AutoTotem());
        // Movement
        register(new Sprint());
        register(new AutoWalk());
        register(new SafeWalk());
        // Render
        register(new HUD());
        register(new Fullbright());
        register(new ESP());
        register(new Tracers());
        register(new Keystrokes());
        register(new PotionHUD());
        register(new ArmorHUD());
        register(new Coordinates());
        // Player
        register(new AutoRespawn());
        register(new FastPlace());
        register(new AutoEat());
        // World
        register(new NoWeather());
        register(new TimeChanger());
        // Misc
        register(new ClickGUI());
        register(new ClientSettings());
        register(new MiddleClick());
        register(new FakePlayer());
        register(new AntiAFK());

        // domyślnie włączone
        for (Class<? extends Module> c : List.of(HUD.class, ClientSettings.class, Keystrokes.class, PotionHUD.class, ArmorHUD.class, Coordinates.class)) {
            get(c).setEnabled(true, false);
        }
    }

    private static void register(Module m) {
        modules.add(m);
        byClass.put(m.getClass(), m);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> clazz) { return (T) byClass.get(clazz); }

    public static boolean isOn(Class<? extends Module> clazz) {
        Module m = byClass.get(clazz);
        return m != null && m.isEnabled();
    }

    public static List<Module> getModules() { return modules; }

    public static List<Module> getByCategory(Category c) {
        return modules.stream().filter(m -> m.getCategory() == c).toList();
    }

    public static Module getByName(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public static long enabledCount() { return modules.stream().filter(Module::isEnabled).count(); }

    public static void onKey(int key) {
        for (Module m : modules) if (m.getKey() == key && key > 0) m.toggle();
    }
}
