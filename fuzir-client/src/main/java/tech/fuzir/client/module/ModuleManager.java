package tech.fuzir.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import tech.fuzir.client.module.impl.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Module, Boolean> HELD = new HashMap<>();

    private ModuleManager() {
    }

    public static void init() {
        add(new Sprint());
        add(new AutoWalk());
        add(new Fullbright());
        add(new Zoom());
        add(new NoWeather());
        add(new Watermark());
        add(new ModuleList());
        add(new Coordinates());
        add(new Info());
        add(new Keystrokes());
        add(new NotificationsModule());
        add(new ClickGui());
    }

    private static void add(Module m) {
        MODULES.add(m);
    }

    public static List<Module> all() {
        return MODULES;
    }

    public static List<Module> in(Category c) {
        return MODULES.stream().filter(m -> m.category == c).toList();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return (T) m;
        throw new IllegalArgumentException(type.getName());
    }

    public static void tick(MinecraftClient mc) {
        // klawisze modułów działają tylko w grze, bez otwartego okna
        if (mc.currentScreen == null && mc.getWindow() != null) {
            long handle = mc.getWindow().getHandle();
            for (Module m : MODULES) {
                if (!m.bind.isSet() || !m.hasToggle()) continue;
                boolean down = InputUtil.isKeyPressed(handle, m.bind.get());
                if (down && !HELD.getOrDefault(m, false)) m.toggle();
                HELD.put(m, down);
            }
        } else {
            HELD.clear();
        }
        if (mc.player == null) return;
        for (Module m : MODULES) if (m.isEnabled()) m.onTick();
    }
}
