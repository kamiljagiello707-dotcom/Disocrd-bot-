package pl.mysticclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import pl.mysticclient.module.misc.ClientSettings;
import pl.mysticclient.setting.Setting;
import pl.mysticclient.util.NotificationManager;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private boolean showInArrayList = true;
    public boolean favorite;

    // animacje GUI / HUD
    public float toggleAnim;
    public float arrayAnim;

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

    public void setEnabled(boolean enabled) { setEnabled(enabled, true); }

    public void setEnabled(boolean enabled, boolean notify) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
        if (notify && showInArrayList) {
            ClientSettings cs = ModuleManager.get(ClientSettings.class);
            if (cs != null && cs.notifications.get() && cs.notifyOnToggle.get()) {
                NotificationManager.push(name, enabled ? "włączony" : "wyłączony", enabled);
            }
        }
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender2D(DrawContext ctx, float tickDelta) {}
    public void onRender3D(WorldRenderContext ctx) {}

    /** Tekst pokazywany obok nazwy na ArrayList (np. tryb). */
    public String getSuffix() { return ""; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return settings; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean showInArrayList() { return showInArrayList; }
    protected void hideFromArrayList() { this.showInArrayList = false; }

    protected boolean nullCheck() { return mc.player == null || mc.world == null; }
}
