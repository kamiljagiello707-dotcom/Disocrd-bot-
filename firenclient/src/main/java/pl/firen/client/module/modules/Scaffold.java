package pl.firen.client.module.modules;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.rotation.RotationManager;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;

/** Stawia bloki pod nogami (ciche rotacje + move fix, tower, keep Y). */
public class Scaffold extends Module {
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate (silent)", true));
    public final BooleanSetting tower = add(new BooleanSetting("Tower", true));
    public final BooleanSetting keepY = add(new BooleanSetting("Keep Y", false));
    public final BooleanSetting swing = add(new BooleanSetting("Swing", true));
    public final BooleanSetting switchBack = add(new BooleanSetting("Switch back", true));
    public final NumberSetting extend = add(new NumberSetting("Extend", 0, 0, 3, 1));

    private BlockPos placeAt;
    private Direction placeSide;
    private Vec3d hitVec;
    private int startY, previousSlot = -1;

    public Scaffold() { super("Scaffold", "Automatycznie stawia bloki pod nogami.", Category.WORLD); }

    @Override
    public void onEnable() {
        if (mc.player != null) startY = mc.player.getBlockPos().getY() - 1;
    }

    @Override
    public void onDisable() {
        if (mc.player != null && switchBack.get() && previousSlot >= 0) mc.player.getInventory().selectedSlot = previousSlot;
        previousSlot = -1;
    }

    private int findBlockSlot() {
        var inv = mc.player.getInventory();
        if (inv.getStack(inv.selectedSlot).getItem() instanceof BlockItem) return inv.selectedSlot;
        for (int i = 0; i < 9; i++) if (inv.getStack(i).getItem() instanceof BlockItem && inv.getStack(i).getCount() > 0) return i;
        return -1;
    }

    private boolean replaceable(BlockPos pos) {
        BlockState s = mc.world.getBlockState(pos);
        return s.isReplaceable();
    }

    @Override
    public void onPreTick() {
        placeAt = null;
        if (findBlockSlot() < 0) return;
        int y = keepY.get() ? startY : mc.player.getBlockPos().getY() - 1;
        BlockPos base = BlockPos.ofFloored(mc.player.getX(), y, mc.player.getZ());
        // rozszerzenie w kierunku ruchu
        if (extend.get() > 0) {
            Vec3d v = mc.player.getVelocity();
            if (v.horizontalLengthSquared() > 1e-4) {
                Vec3d d = v.multiply(1, 0, 1).normalize().multiply(extend.get());
                base = BlockPos.ofFloored(mc.player.getX() + d.x, y, mc.player.getZ() + d.z);
            }
        }
        if (!replaceable(base)) return;
        if (!findNeighbor(base)) {
            // spróbuj o jeden blok obok (np. przy przejściu z krawędzi)
            for (Direction d : Direction.Type.HORIZONTAL) {
                if (replaceable(base.offset(d)) && findNeighbor(base.offset(d))) break;
            }
        }
        if (placeAt != null && rotate.get()) {
            Vec3d eye = mc.player.getEyePos();
            Vec3d diff = hitVec.subtract(eye);
            float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
            float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
            RotationManager.request(yaw, MathHelper.clamp(pitch, -90, 90), true, 40f);
        }
    }

    private boolean findNeighbor(BlockPos target) {
        for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos n = target.offset(d);
            if (replaceable(n)) continue;
            placeAt = n;
            placeSide = d.getOpposite();
            hitVec = Vec3d.ofCenter(n).add(Vec3d.of(placeSide.getVector()).multiply(0.5));
            return true;
        }
        return false;
    }

    @Override
    public void onTick() {
        if (placeAt == null || mc.interactionManager == null) return;
        int slot = findBlockSlot();
        if (slot < 0) return;
        var inv = mc.player.getInventory();
        if (inv.selectedSlot != slot) {
            if (previousSlot < 0) previousSlot = inv.selectedSlot;
            inv.selectedSlot = slot;
        }
        var result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitVec, placeSide, placeAt, false));
        if (result.isAccepted()) {
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.HandSwingC2SPacket(Hand.MAIN_HAND));
            if (tower.get() && mc.options.jumpKey.isPressed() && !mc.options.forwardKey.isPressed()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * 0.3, 0.42, v.z * 0.3);
            }
        }
    }
}
