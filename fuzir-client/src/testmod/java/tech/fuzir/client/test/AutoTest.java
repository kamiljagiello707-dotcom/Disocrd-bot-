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
import tech.fuzir.client.module.Module;
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
        }
        if (t == 140) ModuleManager.get(Keystrokes.class).setEnabled(true);
        if (t == 150) ModuleManager.get(Sprint.class).setEnabled(true);
        if (t == 160) ModuleManager.get(Fullbright.class).setEnabled(true);
        if (t == 170) ModuleManager.get(NoWeather.class).setEnabled(true);
        if (t == 190) shot(mc, "01-hud");
        if (t == 200) {
            cursor(mc, 1100, 330);
            mc.setScreen(new ClickGuiScreen());
        }
        if (t == 240) shot(mc, "02-gui-ruch");
        ClickGuiScreen gui = s instanceof ClickGuiScreen g ? g : null;
        if (gui == null) {
            if (t > 400 && stage == 1) {
                stage = 2;
                System.out.println("[autotest] koniec");
                mc.scheduleStop();
            }
            return;
        }
        if (t == 250) {
            gui.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
            ModuleManager.get(Zoom.class).expanded = true;
            cursor(mc, 1400, 1000);
        }
        if (t == 285) shot(mc, "03-gui-render");
        if (t == 295) {
            gui.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
            ModuleManager.get(Watermark.class).expanded = true;
        }
        if (t == 330) shot(mc, "04-gui-hud");
        if (t == 340) {
            gui.charTyped('s', 0);
            gui.charTyped('p', 0);
        }
        if (t == 365) shot(mc, "05-gui-szukaj");
        if (t == 370) {
            gui.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
            gui.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
            gui.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
            for (Module m : ModuleManager.all()) m.expanded = m instanceof ClickGui;
        }
        if (t == 395) shot(mc, "06-gui-klient");
        if (t == 400) gui.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
    }
}
