package tech.fuzir.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.fuzir.client.gui.HudEditorScreen;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudManager {
    private final List<HudElement> elements = new ArrayList<>();

    public HudManager(ModuleManager modules) {
        for (Module module : modules.getModules()) {
            if (module instanceof HudElement element) elements.add(element);
        }
    }

    public List<HudElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    /** In-game HUD pass, hooked into Fabric's HudRenderCallback. */
    public void render(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.options.debugEnabled || mc.currentScreen instanceof HudEditorScreen) return;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        for (HudElement element : elements) {
            if (!element.isEnabled()) continue;
            draw(context, element, screenWidth, screenHeight, tickDelta, false);
        }
    }

    public static void draw(DrawContext context, HudElement element, int screenWidth, int screenHeight,
                            float tickDelta, boolean editor) {
        element.measure(editor);
        float x = element.getX(screenWidth);
        float y = element.getY(screenHeight);
        element.render(context, x, y, tickDelta, editor);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
