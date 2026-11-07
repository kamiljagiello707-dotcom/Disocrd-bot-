package pl.mysticclient.module.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.util.FriendManager;
import pl.mysticclient.util.NotificationManager;

public class MiddleClick extends Module {
    public MiddleClick() {
        super("MiddleClick", "Środkowy przycisk myszy na graczu = dodaj/usuń znajomego.", Category.MISC);
    }

    /** Wywoływane z MouseMixin. */
    public void onMiddleClick() {
        if (nullCheck() || mc.currentScreen != null) return;
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity p) {
            String name = p.getGameProfile().getName();
            boolean added = FriendManager.toggle(name);
            NotificationManager.push("Znajomi", (added ? "Dodano " : "Usunięto ") + name, added);
            pl.mysticclient.config.ConfigManager.save();
        }
    }

    @Override
    public String getSuffix() { return String.valueOf(FriendManager.count()); }
}
