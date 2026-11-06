package pl.firen.client.module.modules;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.util.Friends;

/** Kliknięcie środkowym przyciskiem na gracza dodaje/usuwa go ze znajomych. */
public class MiddleClickFriend extends Module {
    private boolean wasDown;

    public MiddleClickFriend() {
        super("MiddleClick", "ŚPM na graczu = dodaj/usuń znajomego.", Category.MISC);
        setEnabled(true);
    }

    @Override public String getSuffix() { return String.valueOf(Friends.all().size()); }

    @Override
    public void onTick() {
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (down && !wasDown && mc.currentScreen == null && mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity p) {
            String name = p.getName().getString();
            boolean added = Friends.toggle(name);
            mc.player.sendMessage(Text.literal("[FirenClient] ").formatted(Formatting.GRAY)
                    .append(Text.literal(name).formatted(Formatting.WHITE))
                    .append(Text.literal(added ? " dodany do znajomych" : " usunięty ze znajomych").formatted(added ? Formatting.GREEN : Formatting.RED)), false);
            pl.firen.client.FirenClient.config().saveClient();
        }
        wasDown = down;
    }
}
