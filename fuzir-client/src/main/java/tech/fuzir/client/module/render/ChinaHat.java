package tech.fuzir.client.module.render;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.fuzir.client.event.events.Render3DEvent;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.render.WorldRenderUtil;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.util.ColorUtil;

/**
 * Translucent gradient cone hat floating above the head, visible in third person.
 */
public final class ChinaHat extends Module {
    private final NumberSetting radius = number("Radius", 0.6, 0.3, 1.2, 0.05, "m");
    private final NumberSetting height = number("Height", 0.3, 0.1, 0.8, 0.05, "m");
    private final NumberSetting opacity = number("Opacity", 0.55, 0.1, 1.0, 0.05);
    private final BoolSetting firstPerson = bool("First Person", false);
    private final BoolSetting others = bool("Other Players", false);

    public ChinaHat() {
        super("ChinaHat", "A gradient cone hat above your head.", Category.RENDER);
        on(Render3DEvent.class, this::render);
    }

    private void render(Render3DEvent event) {
        boolean thirdPerson = !mc.options.getPerspective().isFirstPerson();
        if (thirdPerson || firstPerson.isEnabled()) draw(event, mc.player);
        if (!others.isEnabled()) return;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !player.isInvisible()) draw(event, player);
        }
    }

    private void draw(Render3DEvent event, PlayerEntity player) {
        float delta = event.tickDelta();
        double x = MathHelper.lerp(delta, player.prevX, player.getX());
        double y = MathHelper.lerp(delta, player.prevY, player.getY());
        double z = MathHelper.lerp(delta, player.prevZ, player.getZ());
        // Sit just above the head, lower while sneaking because the hitbox shrinks.
        Vec3d base = new Vec3d(x, y + player.getHeight() + 0.08, z);
        int alpha = (int) (255 * opacity.get());
        int apex = ColorUtil.withAlpha(Theme.accentSecondary(), Math.min(255, (int) (alpha * 1.2)));
        WorldRenderUtil.cone(event.matrices(), event.camera(), base, radius.get(), height.get(),
                segment -> ColorUtil.withAlpha(Theme.accentWave(segment * 3200.0 / 72.0), alpha), apex);
    }
}
