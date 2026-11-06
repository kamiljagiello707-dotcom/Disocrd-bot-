package pl.mystic.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pl.mystic.client.module.modules.*;

import java.util.*;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Set<Integer> heldKeys = new HashSet<>();

    public ModuleManager() {
        add(new Fly());
        add(new Speed());
        add(new AutoSprint());
        add(new Step());
        add(new NoFall());
        add(new Fullbright());
        add(new Esp());
        add(new Zoom());
        add(new AutoTotem());
        add(new FastPlace());
        modules.sort(Comparator.comparing(Module::getName));
    }

    private void add(Module m) { modules.add(m); }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        return modules.stream().filter(m -> m.getCategory() == c).toList();
    }

    public Module get(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) modules.stream().filter(type::isInstance).findFirst().orElse(null);
    }

    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        if (!pl.mystic.client.config.ConfigManager.pending.isEmpty()) {
            pl.mystic.client.config.ConfigManager.pending.forEach(m -> m.setEnabled(true));
            pl.mystic.client.config.ConfigManager.pending.clear();
        }

        // Skróty klawiszowe modułów (tylko gdy nie jest otwarty żaden ekran).
        long window = client.getWindow().getHandle();
        for (Module m : modules) {
            int key = m.getKey();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            boolean down = client.currentScreen == null && InputUtil.isKeyPressed(window, key);
            if (down && heldKeys.add(key)) m.toggle();
            else if (!down) heldKeys.remove(key);
        }

        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }
}
