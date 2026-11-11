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
import tech.fuzir.client.config.Profiles;
import tech.fuzir.client.util.Theme;
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
        if (t == 135) ModuleManager.get(ArmorHud.class).setEnabled(true);
        if (t == 150) ModuleManager.get(Fullbright.class).setEnabled(true);
        if (t == 160) ModuleManager.get(NoWeather.class).setEnabled(true);
        if (t == 168) ModuleManager.get(Sprint.class).setEnabled(true);
        if (t == 176) shot(mc, "01-hud");
        if (t == 185) {
            cursor(mc, 900, 420);
            mc.setScreen(new ClickGuiScreen());
        }
        if (t == 225) shot(mc, "02-gui-ruch");
        ClickGuiScreen gui = s instanceof ClickGuiScreen g ? g : null;
        if (gui == null) {
            if (t > 430 && stage == 1) {
                stage = 2;
                System.out.println("[autotest] koniec");
                mc.scheduleStop();
            }
            return;
        }
        if (t == 230) {
            gui.openPage("Wizualne");
            gui.openSettings(ModuleManager.get(Zoom.class));
        }
        if (t == 265) shot(mc, "03-gui-wizualne");
        if (t == 270) {
            gui.openPage("Szukaj");
            gui.type("auto");
        }
        if (t == 300) shot(mc, "04-gui-szukaj");
        if (t == 305) {
            Profiles.save("PvP");
            Profiles.save("Budowanie");
            Profiles.save("Anarchia");
            gui.openPage("Configi");
        }
        if (t == 335) shot(mc, "05-gui-configi");
        if (t == 340) {
            gui.openPage("Motywy");
            Theme.select("Północ");
            ModuleManager.get(ClickGui.class).theme.set("Północ");
        }
        if (t == 375) shot(mc, "06-gui-motywy");
        if (t == 380) {
            gui.openPage("Gracz");
            gui.openSettings(ModuleManager.get(AutoTool.class));
        }
        if (t == 405) shot(mc, "07-gui-polnoc");
        if (t == 410) {
            Theme.select("Monochrom");
            ModuleManager.get(ClickGui.class).theme.set("Monochrom");
            gui.close();
        }
    }
}
