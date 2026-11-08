package dev.wave.gui.components;

import dev.wave.gui.ClickGuiScreen;
import dev.wave.module.Module;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.KeyUtil;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class BindComponent extends Component {
    private final Module module;
    private final Animation listen = new Animation(14, 0);

    public BindComponent(Module module) {
        super(null);
        this.module = module;
    }

    public Module getModule() { return module; }

    public boolean isListening() { return ClickGuiScreen.listening == this; }

    @Override public float baseHeight() { return 19; }

    @Override
    protected void render(DrawContext c, int mx, int my) {
        listen.setTarget(isListening() ? 1 : 0);
        float l = listen.update();
        RenderUtil.text(c, "Keybind", x + 2, y + 5.5f, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(l, hover.get() * 0.6f)));
        String label;
        if (isListening()) {
            int dots = (int) (System.currentTimeMillis() / 300 % 4);
            label = "Press" + ".".repeat(dots);
        } else label = KeyUtil.name(module.getKey());
        float bw = Math.max(26, RenderUtil.width(label) + 8);
        float bx = x + w - bw - 1;
        int bg = ColorUtil.lerp(0xFF1E1E1E, Theme.accent(), l);
        RenderUtil.round(c, bx, y + 3.5f, bw, 12, 4, bg);
        if (!isListening()) RenderUtil.roundOutline(c, bx, y + 3.5f, bw, 12, 4, 0.5f, ColorUtil.alpha(Theme.BORDER, 1 + hover.get()));
        RenderUtil.text(c, label, bx + bw / 2 - RenderUtil.width(label) / 2f, y + 5.5f, ColorUtil.lerp(Theme.TEXT, Theme.onAccent(), l));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isListening()) {
            if (button >= 2) module.setKey(KeyUtil.mouse(button));
            ClickGuiScreen.listening = null;
            ClickGuiScreen.click(1.0f);
            return true;
        }
        if (!isHovered(mx, my)) return false;
        if (button == 0) {
            ClickGuiScreen.listening = this;
            ClickGuiScreen.click(1.3f);
        } else if (button == 1) {
            module.setKey(KeyUtil.NONE);
            ClickGuiScreen.click(0.8f);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!isListening()) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) module.setKey(KeyUtil.NONE);
        else module.setKey(key);
        ClickGuiScreen.listening = null;
        ClickGuiScreen.click(1.0f);
        return true;
    }

    @Override
    public String tooltip() { return "LMB: set bind  |  RMB / Esc: clear bind"; }
}
