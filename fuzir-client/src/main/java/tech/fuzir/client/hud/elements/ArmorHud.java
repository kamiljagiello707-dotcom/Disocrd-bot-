package tech.fuzir.client.hud.elements;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.hud.HudElement;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Worn armor (helmet → boots) plus optional held items, each with a colored durability percentage.
 */
public final class ArmorHud extends HudElement {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final float SLOT = 18f;
    private static final float PADDING = 3f;

    private final BoolSetting hands = bool("Held Items", true);
    private final BoolSetting durability = bool("Durability", true);

    private final List<ItemStack> stacks = new ArrayList<>();

    public ArmorHud() {
        super("ArmorHUD", "Shows your armor and held items with durability.", Anchor.CENTER, Anchor.END, 0f, 58f);
    }

    @Override
    public void measure(boolean editor) {
        stacks.clear();
        if (mc.player != null) {
            if (hands.isEnabled()) stacks.add(mc.player.getMainHandStack());
            for (EquipmentSlot slot : ARMOR) stacks.add(mc.player.getEquippedStack(slot));
            if (hands.isEnabled()) stacks.add(mc.player.getOffHandStack());
        }
        if (!editor) stacks.removeIf(ItemStack::isEmpty);
        int count = editor ? Math.max(stacks.size(), ARMOR.length) : stacks.size();
        width = Math.max(SLOT, count * SLOT) + PADDING * 2f;
        height = 16f + PADDING * 2f + (durability.isEnabled() ? 9f : 0f);
    }

    @Override
    public void render(DrawContext context, float x, float y, float tickDelta, boolean editor) {
        if (stacks.isEmpty() && !editor) return;
        MatrixStack matrices = context.getMatrices();
        drawPanel(context, x, y, width, height);

        float cursor = x + PADDING + 1f;
        int slots = editor ? Math.max(stacks.size(), ARMOR.length) : stacks.size();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            float itemY = y + PADDING;
            if (stack.isEmpty()) {
                RenderUtil.roundedOutline(matrices, cursor, itemY, 16f, 16f, 3f, 0.6f, Theme.BORDER);
            } else {
                matrices.push();
                matrices.translate(cursor, itemY, 0f);
                context.drawItem(stack, 0, 0);
                context.drawItemInSlot(mc.textRenderer, stack, 0, 0);
                matrices.pop();
                if (durability.isEnabled() && stack.isDamageable()) {
                    float remaining = 1f - (float) stack.getDamage() / stack.getMaxDamage();
                    String text = Math.round(remaining * 100f) + "%";
                    int color = ColorUtil.interpolate(0xFFFF5C6C, Theme.accent(), remaining);
                    matrices.push();
                    // Items are drawn at a raised z, keep the label above them.
                    matrices.translate(0f, 0f, 200f);
                    Fonts.draw(context, text, cursor + 8f - Fonts.width(text) * 0.75f / 2f, itemY + 18f, color,
                            Fonts.MEDIUM, 0.75f, false);
                    matrices.pop();
                }
            }
            cursor += SLOT;
        }
    }
}
