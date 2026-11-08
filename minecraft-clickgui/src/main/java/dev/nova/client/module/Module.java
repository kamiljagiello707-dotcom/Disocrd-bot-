package dev.nova.client.module;

import dev.nova.client.setting.KeybindSetting;
import dev.nova.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/** Base module. Override onEnable / onDisable / onTick with your own logic. */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    public final KeybindSetting bind = new KeybindSetting("Bind", -1);

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    public boolean isEnabled() { return enabled; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }

    /** Settings shown in the GUI (the bind is always last). */
    public List<Setting<?>> getSettings() {
        List<Setting<?>> all = new ArrayList<>(settings);
        all.add(bind);
        return all;
    }
}
