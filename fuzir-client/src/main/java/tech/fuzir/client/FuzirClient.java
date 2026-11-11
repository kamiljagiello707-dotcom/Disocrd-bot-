package tech.fuzir.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import tech.fuzir.client.module.impl.ChinaHat;
import tech.fuzir.client.module.impl.JumpCircles;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fuzir.client.config.Config;
import tech.fuzir.client.gui.ClickGuiScreen;
import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.ModuleManager;

public class FuzirClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Fuzir Client");
    public static KeyBinding OPEN_KEY;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fuzir-client.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.fuzir-client"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_KEY.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
            }
            ModuleManager.tick(mc);
        });
        HudRenderCallback.EVENT.register(Hud::render);
        WorldRenderEvents.LAST.register(ctx -> {
            JumpCircles jc = ModuleManager.get(JumpCircles.class);
            if (jc.isEnabled()) jc.render(ctx);
            ChinaHat hat = ModuleManager.get(ChinaHat.class);
            if (hat.isEnabled()) hat.render(ctx);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Config.save());
        LOG.info("Fuzir Client gotowy: {} modułów", ModuleManager.all().size());
    }
}
