package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ColorSetting;
import dev.wave.util.Render3D;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public class StorageESP extends Module {
    public final BooleanSetting chests = new BooleanSetting("Chests", true);
    public final BooleanSetting enderChests = new BooleanSetting("Ender Chests", true);
    public final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    public final BooleanSetting barrels = new BooleanSetting("Barrels", true);
    public final ColorSetting chestColor = new ColorSetting("Chest Color", 0xFFFFC040).visible(chests::isOn);
    public final ColorSetting enderColor = new ColorSetting("Ender Color", 0xFFB040FF).visible(enderChests::isOn);
    public final ColorSetting shulkerColor = new ColorSetting("Shulker Color", 0xFFFF60C0).visible(shulkers::isOn);
    public final ColorSetting barrelColor = new ColorSetting("Barrel Color", 0xFFC08040).visible(barrels::isOn);

    public StorageESP() {
        super("StorageESP", "Outlines chests, shulkers and other storage", Category.VISUALS);
        addSettings(chests, enderChests, shulkers, barrels, chestColor, enderColor, shulkerColor, barrelColor);
    }

    public void render(WorldRenderContext ctx) {
        if (nullCheck()) return;
        Vec3d cam = ctx.camera().getPos();
        List<Render3D.Line> lines = new ArrayList<>();
        int radius = mc.options.getClampedViewDistance();
        ChunkPos center = mc.player.getChunkPos();
        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                WorldChunk chunk = mc.world.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int color = colorFor(be);
                    if (color == 0) continue;
                    Box box = new Box(be.getPos()).contract(0.06).offset(-cam.x, -cam.y, -cam.z);
                    Render3D.boxLines(box, color, lines);
                }
            }
        }
        Render3D.drawLines(ctx, lines);
    }

    private int colorFor(BlockEntity be) {
        if (be instanceof ChestBlockEntity && chests.isOn()) return chestColor.getColor();
        if (be instanceof EnderChestBlockEntity && enderChests.isOn()) return enderColor.getColor();
        if (be instanceof ShulkerBoxBlockEntity && shulkers.isOn()) return shulkerColor.getColor();
        if (be instanceof BarrelBlockEntity && barrels.isOn()) return barrelColor.getColor();
        return 0;
    }
}
