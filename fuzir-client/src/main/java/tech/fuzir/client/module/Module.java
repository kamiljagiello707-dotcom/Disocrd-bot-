package tech.fuzir.client.module;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.hud.Notifications;
import tech.fuzir.client.setting.KeySetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.Setting;
import tech.fuzir.client.util.Anim;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public final char icon;
    public final KeySetting bind = new KeySetting("Klawisz", GLFW.GLFW_KEY_UNKNOWN);
    public final ModeSetting bindMode = new ModeSetting("Tryb klawisza", "Przełącz", "Przełącz", "Przytrzymaj");
    public final List<Setting<?>> settings = new ArrayList<>();

    // stan animacji w menu i na HUD
    public final Anim toggleAnim = new Anim(0);
    public final Anim hoverAnim = new Anim(0);
    public final Anim listAnim = new Anim(0);
    public final Anim bindAnim = new Anim(0);
    /** Czas ostatniego przełączenia (błysk w panelu KeyBinds). */
    public long toggledAt;

    private boolean enabled;

    protected Module(String name, String description, Category category, char icon) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
    }

    protected <T extends Setting<?>> T add(T s) {
        settings.add(s);
        return s;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        setEnabledQuiet(on);
        Notifications.module(this);
    }

    /** Ustawia stan bez powiadomienia (wczytywanie configu). */
    public void setEnabledQuiet(boolean on) {
        if (on == enabled) return;
        enabled = on;
        toggledAt = System.currentTimeMillis();
        if (on) onEnable();
        else onDisable();
    }

    /** Moduły ustawień (np. menu) nie mają przełącznika. */
    public boolean hasToggle() {
        return true;
    }

    /** Czy pokazywać moduł na liście włączonych modułów. */
    public boolean showInList() {
        return category != Category.HUD && category != Category.OTHER;
    }

    public boolean holdMode() {
        return bindMode.is("Przytrzymaj");
    }

    /** Dopisek na liście modułów, np. aktualny tryb. */
    public String suffix() {
        return null;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }
}
