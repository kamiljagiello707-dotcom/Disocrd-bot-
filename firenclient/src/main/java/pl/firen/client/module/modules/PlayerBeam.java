package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Friends;

/** Pionowa smuga światła nad każdym graczem (łatwo znaleźć na mapie). */
public class PlayerBeam extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF9D7CFF));
    public final ColorSetting friendColor = add(new ColorSetting("Friend color", 0xFF3DDC97));
    public final NumberSetting height = add(new NumberSetting("Height", 14, 3, 40, 1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 0.35, 0.1, 1.0, 0.05));
    public final BooleanSetting fade = add(new BooleanSetting("Fade out", true));

    public PlayerBeam() { super("PlayerBeam", "Słup światła nad każdym graczem.", Category.RENDER); }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || ctx.matrixStack() == null || mc.world == null) return;
        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        var ms = ctx.matrixStack();
        Render3D.begin();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator()) continue;
            boolean friend = Friends.is(p.getName().getString());
            int base = (friend ? friendColor.get() : color.get()) & 0xFFFFFF;
            Vec3d pos = p.getLerpedPos(delta).subtract(cam);
            ms.push();
            ms.translate(pos.x, pos.y, pos.z);
            double r = radius.get(), h = height.get();
            int bottom = 0xA0000000 | base;
            int top = (fade.get() ? 0x00000000 : 0xA0000000) | base;
            // 4 ściany słupa jako wstęgi
            Render3D.ringBand(ms, r, (float) h, bottom, 5);
            // jaśniejszy rdzeń
            Render3D.ringBand(ms, r * 0.4, (float) h, (0xFFFFFF & base) | 0xC0000000, 5);
            ms.pop();
        }
        Render3D.end();
    }
}
