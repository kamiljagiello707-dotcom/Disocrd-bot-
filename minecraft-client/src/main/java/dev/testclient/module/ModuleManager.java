package dev.testclient.module;

import dev.testclient.module.movement.Flight;
import dev.testclient.module.movement.Sprint;
import dev.testclient.module.player.NoFall;
import dev.testclient.module.render.Fullbright;
import dev.testclient.module.render.HudModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        modules.add(new Sprint());
        modules.add(new Flight());
        modules.add(new Fullbright());
        modules.add(new HudModule());
        modules.add(new NoFall());
    }

    public List<Module> getModules() { return modules; }

    public List<Module> getByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    public Optional<Module> get(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) {
        for (Module m : modules) if (clazz.isInstance(m)) return (T) m;
        return null;
    }


    public void onTick() {
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }
}
