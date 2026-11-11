package tech.fuzir.client.module.render;

import net.minecraft.util.math.Vec3d;
import tech.fuzir.client.event.events.Render3DEvent;
import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.render.WorldRenderUtil;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Easing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Spawns expanding, fading accent circles on the ground where you jump (and optionally land).
 */
public final class JumpCircles extends Module {
    private final NumberSetting radius = number("Radius", 1.2, 0.4, 3.0, 0.1, "m");
    private final NumberSetting duration = number("Duration", 900, 300, 2500, 50, "ms");
    private final ModeSetting style = mode("Style", "Glow", "Glow", "Ring");
    private final BoolSetting onLanding = bool("On Landing", true);

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround = true;

    private record Circle(Vec3d position, long start) {
    }

    public JumpCircles() {
        super("JumpCircles", "Expanding circles under your feet when you jump and land.", Category.RENDER);
        on(TickEvent.class, e -> tick());
        on(Render3DEvent.class, this::render);
    }

    @Override
    protected void onEnable() {
        circles.clear();
        wasOnGround = mc.player == null || mc.player.isOnGround();
    }

    private void tick() {
        boolean onGround = mc.player.isOnGround();
        boolean jumped = wasOnGround && !onGround && mc.player.getVelocity().y > 0.0;
        boolean landed = !wasOnGround && onGround;
        if (jumped || (landed && onLanding.isEnabled())) {
            circles.add(new Circle(mc.player.getPos().add(0.0, 0.02, 0.0), System.currentTimeMillis()));
        }
        wasOnGround = onGround;
    }

    private void render(Render3DEvent event) {
        long now = System.currentTimeMillis();
        double life = duration.get();
        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();
            double progress = (now - circle.start()) / life;
            if (progress >= 1.0) {
                iterator.remove();
                continue;
            }
            double grow = Easing.OUT_CUBIC.apply(progress);
            float alpha = (float) (1.0 - Easing.IN_OUT_CUBIC.apply(progress));
            double outer = radius.get() * grow;
            double thickness = style.is("Ring") ? 0.06 : outer * 0.45;
            double inner = Math.max(0.0, outer - thickness);
            float innerAlpha = style.is("Ring") ? 1f : 0f;
            WorldRenderUtil.flatRing(event.matrices(), event.camera(), circle.position(), inner, outer,
                    segment -> ColorUtil.withAlpha(Theme.accentWave(segment * 3200.0 / 72.0), (int) (210 * alpha)),
                    innerAlpha, true);
        }
    }
}
