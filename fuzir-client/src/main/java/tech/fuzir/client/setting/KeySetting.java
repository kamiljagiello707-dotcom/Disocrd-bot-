package tech.fuzir.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class KeySetting extends Setting<Integer> {
    public KeySetting(String name, int key) {
        super(name, key);
    }

    public boolean isSet() {
        return value != GLFW.GLFW_KEY_UNKNOWN;
    }

    public String keyName() {
        if (!isSet()) return "Brak";
        String n = InputUtil.Type.KEYSYM.createFromCode(value).getLocalizedText().getString();
        return n.length() <= 2 ? n.toUpperCase(Locale.ROOT) : n;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        if (e.isJsonPrimitive()) value = e.getAsInt();
    }
}
