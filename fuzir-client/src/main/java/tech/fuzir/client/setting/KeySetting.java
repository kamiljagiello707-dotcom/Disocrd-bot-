package tech.fuzir.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * GLFW key binding. {@link GLFW#GLFW_KEY_UNKNOWN} means unbound.
 */
public final class KeySetting extends Setting<Integer> {
    public static final int NONE = GLFW.GLFW_KEY_UNKNOWN;

    public KeySetting(String name, int defaultKey) {
        super(name, defaultKey);
    }

    public int getKey() {
        return value;
    }

    public boolean isBound() {
        return value != NONE;
    }

    public boolean matches(int key) {
        return isBound() && value == key;
    }

    public String getKeyName() {
        if (!isBound()) return "None";
        String name = InputUtil.Type.KEYSYM.createFromCode(value).getLocalizedText().getString();
        if (name.length() == 1) return name.toUpperCase(Locale.ROOT);
        return name;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) set(element.getAsInt());
    }
}
