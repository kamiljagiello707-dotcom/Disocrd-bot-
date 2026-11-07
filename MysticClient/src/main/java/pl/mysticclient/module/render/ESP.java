package pl.mysticclient.module.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.ModeSetting;
import pl.mysticclient.util.FriendManager;
import pl.mysticclient.util.Render3D;
import pl.mysticclient.util.UI;

public class ESP extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Box", "Box", "Glow"));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting monsters = add(new BoolSetting("Monsters", true));
    private final BoolSetting animals = add(new BoolSetting("Animals", false));
    private final BoolSetting items = add(new BoolSetting("Items", false));
    private final BoolSetting fill = add(new BoolSetting("Fill", true)).visibleWhen(() -> mode.is("Box"));
    private final pl.mysticclient.setting.NumberSetting range = add(new pl.mysticclient.setting.NumberSetting("Zasięg", 64, 8, 256, 8));

    public ESP() {
        super("ESP", "Podświetla graczy i moby przez ściany.", Category.RENDER);
    }

    public boolean shouldRender(Entity e) {
        if (mc.player == null || e == mc.player) return false;
        if (e.squaredDistanceTo(mc.player) > range.get() * range.get()) return false;
        if (e instanceof PlayerEntity) return players.get() && !e.isSpectator();
        if (e instanceof Monster) return monsters.get();
        if (e instanceof AnimalEntity) return animals.get();
        if (e instanceof ItemEntity) return items.get();
        return false;
    }

    public int colorFor(Entity e) {
        if (e instanceof PlayerEntity p) return FriendManager.isFriend(p.getGameProfile().getName()) ? 0xFF4ADE80 : 0xFFFFFFFF;
        if (e instanceof Monster) return 0xFFF87171;
        if (e instanceof AnimalEntity) return 0xFF6FE0FF;
        return 0xFFFFD36F;
    }

    @Override
    public void onRender3D(WorldRenderContext ctx) {
        if (nullCheck() || !mode.is("Box")) return;
        float td = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = Render3D.matrix(ctx);

        if (fill.get()) {
            BufferBuilder q = Render3D.beginQuads();
            for (Entity e : mc.world.getEntities()) {
                if (!shouldRender(e)) continue;
                Render3D.boxFill(q, m, cam, box(e, td), UI.withAlpha(colorFor(e), 40));
            }
            Render3D.draw(q);
        }
        BufferBuilder b = Render3D.beginLines();
        for (Entity e : mc.world.getEntities()) {
            if (!shouldRender(e)) continue;
            Render3D.boxLines(b, m, cam, box(e, td), colorFor(e));
        }
        Render3D.draw(b);
    }

    static Box box(Entity e, float td) {
        Vec3d lerp = e.getLerpedPos(td);
        return e.getBoundingBox().offset(lerp.subtract(e.getPos())).expand(0.05);
    }

    @Override
    public String getSuffix() { return mode.get(); }
}
