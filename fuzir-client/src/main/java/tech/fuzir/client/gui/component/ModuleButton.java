package tech.fuzir.client.gui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.render.Scissor;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.KeySetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.setting.Setting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

import java.util.ArrayList;
import java.util.List;

/**
 * Module row: left click toggles, right click expands the settings drawer.
 */
public final class ModuleButton extends Component {
    public static final float HEIGHT = 18f;

    private final Module module;
    private final List<SettingComponent<?>> components = new ArrayList<>();
    private final Animation toggle;
    private final Animation hover = new Animation(Easing.OUT_QUAD, 160);
    private final Animation expand = new Animation(Easing.OUT_CUBIC, 280);
    private boolean expanded;
    private boolean hovered;

    public ModuleButton(Module module) {
        this.module = module;
        this.toggle = new Animation(Easing.OUT_CUBIC, 220, module.isEnabled() ? 1 : 0);
        for (Setting<?> setting : module.getSettings()) {
            SettingComponent<?> component = create(setting);
            if (component != null) components.add(component);
        }
        components.add(new KeyComponent(module.getBind()));
    }

    private static SettingComponent<?> create(Setting<?> setting) {
        if (setting instanceof BoolSetting bool) return new BoolComponent(bool);
        if (setting instanceof NumberSetting number) return new SliderComponent(number);
        if (setting instanceof ModeSetting mode) return new ModeComponent(mode);
        if (setting instanceof KeySetting key) return new KeyComponent(key);
        return null;
    }

    public Module getModule() {
        return module;
    }

    public boolean isHoveredRow() {
        return hovered;
    }

    private float settingsHeight() {
        float total = 0f;
        for (SettingComponent<?> component : components) total += component.getHeight();
        return total + 3f;
    }

    @Override
    public float getHeight() {
        return HEIGHT + settingsHeight() * expand.animateTo(expanded).getFloat();
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        hovered = isHovered(mouseX, mouseY, x, y, width, HEIGHT);
        float hoverProgress = hover.animateTo(hovered).getFloat();
        float enabled = toggle.animateTo(module.isEnabled()).getFloat();
        float expandProgress = expand.animateTo(expanded).getFloat();
        int accent = Theme.accent();

        if (hoverProgress > 0.01f) {
            RenderUtil.rect(matrices, x, y, width, HEIGHT, ColorUtil.multiplyAlpha(Theme.ELEMENT_HOVER, hoverProgress));
        }
        if (enabled > 0.01f) {
            // Accent wash fading out to the right plus a glowing indicator bar on the left edge.
            RenderUtil.horizontalGradient(matrices, x, y, width * 0.85f, HEIGHT,
                    ColorUtil.withAlpha(accent, (int) (46 * enabled)), ColorUtil.withAlpha(accent, 0));
            float barHeight = (HEIGHT - 8f) * enabled;
            float barY = y + (HEIGHT - barHeight) / 2f;
            RenderUtil.shadow(matrices, x + 1f, barY, 2f, barHeight, 1f, 3f, ColorUtil.withAlpha(accent, (int) (90 * enabled)));
            RenderUtil.roundedRect(matrices, x + 1f, barY, 2f, barHeight, 1f, ColorUtil.multiplyAlpha(accent, enabled));
        }

        int nameColor = ColorUtil.interpolate(Theme.TEXT_DIM, Theme.TEXT, hoverProgress * 0.7f);
        nameColor = ColorUtil.interpolate(nameColor, accent, enabled);
        Fonts.draw(context, module.getName(), x + 7f + 2f * enabled, y + (HEIGHT - Fonts.HEIGHT) / 2f, nameColor);

        // Expand indicator: chevron rotating downwards and tinting to the accent while open.
        float cx = x + width - 9f;
        float cy = y + HEIGHT / 2f;
        matrices.push();
        matrices.translate(cx, cy, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f * expandProgress));
        int indicator = ColorUtil.interpolate(Theme.TEXT_MUTED, accent, Math.max(expandProgress, hoverProgress * 0.4f));
        Fonts.draw(context, "›", -Fonts.width("›") / 2f, -Fonts.HEIGHT / 2f, indicator);
        matrices.pop();

        if (expandProgress <= 0.005f) return;

        float drawerY = y + HEIGHT;
        float drawerHeight = settingsHeight() * expandProgress;
        Scissor.push(context, x, drawerY, width, drawerHeight);
        RenderUtil.rect(matrices, x, drawerY, width, drawerHeight, Theme.SETTINGS_BG);
        RenderUtil.rect(matrices, x + 3f, drawerY + 2f, 0.6f, Math.max(0f, drawerHeight - 5f),
                ColorUtil.withAlpha(accent, (int) (110 * expandProgress)));

        float cursor = drawerY + 1f;
        for (SettingComponent<?> component : components) {
            float visibility = component.getVisibility();
            float height = component.getHeight();
            if (height < 0.5f) continue;
            component.setBounds(x + 2f, cursor, width - 2f);
            boolean partial = visibility < 0.999f;
            if (partial) Scissor.push(context, x, cursor, width, height);
            component.render(context, mouseX, mouseY, delta);
            if (partial) Scissor.pop(context);
            cursor += height;
        }
        Scissor.pop(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y, width, HEIGHT)) {
            if (button == 0) module.toggle();
            else if (button == 1) expanded = !expanded;
            else return false;
            return true;
        }
        if (!expanded || expand.getFloat() < 0.95f) return false;
        boolean handled = false;
        for (SettingComponent<?> component : components) {
            if (!component.isVisible()) continue;
            // Every component sees the click so focused key binders can lose focus.
            if (!handled && component.mouseClicked(mouseX, mouseY, button)) handled = true;
            else if (component instanceof KeyComponent key && key.isListening()) key.mouseClicked(mouseX, mouseY, -1);
        }
        return handled;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (SettingComponent<?> component : components) component.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!expanded) return false;
        for (SettingComponent<?> component : components) {
            if (component.isVisible() && component.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    public boolean isListening() {
        if (!expanded) return false;
        for (SettingComponent<?> component : components) {
            if (component instanceof KeyComponent key && key.isListening()) return true;
        }
        return false;
    }
}
