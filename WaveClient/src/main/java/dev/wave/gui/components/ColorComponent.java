package dev.wave.gui.components;

import dev.wave.gui.ClickGuiScreen;
import dev.wave.setting.ColorSetting;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class ColorComponent extends Component {
    private static final float EXTRA = 76;
    private final ColorSetting s;
    private final Animation expand = new Animation(16, 0);
    private final Animation rainbowKnob;
    private boolean open;
    private int dragging; // 0 none, 1 sb, 2 hue, 3 alpha

    public ColorComponent(ColorSetting s) {
        super(s);
        this.s = s;
        this.rainbowKnob = new Animation(16, s.isRainbow() ? 1 : 0);
    }

    @Override public float baseHeight() { return 19 + EXTRA * expand.get(); }

    @Override
    protected void render(DrawContext c, int mx, int my) {
        expand.setTarget(open ? 1 : 0);
        float e = expand.update();
        boolean headerHover = RenderUtil.hovered(mx, my, x, y, w, 19);
        RenderUtil.text(c, s.getName(), x + 2, y + 5.5f, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, headerHover ? 0.6f : 0));
        float sx = x + w - 20;
        RenderUtil.round(c, sx, y + 4.5f, 18, 10, 3.5f, 0xFF2A2A2A);
        RenderUtil.round(c, sx + 1, y + 5.5f, 16, 8, 3, ColorUtil.withAlpha(s.getColor(), 255));

        if (e < 0.01f) return;
        float px = x + 2, pw = w - 4;
        float sqY = y + 21, sqH = 38;
        float hueY = sqY + sqH + 4, alphaY = hueY + 9, rbY = alphaY + 8;

        if (dragging == 1) {
            s.setSaturation((mx - px) / pw);
            s.setBrightness(1 - (my - sqY) / sqH);
        } else if (dragging == 2) {
            s.setHue((mx - px) / pw);
        } else if (dragging == 3) {
            s.setAlpha((mx - px) / pw);
        }

        RenderUtil.scissor(c, x, y + 19, w, EXTRA * e);
        int hueColor = ColorUtil.hsb(s.getHue(), 1, 1, 255);
        RenderUtil.gradientH(c, px, sqY, pw, sqH, 0xFFFFFFFF, hueColor);
        RenderUtil.gradientV(c, px, sqY, pw, sqH, 0x00000000, 0xFF000000);
        float kx = px + s.getSaturation() * pw, ky = sqY + (1 - s.getBrightness()) * sqH;
        RenderUtil.roundOutline(c, kx - 3, ky - 3, 6, 6, 3, 1, 0xFFFFFFFF);

        // hue bar
        int[] stops = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        float seg = pw / 6f;
        for (int i = 0; i < 6; i++) RenderUtil.gradientH(c, px + i * seg, hueY, seg, 5, stops[i], stops[i + 1]);
        RenderUtil.rect(c, px + s.getHue() * pw - 1, hueY - 1, 2, 7, 0xFFFFFFFF);

        // alpha bar
        RenderUtil.rect(c, px, alphaY, pw, 5, 0xFF3A3A3A);
        RenderUtil.gradientH(c, px, alphaY, pw, 5, ColorUtil.withAlpha(s.get(), 0), ColorUtil.withAlpha(s.get(), 255));
        RenderUtil.rect(c, px + s.getAlphaF() * pw - 1, alphaY - 1, 2, 7, 0xFFFFFFFF);

        // rainbow toggle
        rainbowKnob.setTarget(s.isRainbow() ? 1 : 0);
        float k = rainbowKnob.update();
        RenderUtil.text(c, "Rainbow", x + 2, rbY + 2, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, k));
        float tx = x + w - 18, ty = rbY + 2;
        RenderUtil.round(c, tx, ty, 16, 8, 4, ColorUtil.lerp(0xFF2A2A2A, ColorUtil.rainbow(0, 0.6f, 1, 1), k));
        RenderUtil.circle(c, tx + 4 + k * 8, ty + 4, 2.8f, ColorUtil.lerp(0xFF8A8A8A, 0xFFFFFFFF, k));
        RenderUtil.endScissor(c);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (RenderUtil.hovered(mx, my, x, y, w, 19)) {
            if (button == 0 || button == 1) {
                open = !open;
                ClickGuiScreen.click(open ? 1.2f : 0.9f);
            }
            return true;
        }
        if (!open || expand.get() < 0.9f || button != 0) return false;
        float px = x + 2, pw = w - 4;
        float sqY = y + 21, sqH = 38;
        float hueY = sqY + sqH + 4, alphaY = hueY + 9, rbY = alphaY + 8;
        if (RenderUtil.hovered(mx, my, px, sqY, pw, sqH)) dragging = 1;
        else if (RenderUtil.hovered(mx, my, px, hueY - 1, pw, 7)) dragging = 2;
        else if (RenderUtil.hovered(mx, my, px, alphaY - 1, pw, 7)) dragging = 3;
        else if (RenderUtil.hovered(mx, my, x, rbY, w, 12)) {
            s.setRainbow(!s.isRainbow());
            ClickGuiScreen.click(1.2f);
        } else return false;
        return true;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) { dragging = 0; }
}
