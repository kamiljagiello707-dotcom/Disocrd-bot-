package dev.wave.util;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Keys are GLFW key codes; mouse buttons are encoded as {@code MOUSE_OFFSET - button}. */
public final class KeyUtil {
    private KeyUtil() {}

    public static final int NONE = GLFW.GLFW_KEY_UNKNOWN;
    public static final int MOUSE_OFFSET = -100;

    public static int mouse(int button) { return MOUSE_OFFSET - button; }

    public static boolean isMouse(int key) { return key <= MOUSE_OFFSET; }

    public static String name(int key) {
        if (key == NONE) return "None";
        if (isMouse(key)) return "MB" + (MOUSE_OFFSET - key + 1);
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RShift";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LShift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCtrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RAlt";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LAlt";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PgDn";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps";
            case GLFW.GLFW_KEY_SPACE: return "Space";
            default: break;
        }
        try {
            String s = InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
            if (s.length() == 1) s = s.toUpperCase();
            return s;
        } catch (Exception e) {
            return "Key" + key;
        }
    }
}
