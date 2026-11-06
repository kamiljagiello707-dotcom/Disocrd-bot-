package pl.firen.client.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

/** Stawianie bloków: szuka podpory, robi "silent switch" slotu i opcjonalnie obraca pakietem. */
public final class BlockPlacer {
    private BlockPlacer() {}

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static boolean replaceable(BlockPos pos) {
        return mc().world.getBlockState(pos).isReplaceable();
    }

    /** Czy blok da się tu postawić (wolne miejsce, brak encji w środku). */
    public static boolean canPlace(BlockPos pos, Block block) {
        if (!replaceable(pos)) return false;
        BlockState state = block.getDefaultState();
        return mc().world.canPlace(state, pos, ShapeContext.absent());
    }

    /** Ściana sąsiedniego, stałego bloku, o którą można "oprzeć" nowy blok. */
    public static Direction supportSide(BlockPos pos) {
        for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
            if (!replaceable(pos.offset(d))) return d;
        }
        return null;
    }

    /** Slot paska szybkiego wyboru z blokiem spełniającym warunek (np. obsydian). */
    public static int findSlot(Predicate<Item> filter) {
        var inv = mc().player.getInventory();
        for (int i = 0; i < 9; i++) {
            var st = inv.getStack(i);
            if (!st.isEmpty() && st.getItem() instanceof BlockItem && filter.test(st.getItem())) return i;
        }
        return -1;
    }

    public static Block blockInSlot(int slot) {
        return ((BlockItem) mc().player.getInventory().getStack(slot).getItem()).getBlock();
    }

    /** Stawia blok. Zwraca true, gdy serwer przyjął interakcję. */
    public static boolean place(BlockPos pos, int slot, boolean rotate, boolean swing) {
        var p = mc().player;
        Direction side = supportSide(pos);
        if (side == null || slot < 0) return false;
        BlockPos neighbor = pos.offset(side);
        Direction face = side.getOpposite();
        Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(face.getVector()).multiply(0.5));
        if (p.getEyePos().squaredDistanceTo(hit) > 5.2 * 5.2) return false;

        if (rotate) {
            Vec3d d = hit.subtract(p.getEyePos());
            float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
            float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, MathHelper.clamp(pitch, -90, 90), p.isOnGround(), p.horizontalCollision));
        }
        var inv = p.getInventory();
        int prev = inv.selectedSlot;
        inv.selectedSlot = slot;
        var result = mc().interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hit, face, neighbor, false));
        inv.selectedSlot = prev;
        if (result.isAccepted()) {
            if (swing) p.swingHand(Hand.MAIN_HAND);
            else p.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return true;
        }
        return false;
    }
}
