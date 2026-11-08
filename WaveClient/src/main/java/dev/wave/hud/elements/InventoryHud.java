package dev.wave.hud.elements;

import dev.wave.hud.HudElement;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class InventoryHud extends HudElement {
    public InventoryHud() {
        super("Inventory", "Preview of your inventory");
        addSettings(background);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { x = sw / 2f - 82; y = 4; }

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        float w = 9 * 18 + 4, h = 3 * 18 + 4;
        resize(w, h);
        if (background.isOn()) RenderUtil.round(c, x, y, w, h, 4, Theme.HUD_BG);
        for (int i = 0; i < 27; i++) {
            float sx = x + 2 + (i % 9) * 18, sy = y + 2 + (i / 9) * 18;
            RenderUtil.round(c, sx + 0.5f, sy + 0.5f, 17, 17, 2, 0x25FFFFFF);
            if (mc.player == null) continue;
            ItemStack st = mc.player.getInventory().getStack(9 + i);
            if (st.isEmpty()) continue;
            c.drawItem(st, (int) sx + 1, (int) sy + 1);
            c.drawStackOverlay(mc.textRenderer, st, (int) sx + 1, (int) sy + 1);
        }
    }
}
