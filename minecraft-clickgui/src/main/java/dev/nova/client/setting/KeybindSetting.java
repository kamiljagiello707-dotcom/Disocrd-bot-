package dev.nova.client.setting;

import net.minecraft.client.util.InputUtil;

public class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String name, int key) { super(name, key); }

    public String display() {
        if (value < 0) return "None";
        return InputUtil.Type.KEYSYM.createFromCode(value).getLocalizedText().getString();
    }
}
