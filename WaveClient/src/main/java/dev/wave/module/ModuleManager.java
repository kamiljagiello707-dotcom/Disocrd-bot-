package dev.wave.module;

import dev.wave.hud.elements.*;
import dev.wave.module.client.ClickGUI;
import dev.wave.module.client.HudEditor;
import dev.wave.module.client.Notifications;
import dev.wave.module.movement.AutoWalk;
import dev.wave.module.movement.Eagle;
import dev.wave.module.movement.Parkour;
import dev.wave.module.movement.Sprint;
import dev.wave.module.player.AntiAFK;
import dev.wave.module.player.AutoRespawn;
import dev.wave.module.visuals.*;
import dev.wave.util.KeyUtil;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> byClass = new HashMap<>();

    public ModuleManager() {
        // Visuals
        add(new Fullbright());
        add(new ESP());
        add(new Tracers());
        add(new StorageESP());
        add(new Zoom());
        add(new Crosshair());
        add(new NoHurtCam());
        add(new NoOverlay());
        add(new Ambience());
        // Movement
        add(new Sprint());
        add(new AutoWalk());
        add(new Parkour());
        add(new Eagle());
        // Player
        add(new AutoRespawn());
        add(new AntiAFK());
        // HUD
        add(new Watermark());
        add(new ArrayListHud());
        add(new InfoHud());
        add(new CoordinatesHud());
        add(new ArmorHud());
        add(new PotionHud());
        add(new KeystrokesHud());
        add(new InventoryHud());
        // Client
        add(new ClickGUI());
        add(new Notifications());
        add(new HudEditor());
    }

    private void add(Module m) {
        modules.add(m);
        byClass.put(m.getClass(), m);
    }

    public List<Module> getModules() { return modules; }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) { return (T) byClass.get(clazz); }

    public Module getByName(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public List<Module> getByCategory(Category c) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) list.add(m);
        return list;
    }

    public void onTick() {
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }

    /** Handles a key or mouse-button bind event. */
    public void onBind(int key, int action) {
        if (key == KeyUtil.NONE) return;
        boolean inGame = MinecraftClient.getInstance().currentScreen == null;
        for (Module m : modules) {
            if (m.getKey() != key) continue;
            if (m.bindMode.is("Hold") && !(m instanceof ClickGUI) && !(m instanceof HudEditor)) {
                if (action == GLFW.GLFW_PRESS && inGame) m.setEnabled(true);
                else if (action == GLFW.GLFW_RELEASE) m.setEnabled(false);
            } else if (action == GLFW.GLFW_PRESS && inGame) {
                m.toggle();
            }
        }
    }
}
