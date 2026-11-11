package tech.fuzir.client.gui.component;

import tech.fuzir.client.setting.Setting;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

/**
 * Component bound to a {@link Setting}. Collapses smoothly when the setting's visibility condition fails.
 */
public abstract class SettingComponent<S extends Setting<?>> extends Component {
    protected static final float PADDING = 7f;

    protected final S setting;
    private final Animation visibility;

    protected SettingComponent(S setting) {
        this.setting = setting;
        this.visibility = new Animation(Easing.OUT_CUBIC, 180, setting.isVisible() ? 1 : 0);
    }

    /** Height when fully visible. */
    protected abstract float getBaseHeight();

    public float getVisibility() {
        return visibility.animateTo(setting.isVisible()).getFloat();
    }

    @Override
    public float getHeight() {
        return getBaseHeight() * getVisibility();
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }

    public S getSetting() {
        return setting;
    }
}
