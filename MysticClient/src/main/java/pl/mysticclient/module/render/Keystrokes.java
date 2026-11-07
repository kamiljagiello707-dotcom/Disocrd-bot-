package pl.mysticclient.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;
import pl.mysticclient.util.UI.Weight;

import java.util.ArrayDeque;
import java.util.Deque;

public class Keystrokes extends Module {
    private final BoolSetting mouse = add(new BoolSetting("Przyciski myszy", true));
    private final BoolSetting space = add(new BoolSetting("Spacja", true));

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private final float[] anims = new float[7];

    public Keystrokes() {
        super("Keystrokes", "Pokazuje wciśnięte klawisze i CPS.", Category.RENDER);
        hideFromArrayList();
    }

    public static void registerClick(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) leftClicks.add(now); else if (button == 1) rightClicks.add(now);
    }

    private static int cps(Deque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        float s = 38, g = 4;
        float x = 12, y = 56;
        key(0, mc.options.forwardKey, "W", null, x + s + g, y, s, s);
        key(1, mc.options.leftKey, "A", null, x, y + s + g, s, s);
        key(2, mc.options.backKey, "S", null, x + s + g, y + s + g, s, s);
        key(3, mc.options.rightKey, "D", null, x + 2 * (s + g), y + s + g, s, s);
        float full = 3 * s + 2 * g;
        float cy = y + 2 * (s + g);
        if (mouse.get()) {
            float half = (full - g) / 2;
            key(4, mc.options.attackKey, "LMB", cps(leftClicks) + " CPS", x, cy, half, s);
            key(5, mc.options.useKey, "RMB", cps(rightClicks) + " CPS", x + half + g, cy, half, s);
            cy += s + g;
        }
        if (space.get()) key(6, mc.options.jumpKey, null, null, x, cy, full, 20);
    }

    private void key(int idx, KeyBinding kb, String label, String sub, float x, float y, float w, float h) {
        anims[idx] = UI.approach(anims[idx], kb.isPressed() ? 1f : 0f, 20f);
        float a = anims[idx];
        UI.round(x, y, w, h, 9, UI.mix(Theme.glass(), Theme.accent(), a));
        UI.outline(x, y, w, h, 9, 1, UI.a(Theme.glassBorder(), 1f - a));
        int tc = UI.mix(Theme.text(), Theme.onAccent(), a);
        if (label == null) {
            UI.round(x + w / 2f - 18, y + h / 2f - 1, 36, 2, 1, UI.a(tc, 0.7f));
        } else if (sub != null) {
            UI.textCenter(label, x + w / 2f, y + h / 2f - 6, 12, Weight.SEMIBOLD, tc);
            UI.textCenter(sub, x + w / 2f, y + h / 2f + 8, 9.5f, Weight.MEDIUM, UI.a(tc, 0.6f));
        } else {
            UI.textCenter(label, x + w / 2f, y + h / 2f, 14, Weight.SEMIBOLD, tc);
        }
    }
}
