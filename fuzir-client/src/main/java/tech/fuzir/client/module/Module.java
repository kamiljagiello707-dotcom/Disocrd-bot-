package tech.fuzir.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.event.EventBus;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.KeySetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for every toggleable feature. Event listeners declared through {@link #on(Class, Consumer)}
 * are attached to the bus only while the module is enabled.
 */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<EventBus.Listener<?>> listeners = new ArrayList<>();
    private final KeySetting bind;
    private boolean enabled;
    private boolean enabledByDefault;
    private boolean drawn = true;

    protected Module(String name, String description, Category category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.bind = new KeySetting("Bind", defaultKey);
    }

    protected Module(String name, String description, Category category) {
        this(name, description, category, KeySetting.NONE);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean state) {
        if (state == enabled) return;
        enabled = state;
        EventBus bus = FuzirClient.get().getEventBus();
        if (state) {
            listeners.forEach(bus::register);
            onEnable();
        } else {
            listeners.forEach(bus::unregister);
            onDisable();
        }
    }

    protected final <T> void on(Class<T> type, Consumer<T> handler) {
        EventBus.Listener<T> listener = new EventBus.Listener<>(type, handler);
        listeners.add(listener);
        if (enabled) FuzirClient.get().getEventBus().register(listener);
    }

    protected final <S extends Setting<?>> S register(S setting) {
        settings.add(setting);
        return setting;
    }

    protected final BoolSetting bool(String name, boolean defaultValue) {
        return register(new BoolSetting(name, defaultValue));
    }

    protected final NumberSetting number(String name, double defaultValue, double min, double max, double step) {
        return register(new NumberSetting(name, defaultValue, min, max, step));
    }

    protected final NumberSetting number(String name, double defaultValue, double min, double max, double step, String unit) {
        return register(new NumberSetting(name, defaultValue, min, max, step, unit));
    }

    protected final ModeSetting mode(String name, String defaultValue, String... modes) {
        return register(new ModeSetting(name, defaultValue, modes));
    }

    protected final void setEnabledByDefault() {
        this.enabledByDefault = true;
    }

    /** Hides the module from the HUD module list (it still appears in the ClickGUI). */
    protected final void setDrawn(boolean drawn) {
        this.drawn = drawn;
    }

    /** Short state shown next to the name in the module list, e.g. the active mode. */
    public String getSuffix() {
        return null;
    }

    protected static boolean inGame() {
        return mc.player != null && mc.world != null;
    }

    public JsonObject save() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        object.add("bind", bind.toJson());
        JsonObject values = new JsonObject();
        for (Setting<?> setting : settings) values.add(setting.getName(), setting.toJson());
        object.add("settings", values);
        return object;
    }

    public void load(JsonObject object) {
        if (object.has("bind")) bind.fromJson(object.get("bind"));
        if (object.has("settings") && object.get("settings").isJsonObject()) {
            JsonObject values = object.getAsJsonObject("settings");
            for (Setting<?> setting : settings) {
                JsonElement element = values.get(setting.getName());
                if (element != null) setting.fromJson(element);
            }
        }
        if (object.has("enabled")) setEnabled(object.get("enabled").getAsBoolean());
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public KeySetting getBind() {
        return bind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public boolean isDrawn() {
        return drawn;
    }
}
