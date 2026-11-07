package dev.testclient;

import dev.testclient.gui.ClickGuiScreen;
import dev.testclient.gui.Hud;
import dev.testclient.module.Module;
import dev.testclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestClient implements ClientModInitializer {
    public static final String NAME = "TestClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    static final List<Module> pendingEnable = new ArrayList<>();
    private static ModuleManager modules;
    private static KeyBinding guiKey;
    private final boolean[] keyState = new boolean[GLFW.GLFW_KEY_LAST + 1];

    public static ModuleManager modules() { return modules; }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        Config.load();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.testclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.testclient"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());

        LOGGER.info("{} załadowany - Right Shift otwiera ClickGUI", NAME);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null) return;

        if (!pendingEnable.isEmpty()) {
            pendingEnable.forEach(m -> m.setEnabled(true));
            pendingEnable.clear();
        }

        while (guiKey.wasPressed()) client.setScreen(new ClickGuiScreen());

        // Bindy modułów działają tylko gdy nie ma otwartego ekranu (czat, ekwipunek, GUI)
        long window = client.getWindow().getHandle();
        for (Module m : modules.getModules()) {
            int key = m.getKey();
            if (key < 0 || key > GLFW.GLFW_KEY_LAST) continue;
            boolean down = client.currentScreen == null && InputUtil.isKeyPressed(window, key);
            if (down && !keyState[key]) m.toggle();
            keyState[key] = down;
        }

        modules.onTick();
    }
}
