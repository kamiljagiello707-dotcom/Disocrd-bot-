package pl.firen.client.module;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private boolean enabled;

    // Stan animacji GUI / HUD
    public float toggleAnim, hoverAnim, expandAnim, arrayAnim;
    public boolean expanded;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) {
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
    /** Wywoływane na początku ticka, przed wysłaniem pakietu ruchu (rotacje). */
    public void onPreTick() {}
    public void onRenderWorld(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {}

    /** Dodatkowa informacja wyświetlana w liście modułów na HUD (np. tryb). */
    public String getSuffix() { return null; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return settings; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
}
