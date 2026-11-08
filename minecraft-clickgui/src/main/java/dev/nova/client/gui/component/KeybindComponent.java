package dev.nova.client.gui.component;

import dev.nova.client.setting.KeybindSetting;
import dev.nova.client.setting.Setting;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** Click to listen, press a key to bind. Escape / Backspace / Delete clears. */
public class KeybindComponent extends Component {
    private final KeybindSetting setting;
    private boolean listening;

    public KeybindComponent(KeybindSetting s) { this.setting = s; }

    @Override public Setting<?> setting() { return setting; }
    @Override public double height() { return 14; }

    public boolean isListening() { return listening; }

    @Override
    protected void draw(DrawContext ctx, int mx, int my) {
        text(ctx, setting.getName(), x + 6, y + 3, Theme.TEXT);
        String v = listening ? dots() : setting.display();
        int vw = font().getWidth(v);
        double px = x + w - vw - 12;
        Render2D.roundRect(ctx, px, y + 1.5, vw + 8, 11, 3, c(listening ? 0xFF3A3050 : 0xFF262632));
        text(ctx, v, px + 4, y + 3, listening ? Theme.accent(px) : Theme.TEXT_DIM);
    }

    private static String dots() {
        int n = (int) (System.currentTimeMillis() / 300 % 4);
        return "." .repeat(Math.max(1, n));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening) {
            // Allow binding mouse side buttons? Keep it simple: any click cancels.
            listening = false;
            return true;
        }
        if (isHovered(mx, my) && button == 0) { listening = true; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!listening) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) setting.set(-1);
        else setting.set(key);
        listening = false;
        return true;
    }
}
