package tech.fuzir.client.module;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.event.EventBus;
import tech.fuzir.client.event.events.KeyEvent;
import tech.fuzir.client.hud.elements.ArmorHud;
import tech.fuzir.client.hud.elements.Coords;
import tech.fuzir.client.hud.elements.Keystrokes;
import tech.fuzir.client.hud.elements.ModuleList;
import tech.fuzir.client.hud.elements.Watermark;
import tech.fuzir.client.module.combat.KillAura;
import tech.fuzir.client.module.combat.TriggerBot;
import tech.fuzir.client.module.misc.ClickGuiModule;
import tech.fuzir.client.module.movement.Sprint;
import tech.fuzir.client.module.movement.Step;
import tech.fuzir.client.module.player.AutoRespawn;
import tech.fuzir.client.module.player.FastPlace;
import tech.fuzir.client.module.render.FullBright;
import tech.fuzir.client.module.world.Ambience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager(EventBus bus) {
        register(new KillAura());
        register(new TriggerBot());
        register(new Sprint());
        register(new Step());
        register(new FullBright());
        register(new AutoRespawn());
        register(new FastPlace());
        register(new Ambience());
        register(new ClickGuiModule());

        register(new Watermark());
        register(new ModuleList());
        register(new ArmorHud());
        register(new Coords());
        register(new Keystrokes());

        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        bus.subscribe(KeyEvent.class, this::onKey);
    }

    /** Enables modules flagged as on-by-default. Called before the config is applied. */
    public void applyDefaults() {
        for (Module module : modules) {
            if (module.isEnabledByDefault()) module.setEnabled(true);
        }
    }

    private void register(Module module) {
        byClass.put(module.getClass(), module);
        modules.add(module);
    }

    private void onKey(KeyEvent event) {
        if (event.action() != GLFW.GLFW_PRESS || event.key() == GLFW.GLFW_KEY_UNKNOWN) return;
        if (MinecraftClient.getInstance().currentScreen != null) return;
        for (Module module : modules) {
            if (module.getBind().matches(event.key())) module.toggle();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        Module module = byClass.get(type);
        if (module == null) throw new IllegalArgumentException("Module not registered: " + type.getName());
        return (T) module;
    }

    public Optional<Module> byName(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getModules(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }
}
