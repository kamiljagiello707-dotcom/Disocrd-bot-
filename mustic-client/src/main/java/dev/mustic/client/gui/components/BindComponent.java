package dev.mustic.client.gui.components;

import dev.mustic.client.module.Module;
import dev.mustic.client.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class BindComponent extends Component {
    private final Module module;
    private boolean listening;

    public BindComponent(Module module) {
        this.module = module;
    }

    public void startListening() { listening = true; }
    public boolean isListening() { return listening; }

    @Override public int height() { return 13; }

    @Override
    protected void draw(DrawContext ctx, int mx, int my, boolean hovered) {
        ctx.drawText(font(), "Bind", x + 8, y + 3, hovered ? Theme.TEXT : Theme.TEXT_DIM, false);
        String val;
        int color;
        if (listening) {
            val = (System.currentTimeMillis() / 400) % 2 == 0 ? "..." : "..";
            color = Theme.accent(y * 8L);
        } else {
            val = "[" + keyName(module.getKey()) + "]";
            color = Theme.TEXT;
        }
        ctx.drawText(font(), val, x + w - 6 - font().getWidth(val), y + 3, color, false);
    }

    public static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "None";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hovered(mx, my) && button == 0) {
            listening = !listening;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!listening) return false;
        listening = false;
        if (key == GLFW.GLFW_KEY_ESCAPE) return true; // anuluj
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) module.setKey(GLFW.GLFW_KEY_UNKNOWN);
        else module.setKey(key);
        return true;
    }
}
