package dev.nova.client.module;

import dev.nova.client.module.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // Register your modules here.
        add(new KillAura());
        add(new Velocity());
        add(new Criticals());
        add(new Sprint());
        add(new Flight());
        add(new Speed());
        add(new ESP());
        add(new Fullbright());
        add(new HudModule());
        add(new ClickGuiModule());
        add(new NoFall());
        add(new FastPlace());
        add(new AutoTool());
        add(new Timer());
    }

    private void add(Module m) { modules.add(m); }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        return modules.stream().filter(m -> m.getCategory() == c).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return (T) m;
        throw new IllegalArgumentException(type.getName());
    }

    public Optional<Module> byName(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void onTick() {
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }
}
