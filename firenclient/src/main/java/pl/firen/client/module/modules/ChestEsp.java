package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

public class ChestEsp extends Module {
    public final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    public final BooleanSetting enderChests = add(new BooleanSetting("Ender chests", true));
    public final BooleanSetting shulkers = add(new BooleanSetting("Shulkers", true));
    public final BooleanSetting barrels = add(new BooleanSetting("Barrels", true));
    public final ColorSetting chestColor = add(new ColorSetting("Chest color", 0xFFFFB347));
    public final ColorSetting enderColor = add(new ColorSetting("Ender color", 0xFFB06BFF));
    public final ColorSetting shulkerColor = add(new ColorSetting("Shulker color", 0xFFFF5C8A));
    public final NumberSetting fill = add(new NumberSetting("Fill alpha", 45, 0, 255, 5));

    private record Found(Box box, int color) {}
    private final List<Found> found = new ArrayList<>();
    private int scanTicks;

    public ChestEsp() { super("ChestESP", "Podświetla skrzynie, shulkery i beczki.", Category.RENDER); }

    @Override public String getSuffix() { return String.valueOf(found.size()); }

    @Override
    public void onTick() {
        if (scanTicks-- > 0) return;
        scanTicks = 10;
        found.clear();
        ChunkPos center = mc.player.getChunkPos();
        int r = mc.options.getClampedViewDistance();
        for (int cx = center.x - r; cx <= center.x + r; cx++) {
            for (int cz = center.z - r; cz <= center.z + r; cz++) {
                if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                for (BlockEntity be : mc.world.getChunk(cx, cz).getBlockEntities().values()) {
                    int c = colorOf(be);
                    if (c == 0) continue;
                    found.add(new Found(new Box(be.getPos()).contract(0.06), c));
                }
            }
        }
    }

    private int colorOf(BlockEntity be) {
        if (be instanceof EnderChestBlockEntity) return enderChests.get() ? enderColor.get() : 0;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.get() ? shulkerColor.get() : 0;
        if (be instanceof ChestBlockEntity) return chests.get() ? chestColor.get() : 0;
        if (be instanceof BarrelBlockEntity) return barrels.get() ? chestColor.get() : 0;
        return 0;
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || ctx.matrixStack() == null || found.isEmpty()) return;
        Vec3d cam = ctx.camera().getPos().negate();
        Render3D.begin();
        for (Found f : found) {
            Box b = f.box.offset(cam);
            if (fill.get() > 0) Render3D.boxFill(ctx.matrixStack(), b, (fill.get().intValue() << 24) | (f.color & 0xFFFFFF));
            Render3D.boxLines(ctx.matrixStack(), b, 0xFF000000 | f.color, 1.5f);
        }
        Render3D.end();
    }
}
