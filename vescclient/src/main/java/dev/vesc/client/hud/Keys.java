package dev.vesc.client.hud;

import dev.vesc.client.lang.Lang;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}
    public static String name(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return Lang.get("bind.none");
        String n = InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
        return n.length() == 1 ? n.toUpperCase() : n;
    }
}
