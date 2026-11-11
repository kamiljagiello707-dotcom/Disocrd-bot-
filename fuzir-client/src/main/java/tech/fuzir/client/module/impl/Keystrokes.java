package tech.fuzir.client.module.impl;

import net.minecraft.client.option.KeyBinding;
import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;

import java.util.HashMap;
import java.util.Map;

public class Keystrokes extends HudModule {
    public final BoolSetting mouse = add(new BoolSetting("Przyciski myszy", true));
    public final BoolSetting space = add(new BoolSetting("Spacja", true));
    private final Map<String, Anim> anims = new HashMap<>();
    private static final float S = 24, G = 3;

    public Keystrokes() {
        super("Klawisze", "Podgląd wciskanych klawiszy WASD", Icons.POINTER, 0f, 0.62f);
    }

    @Override
    public float[] size(Canvas cv) {
        float h = S * 2 + G;
        if (mouse.on()) h += G + 18;
        if (space.on()) h += G + 12;
        return new float[]{S * 3 + G * 2, h};
    }

    private void key(Canvas cv, String id, String label, float x, float y, float w, float h, boolean down) {
        float a = anims.computeIfAbsent(id, k -> new Anim(0)).to(down ? 1 : 0, 20);
        float shrink = a * 1.2f;
        float kx = x + shrink, ky = y + shrink, kw = w - shrink * 2, kh = h - shrink * 2;
        cv.shadow(kx, ky + 1.5f, kw, kh, 6, 6, 0x60000000);
        cv.round(kx, ky, kw, kh, 6, Canvas.mix(Hud.PANEL, Hud.ACCENT, a));
        cv.outline(kx, ky, kw, kh, 6, 1f, Canvas.mix(Hud.BORDER, 0x00FFFFFF, a));
        int tc = Canvas.mix(Hud.TEXT, Hud.ON_ACCENT, a);
        if (label.isEmpty()) cv.round(x + w / 2 - 10, y + h / 2 - 1, 20, 2, 1, tc);
        else cv.textCenter(label, x + w / 2, y + h / 2, tc, F.BOLD);
    }

    @Override
    public void draw(Canvas cv, float x, float y, float w, float h, boolean right) {
        KeyBinding[] k = {mc.options.forwardKey, mc.options.leftKey, mc.options.backKey, mc.options.rightKey};
        key(cv, "w", "W", x + S + G, y, S, S, k[0].isPressed());
        key(cv, "a", "A", x, y + S + G, S, S, k[1].isPressed());
        key(cv, "s", "S", x + S + G, y + S + G, S, S, k[2].isPressed());
        key(cv, "d", "D", x + (S + G) * 2, y + S + G, S, S, k[3].isPressed());
        float ry = y + (S + G) * 2;
        if (mouse.on()) {
            float half = (w - G) / 2;
            key(cv, "lmb", "LPM", x, ry, half, 18, mc.options.attackKey.isPressed());
            key(cv, "rmb", "PPM", x + half + G, ry, half, 18, mc.options.useKey.isPressed());
            ry += 18 + G;
        }
        if (space.on()) key(cv, "space", "", x, ry, w, 12, mc.options.jumpKey.isPressed());
    }
}
