package dev.mustic.client.module;

import dev.mustic.client.module.modules.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        // CLIENT
        register(new ClickGuiModule());
        register(new HudModule());
        // MOVEMENT
        register(new SprintModule());
        // RENDER
        register(new FullbrightModule());
        // PLAYER
        register(new AutoRespawnModule());
    }

    private static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> getByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : MODULES) if (m.getCategory() == category) list.add(m);
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> clazz) {
        for (Module m : MODULES) if (m.getClass() == clazz) return (T) m;
        throw new IllegalArgumentException("Brak modulu " + clazz.getSimpleName());
    }

    public static Module getByName(String name) {
        for (Module m : MODULES) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public static void onKeyPress(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;
        for (Module m : MODULES) if (m.getKey() == key) m.toggle();
    }

    public static void onTick() {
        if (MinecraftClient.getInstance().player == null) return;
        for (Module m : MODULES) if (m.isEnabled()) m.onTick();
    }

    public static void onRender2D(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        for (Module m : MODULES) if (m.isEnabled()) m.onRender2D(context, tickDelta);
    }
}
