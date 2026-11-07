package pl.mysticclient.setting;

public class BoolSetting extends Setting<Boolean> {
    public float anim;

    public BoolSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
        this.anim = defaultValue ? 1f : 0f;
    }

    public void toggle() { set(!get()); }
}
