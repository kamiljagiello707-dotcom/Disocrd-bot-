package dev.mustic.client.module;

import dev.mustic.client.setting.Setting;
import dev.mustic.client.util.Anim;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private int key;
    private boolean enabled;
    private boolean visibleInArrayList = true;

    /** Animacja wjazdu na ArrayList w HUD. */
    public final Anim arrayAnim = new Anim(12f, 0f);

    protected Module(String name, String description, Category category) {
        this(name, description, category, GLFW.GLFW_KEY_UNKNOWN);
    }

    protected Module(String name, String description, Category category, int key) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = key;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    protected void hideFromArrayList() {
        this.visibleInArrayList = false;
    }

    protected boolean nullCheck() {
        return mc.player == null || mc.world == null;
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender2D(DrawContext context, float tickDelta) {}

    /** Dodatkowy tekst obok nazwy na ArrayList (np. aktualny tryb). */
    public String getSuffix() { return null; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return settings; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean isEnabled() { return enabled; }
    public boolean isVisibleInArrayList() { return visibleInArrayList; }
}
