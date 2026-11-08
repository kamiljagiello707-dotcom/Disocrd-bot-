package dev.wave.hud.elements;

import dev.wave.WaveClient;
import dev.wave.hud.HudElement;
import dev.wave.setting.BooleanSetting;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

public class KeystrokesHud extends HudElement {
    public final BooleanSetting mouse = new BooleanSetting("Mouse Buttons", true);
    public final BooleanSetting cps = new BooleanSetting("CPS", true).visible(mouse::isOn);
    public final BooleanSetting space = new BooleanSetting("Space Bar", true);

    private final Animation[] anims = new Animation[7];

    public KeystrokesHud() {
        super("Keystrokes", "Shows your movement keys and CPS");
        addSettings(mouse, cps, space, background);
        for (int i = 0; i < anims.length; i++) anims[i] = new Animation(18, 0);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { x = 4; y = sh / 2f - 40; }

    private void key(DrawContext c, int idx, KeyBinding bind, String label, float kx, float ky, float kw, float kh) {
        anims[idx].setTarget(bind.isPressed() ? 1 : 0);
        float a = anims[idx].update();
        RenderUtil.round(c, kx, ky, kw, kh, 3, ColorUtil.lerp(Theme.HUD_BG, ColorUtil.withAlpha(Theme.accent(), 0xE6), a));
        int tc = ColorUtil.lerp(Theme.TEXT, Theme.onAccent(), a);
        String[] parts = label.split("\n");
        if (parts.length == 1) {
            RenderUtil.text(c, parts[0], kx + kw / 2 - RenderUtil.width(parts[0]) / 2f, ky + kh / 2 - 4, tc, false);
        } else {
            RenderUtil.text(c, parts[0], kx + kw / 2 - RenderUtil.width(parts[0]) / 2f, ky + 3, tc, false);
            RenderUtil.textScaled(c, parts[1], kx + kw / 2 - RenderUtil.width(parts[1]) * 0.35f, ky + kh - 8, 0.7f, ColorUtil.alpha(tc, 0.8f), false);
        }
    }

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        float k = 22, g = 2;
        float w = k * 3 + g * 2;
        float h = k * 2 + g;
        if (mouse.isOn()) h += k + g;
        if (space.isOn()) h += 12 + g;
        resize(w, h);
        var o = mc.options;
        key(c, 0, o.forwardKey, name(o.forwardKey), x + k + g, y, k, k);
        key(c, 1, o.leftKey, name(o.leftKey), x, y + k + g, k, k);
        key(c, 2, o.backKey, name(o.backKey), x + k + g, y + k + g, k, k);
        key(c, 3, o.rightKey, name(o.rightKey), x + (k + g) * 2, y + k + g, k, k);
        float cy = y + (k + g) * 2;
        if (mouse.isOn()) {
            float mw = (w - g) / 2;
            String l = cps.isOn() ? "LMB\n" + WaveClient.INSTANCE.leftCps() + " CPS" : "LMB";
            String r = cps.isOn() ? "RMB\n" + WaveClient.INSTANCE.rightCps() + " CPS" : "RMB";
            key(c, 4, o.attackKey, l, x, cy, mw, k);
            key(c, 5, o.useKey, r, x + mw + g, cy, mw, k);
            cy += k + g;
        }
        if (space.isOn()) {
            anims[6].setTarget(o.jumpKey.isPressed() ? 1 : 0);
            float a = anims[6].update();
            RenderUtil.round(c, x, cy, w, 12, 3, ColorUtil.lerp(Theme.HUD_BG, ColorUtil.withAlpha(Theme.accent(), 0xE6), a));
            RenderUtil.rect(c, x + w / 2 - 12, cy + 5.5f, 24, 1, ColorUtil.lerp(Theme.TEXT, Theme.onAccent(), a));
        }
    }

    private static String name(KeyBinding b) {
        String s = b.getBoundKeyLocalizedText().getString();
        return s.length() > 3 ? s.substring(0, 3) : s.toUpperCase();
    }
}
