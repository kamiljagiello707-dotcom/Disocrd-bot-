package tech.fuzir.client.event.events;

import net.minecraft.client.gui.DrawContext;

/**
 * Posted every frame after the Fuzir HUD has been drawn on top of the vanilla in-game HUD.
 */
public record Render2DEvent(DrawContext context, float tickDelta) {
}
