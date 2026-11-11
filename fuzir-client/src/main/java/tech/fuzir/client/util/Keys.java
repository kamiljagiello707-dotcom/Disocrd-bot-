package tech.fuzir.client.util;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class Keys {
    private Keys() {
    }

    /** Czy klawisz przypisany do akcji jest naprawdę wciśnięty na klawiaturze. */
    public static boolean physical(KeyBinding kb) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(kb);
        if (key.getCategory() != InputUtil.Type.KEYSYM || key.getCode() < 0) return false;
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), key.getCode());
    }

    /** Przywraca stan akcji do tego, co faktycznie trzyma gracz. */
    public static void restore(KeyBinding kb) {
        kb.setPressed(MinecraftClient.getInstance().currentScreen == null && physical(kb));
    }
}
