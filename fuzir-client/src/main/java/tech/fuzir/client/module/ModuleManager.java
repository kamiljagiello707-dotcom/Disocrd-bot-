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
        add(new ToggleSneak());
        add(new AutoWalk());
        add(new AutoTool());
        add(new AutoRespawn());
        add(new Fullbright());
        add(new Zoom());
        add(new NoWeather());
        add(new NoHurtCam());
        add(new NoFireOverlay());
        add(new Cape());
        add(new JumpCircles());
        add(new ChinaHat());
        add(new Watermark());
        add(new KeyBinds());
        add(new ModuleList());
        add(new Info());
        add(new Coordinates());
        add(new Keystrokes());
        add(new ArmorHud());
        add(new NotificationsModule());
        add(new ClickGui());
        modules.add(new TargetESP());
modules.add(new ESP());
modules.add(new Tracers());
modules.add(new NameTags());
modules.add(new FullbrightExtras());
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

    public static List<HudModule> hud() {
        return MODULES.stream().filter(m -> m instanceof HudModule).map(m -> (HudModule) m).toList();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return (T) m;
        throw new IllegalArgumentException(type.getName());
    }

    public static void tick(MinecraftClient mc) {
        // klawisze modułów działają tylko w grze, bez otwartego okna
        boolean inGame = mc.currentScreen == null && mc.getWindow() != null;
        for (Module m : MODULES) {
            if (!m.bind.isSet() || !m.hasToggle()) continue;
            boolean down = inGame && InputUtil.isKeyPressed(mc.getWindow().getHandle(), m.bind.get());
            boolean was = HELD.getOrDefault(m, false);
            if (m.holdMode()) {
                if (down != was && (inGame || !down)) m.setEnabled(down);
            } else if (down && !was) {
                m.toggle();
            }
            HELD.put(m, down);
        }
        if (mc.player == null) return;
        for (Module m : MODULES) if (m.isEnabled()) m.onTick();
    }
}
