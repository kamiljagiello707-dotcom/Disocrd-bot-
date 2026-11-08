package dev.wave.module;

import dev.wave.WaveClient;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.setting.Setting;
import dev.wave.util.Animation;
import dev.wave.util.KeyUtil;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int key = KeyUtil.NONE;

    /** Built-in settings shown at the bottom of every module. */
    public final ModeSetting bindMode = new ModeSetting("Bind Mode", "Toggle: press to switch. Hold: active while held.", "Toggle", "Toggle", "Hold");
    public final BooleanSetting drawn = new BooleanSetting("Show in list", "Show this module in the ArrayList HUD", true);

    /** Used by the ArrayList HUD for slide in/out. */
    public final Animation listAnimation = new Animation(12, 0);

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected void addSettings(Setting<?>... s) { settings.addAll(Arrays.asList(s)); }

    public List<Setting<?>> getSettings() { return settings; }

    public List<Setting<?>> getAllSettings() {
        List<Setting<?>> all = new ArrayList<>(settings);
        all.add(bindMode);
        all.add(drawn);
        return all;
    }

    public String getName() { return name; }
    public String getIcon() { return dev.wave.util.Icons.key(name); }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    /** Extra info displayed next to the name in the ArrayList. */
    public String getSuffix() { return null; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean state) { setEnabled(state, true); }

    public void setEnabled(boolean state, boolean notify) {
        if (state == enabled) return;
        enabled = state;
        if (state) onEnable(); else onDisable();
        if (notify && WaveClient.INSTANCE != null) WaveClient.INSTANCE.onModuleToggle(this);
    }

    /** Silent state change used when loading the config. */
    public void setEnabledSilently(boolean state) { setEnabled(state, false); }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    protected boolean nullCheck() { return mc.player == null || mc.world == null; }
}
