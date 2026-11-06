package pl.firen.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Wspólne filtry encji dla modułów. */
public final class Targets {
    private Targets() {}

    public static boolean isPlayer(Entity e) {
        return e instanceof PlayerEntity && e != MinecraftClient.getInstance().player;
    }

    public static boolean matches(Entity e, boolean players, boolean mobs, boolean animals) {
        if (e == MinecraftClient.getInstance().player || !(e instanceof LivingEntity l) || !l.isAlive()) return false;
        if (e instanceof PlayerEntity) return players;
        if (e instanceof Monster) return mobs;
        if (e instanceof AnimalEntity) return animals;
        return false;
    }

    /** Kolor od czerwonego (blisko) do zielonego (daleko). */
    public static int distanceColor(double dist, int alpha) {
        float t = (float) Math.max(0, Math.min(1, dist / 40.0));
        int r = (int) (255 * (1 - t)), g = (int) (90 + 165 * t);
        return (alpha & 255) << 24 | r << 16 | g << 8 | 70;
    }
}
