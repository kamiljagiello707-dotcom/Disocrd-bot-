package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.OutlineManager;
import pl.firen.client.render.Project;
import pl.firen.client.render.Render2D;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Targets;

public class Esp extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Box", "Box", "2D", "Glow"));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final ColorSetting playerColor = add(new ColorSetting("Players color", 0xFFFFFFFF));
    public final ColorSetting mobColor = add(new ColorSetting("Mobs color", 0xFFFF4D5E));
    public final ColorSetting animalColor = add(new ColorSetting("Animals color", 0xFF3DDC97));
    public final NumberSetting fill = add(new NumberSetting("Fill alpha", 40, 0, 255, 5));
    public final NumberSetting width = add(new NumberSetting("Line width", 2.0, 1.0, 5.0, 0.5));
    public final BooleanSetting healthBar = add(new BooleanSetting("Health bar", true));

    public Esp() {
        super("ESP", "Pokazuje graczy i moby przez ściany (Box / 2D / Glow).", Category.RENDER);
        fill.visibleWhen(() -> mode.is("Box"));
        width.visibleWhen(() -> mode.is("Box"));
        healthBar.visibleWhen(() -> mode.is("2D"));
    }

    @Override public String getSuffix() { return mode.get(); }

    private int colorOf(Entity e) {
        if (e instanceof PlayerEntity) return playerColor.get();
        return Targets.matches(e, false, true, false) ? mobColor.get() : animalColor.get();
    }

    private boolean valid(Entity e) {
        return Targets.matches(e, players.get(), mobs.get(), animals.get());
    }

    @Override
    public void onTick() {
        if (!mode.is("Glow")) return;
        for (Entity e : mc.world.getEntities()) if (valid(e)) OutlineManager.add(e.getId(), colorOf(e));
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || !mode.is("Box") || ctx.matrixStack() == null) return;
        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        Render3D.begin();
        for (Entity e : mc.world.getEntities()) {
            if (!valid(e)) continue;
            Vec3d lerp = e.getLerpedPos(delta);
            Box box = e.getBoundingBox().offset(lerp.subtract(e.getPos())).offset(cam.negate());
            int c = colorOf(e);
            if (fill.get() > 0) Render3D.boxFill(ctx.matrixStack(), box, (fill.get().intValue() << 24) | (c & 0xFFFFFF));
            Render3D.boxLines(ctx.matrixStack(), box, 0xFF000000 | c, width.get().floatValue());
        }
        Render3D.end();
    }

    @Override
    public void onRenderHud(DrawContext ctx, float s) {
        if (!isEnabled() || !mode.is("2D")) return;
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        for (Entity e : mc.world.getEntities()) {
            if (!valid(e)) continue;
            Vec3d lerp = e.getLerpedPos(delta);
            Box b = e.getBoundingBox().offset(lerp.subtract(e.getPos()));
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean ok = true;
            for (int i = 0; i < 8; i++) {
                float[] p = Project.toScreen(new Vec3d((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ));
                if (p == null) { ok = false; break; }
                minX = Math.min(minX, p[0] / s); minY = Math.min(minY, p[1] / s);
                maxX = Math.max(maxX, p[0] / s); maxY = Math.max(maxY, p[1] / s);
            }
            if (!ok) continue;
            int c = 0xFF000000 | colorOf(e);
            float w = maxX - minX, h = maxY - minY;
            Render2D.outline(ctx, minX - 0.5f, minY - 0.5f, w + 1, h + 1, 1.5f, 1.5f, 0x90000000);
            Render2D.outline(ctx, minX, minY, w, h, 1.5f, 0.75f, c);
            if (healthBar.get() && e instanceof LivingEntity l) {
                float pct = Math.max(0, Math.min(1, l.getHealth() / Math.max(1, l.getMaxHealth())));
                float bx = minX - 4.5f;
                Render2D.round(ctx, bx - 0.5f, minY - 0.5f, 2.5f, h + 1, 1.2f, 0xB0000000);
                int hc = pct > 0.5f ? 0xFF3DDC97 : pct > 0.25f ? 0xFFFFC34D : 0xFFFF4D5E;
                Render2D.round(ctx, bx, minY + h * (1 - pct), 1.5f, h * pct, 0.75f, hc);
            }
        }
    }
}
