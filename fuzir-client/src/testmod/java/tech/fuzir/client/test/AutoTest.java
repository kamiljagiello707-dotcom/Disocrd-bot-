package tech.fuzir.client.test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.gui.ClickGuiScreen;
import tech.fuzir.client.gui.HudEditorScreen;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.*;

/** Tylko do testów: tworzy świat, klika po menu i robi zrzuty ekranu. */
public class AutoTest implements ClientModInitializer {
    private int stage, t;
    private boolean created;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void shot(MinecraftClient mc, String name) {
        ScreenshotRecorder.saveScreenshot(mc.runDirectory, name + ".png", mc.getFramebuffer(), m -> System.out.println("[autotest] " + m.getString()));
    }

    private void cursor(MinecraftClient mc, double x, double y) {
        GLFW.glfwSetCursorPos(mc.getWindow().getHandle(), x, y);
    }

    private void tick(MinecraftClient mc) {
        Screen s = mc.currentScreen;
        if (mc.player == null) {
            if (s instanceof TitleScreen && !created) {
                created = true;
                CreateWorldScreen.show(mc, s);
            }
            if (s instanceof CreateWorldScreen cw && stage == 0) {
                cw.getWorldCreator().setWorldName("Fuzir test");
                cw.getWorldCreator().setGameMode(WorldCreator.Mode.CREATIVE);
                cw.getWorldCreator().setCheatsEnabled(true);
                for (var child : cw.children()) {
                    if (child instanceof ButtonWidget b && b.getMessage().getString().equals(Text.translatable("selectWorld.create").getString())) {
                        stage = 1;
                        b.onPress();
                        break;
                    }
                }
            }
            return;
        }
        t++;
        if (t == 1) {
            mc.player.networkHandler.sendChatCommand("time set 23200");
            mc.player.networkHandler.sendChatCommand("weather clear");
            mc.player.networkHandler.sendChatCommand("tp @s ~ ~12 ~ -30 8");
            mc.player.getAbilities().flying = true;
            ModuleManager.get(Sprint.class).bind.set(GLFW.GLFW_KEY_R);
            ModuleManager.get(Fullbright.class).bind.set(GLFW.GLFW_KEY_G);
            ModuleManager.get(Zoom.class).bind.set(GLFW.GLFW_KEY_Z);
            ModuleManager.get(NoWeather.class).bind.set(GLFW.GLFW_KEY_N);
            ModuleManager.get(AutoWalk.class).bind.set(GLFW.GLFW_KEY_V);
            ModuleManager.get(AutoWalk.class).bindMode.set("Przytrzymaj");
        }
        if (t == 130) ModuleManager.get(Keystrokes.class).setEnabled(true);
        if (t == 150) ModuleManager.get(Fullbright.class).setEnabled(true);
        if (t == 160) ModuleManager.get(NoWeather.class).setEnabled(true);
        if (t == 168) ModuleManager.get(Sprint.class).setEnabled(true);
        if (t == 176) shot(mc, "01-hud");
        if (t == 185) {
            cursor(mc, 820, 380);
            mc.setScreen(new ClickGuiScreen());
        }
        if (t == 225) shot(mc, "02-gui-ruch");
        ClickGuiScreen gui = s instanceof ClickGuiScreen g ? g : null;
        if (t == 230 && gui != null) gui.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
        if (t == 260) shot(mc, "03-gui-render");
        if (t == 265 && gui != null) gui.openSettings(ModuleManager.get(Zoom.class));
        if (t == 295) shot(mc, "04-gui-ustawienia");
        if (t == 300 && gui != null) {
            gui.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
            gui.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
        }
        if (t == 330) shot(mc, "05-gui-hud");
        if (t == 335 && gui != null) {
            gui.charTyped('k', 0);
            gui.charTyped('e', 0);
            gui.charTyped('y', 0);
        }
        if (t == 360) shot(mc, "06-gui-szukaj");
        if (t == 365) {
            cursor(mc, 60, 240);
            mc.setScreen(new HudEditorScreen());
        }
        if (t == 395) shot(mc, "07-edytor-hud");
        if (t == 400) mc.setScreen(null);
        if (t == 430 && stage == 1) {
            stage = 2;
            System.out.println("[autotest] koniec");
            mc.scheduleStop();
        }
    }
}
