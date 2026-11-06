package pl.firen.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.module.modules.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Set<Integer> heldKeys = new HashSet<>();

    public ModuleManager() {
        // Combat
        modules.add(new KillAura());
        modules.add(new Velocity());
        modules.add(new AutoClicker());
        modules.add(new Criticals());
        // Movement
        modules.add(new Sprint());
        modules.add(new Flight());
        modules.add(new Speed());
        modules.add(new NoFall());
        // Render
        modules.add(new Hud());
        modules.add(new ClickGui());
        modules.add(new Esp());
        modules.add(new Fullbright());
        modules.add(new Tracers());
        // Player
        modules.add(new AutoTool());
        modules.add(new FastPlace());
        modules.add(new ChestStealer());
        // World
        modules.add(new Scaffold());
        modules.add(new Timer());
        modules.add(new XRay());
    }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) modules.stream().filter(type::isInstance).findFirst().orElseThrow();
    }

    public Optional<Module> byName(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void preTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        for (Module m : modules) {
            if (m.isEnabled()) m.onPreTick();
        }
    }

    public void renderWorld(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (MinecraftClient.getInstance().player == null) return;
        for (Module m : modules) m.onRenderWorld(context);
    }

    public void tick(MinecraftClient client) {
        handleKeybinds(client);
        if (client.player == null) return;
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick();
        }
    }

    private void handleKeybinds(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        for (Module m : modules) {
            int key = m.getKey();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            boolean down = InputUtil.isKeyPressed(handle, key);
            if (down && heldKeys.add(key)) {
                if (client.currentScreen == null) m.toggle();
            } else if (!down) {
                heldKeys.remove(key);
            }
        }
    }
}
