package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ColorSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.setting.NumberSetting;
import dev.wave.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;

public class Crosshair extends Module {
    public final ModeSetting style = new ModeSetting("Style", "", "Cross", "Cross", "Dot", "Circle", "Cross + Dot");
    public final NumberSetting size = new NumberSetting("Size", "", 4, 1, 12, 0.5, "");
    public final NumberSetting gap = new NumberSetting("Gap", "", 2, 0, 8, 0.5, "").visible(() -> !style.is("Dot"));
    public final NumberSetting thickness = new NumberSetting("Thickness", "", 1, 0.5, 4, 0.5, "");
    public final BooleanSetting dynamic = new BooleanSetting("Dynamic", "Expand while moving", true);
    public final BooleanSetting outline = new BooleanSetting("Outline", true);
    public final ColorSetting color = new ColorSetting("Color", 0xFFFFFFFF);

    private float spread;

    public Crosshair() {
        super("Crosshair", "Replaces the vanilla crosshair with a custom one", Category.VISUALS);
        addSettings(style, size, gap, thickness, dynamic, outline, color);
        drawn.set(false);
    }

    @Override public String getSuffix() { return style.get(); }

    public void render(DrawContext c, int sw, int sh, float delta) {
        if (mc.player == null || !mc.options.getPerspective().isFirstPerson()) return;
        float cx = sw / 2f, cy = sh / 2f;
        double speed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
        float target = dynamic.isOn() ? (float) Math.min(4, speed * 12) : 0;
        spread += (target - spread) * 0.2f;
        float s = size.getFloat(), g = gap.getFloat() + spread, t = thickness.getFloat();
        int col = color.getColor();
        int out = 0xC0000000;
        boolean cross = style.is("Cross") || style.is("Cross + Dot");
        if (cross) {
            float[][] bars = {
                    {cx - t / 2, cy - g - s, t, s}, {cx - t / 2, cy + g, t, s},
                    {cx - g - s, cy - t / 2, s, t}, {cx + g, cy - t / 2, s, t}};
            if (outline.isOn()) for (float[] b : bars) RenderUtil.rect(c, b[0] - 0.5f, b[1] - 0.5f, b[2] + 1, b[3] + 1, out);
            for (float[] b : bars) RenderUtil.rect(c, b[0], b[1], b[2], b[3], col);
        }
        if (style.is("Dot") || style.is("Cross + Dot")) {
            float r = style.is("Dot") ? s / 2f : t;
            if (outline.isOn()) RenderUtil.circle(c, cx, cy, r + 0.5f, out);
            RenderUtil.circle(c, cx, cy, r, col);
        }
        if (style.is("Circle")) {
            float r = s + g;
            if (outline.isOn()) RenderUtil.roundOutline(c, cx - r - 0.5f, cy - r - 0.5f, r * 2 + 1, r * 2 + 1, r + 0.5f, t + 1, out);
            RenderUtil.roundOutline(c, cx - r, cy - r, r * 2, r * 2, r, t, col);
            RenderUtil.circle(c, cx, cy, 0.5f, col);
        }
    }
}
