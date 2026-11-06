package pl.mystic.client.module.modules;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

/** Podświetla graczy i potwory przez ściany (MinecraftClientMixin). */
public class Esp extends Module {
    public Esp() { super("ESP", "Obrys graczy i mobów przez ściany", Category.RENDER, GLFW.GLFW_KEY_Y); }

    public boolean shouldHighlight(Entity entity) {
        if (entity == mc.player) return false;
        return entity instanceof PlayerEntity || entity instanceof Monster;
    }
}
