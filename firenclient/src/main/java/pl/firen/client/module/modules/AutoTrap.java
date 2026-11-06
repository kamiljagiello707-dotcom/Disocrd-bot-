package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.BlockPlacer;
import pl.firen.client.util.Friends;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AutoTrap: zamyka najbliższego gracza w klatce z obsydianu.
 * Pierścień przy nogach → pierścień przy głowie → sufit; stawia podpory, gdy trzeba.
 */
public class AutoTrap extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Full", "Full", "Top", "Face"));
    public final ModeSetting blocks = add(new ModeSetting("Blocks", "Obsidian", "Obsidian", "Any"));
    public final NumberSetting range = add(new NumberSetting("Range", 4.5, 2.0, 6.0, 0.1));
    public final NumberSetting perTick = add(new NumberSetting("Blocks/tick", 2, 1, 8, 1));
    public final NumberSetting delay = add(new NumberSetting("Delay ticks", 0, 0, 10, 1));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    public final BooleanSetting swing = add(new BooleanSetting("Swing", true));
    public final BooleanSetting antiStep = add(new BooleanSetting("Anti step", true));
    public final BooleanSetting autoDisable = add(new BooleanSetting("Disable when done", false));
    public final BooleanSetting render = add(new BooleanSetting("Render", true));

    private PlayerEntity target;
    private List<BlockPos> plan = new ArrayList<>();
    private int wait, placedTotal;
    private long doneSince;

    public AutoTrap() { super("AutoTrap", "Zamyka najbliższego gracza w obsydianie.", Category.COMBAT); }

    @Override public String getSuffix() { return target == null ? null : target.getName().getString(); }

    public PlayerEntity getTarget() { return target; }

    @Override
    public void onDisable() {
        target = null;
        plan = new ArrayList<>();
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || Friends.is(p.getName().getString())) continue;
            double d = mc.player.distanceTo(p);
            if (d <= range.get() + 1.5 && d < bestDist) { best = p; bestDist = d; }
        }
        return best;
    }

    /** Pozycje klatki dla gracza (uwzględnia stanie na krawędzi = zajmowanie 2–4 kolumn). */
    private List<BlockPos> buildPlan(PlayerEntity t) {
        Box bb = t.getBoundingBox();
        int y = MathHelper.floor(bb.minY + 0.2);
        Set<Long> cols = new LinkedHashSet<>();
        for (int x = MathHelper.floor(bb.minX + 0.001); x <= MathHelper.floor(bb.maxX - 0.001); x++)
            for (int z = MathHelper.floor(bb.minZ + 0.001); z <= MathHelper.floor(bb.maxZ - 0.001); z++)
                cols.add(BlockPos.asLong(x, 0, z));

        List<BlockPos> ring = new ArrayList<>();
        for (long c : cols) {
            int x = BlockPos.unpackLongX(c), z = BlockPos.unpackLongZ(c);
            for (Direction d : Direction.Type.HORIZONTAL) {
                long n = BlockPos.asLong(x + d.getOffsetX(), 0, z + d.getOffsetZ());
                if (cols.contains(n)) continue;
                BlockPos p = new BlockPos(x + d.getOffsetX(), y, z + d.getOffsetZ());
                if (!ring.contains(p)) ring.add(p);
            }
        }

        List<BlockPos> out = new ArrayList<>();
        boolean feet = !mode.is("Top");
        // 1) podpory pod pierścieniem nóg (gdy gracz stoi nad dziurą / na krawędzi)
        if (feet) for (BlockPos p : ring) if (BlockPlacer.replaceable(p.down())) out.add(p.down());
        // 2) pierścień przy nogach
        if (feet) out.addAll(ring);
        // 3) pierścień przy głowie
        for (BlockPos p : ring) out.add(p.up());
        // 4) sufit: najpierw blok(i) na wysokości +2 obok (podpora / anti step), potem nad głową
        if (!mode.is("Face")) {
            if (antiStep.get()) for (BlockPos p : ring) out.add(p.up(2));
            else if (!ring.isEmpty()) out.add(ring.get(0).up(2));
            for (long c : cols) out.add(new BlockPos(BlockPos.unpackLongX(c), y + 2, BlockPos.unpackLongZ(c)));
        }
        return out;
    }

    @Override
    public void onTick() {
        if (mc.interactionManager == null) return;
        target = findTarget();
        if (target == null) { plan = new ArrayList<>(); return; }
        plan = buildPlan(target);

        int slot = BlockPlacer.findSlot(item -> blocks.is("Any") || item == Blocks.OBSIDIAN.asItem() || item == Blocks.CRYING_OBSIDIAN.asItem());
        if (slot < 0) return;
        Block block = BlockPlacer.blockInSlot(slot);

        if (wait-- > 0) return;
        int placed = 0;
        boolean missing = false;
        for (BlockPos pos : plan) {
            if (!BlockPlacer.replaceable(pos)) continue;
            missing = true;
            if (placed >= perTick.get()) break;
            if (!BlockPlacer.canPlace(pos, block)) continue;
            if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > range.get() + 0.5) continue;
            if (BlockPlacer.place(pos, slot, rotate.get(), swing.get())) {
                placed++;
                placedTotal++;
            }
        }
        if (placed > 0) wait = delay.get().intValue();
        if (!missing) {
            if (doneSince == 0) doneSince = System.currentTimeMillis();
            if (autoDisable.get() && System.currentTimeMillis() - doneSince > 300) setEnabled(false);
        } else {
            doneSince = 0;
        }
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || !render.get() || plan.isEmpty() || ctx.matrixStack() == null) return;
        Vec3d cam = ctx.camera().getPos().negate();
        int accent = Theme.accent() & 0xFFFFFF;
        Render3D.begin();
        for (BlockPos p : plan) {
            boolean done = !BlockPlacer.replaceable(p);
            Box b = new Box(p).contract(0.02).offset(cam);
            if (!done) Render3D.boxFill(ctx.matrixStack(), b, 0x30000000 | accent);
            Render3D.boxLines(ctx.matrixStack(), b, (done ? 0x55000000 : 0xE0000000) | accent, 1.5f);
        }
        Render3D.end();
    }
}
