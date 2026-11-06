package pl.firen.client.module.modules;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.BlockPlacer;

/** Surround: obsydian dookoła własnych nóg (ochrona przed kryształami). */
public class Surround extends Module {
    public final NumberSetting perTick = add(new NumberSetting("Blocks/tick", 4, 1, 8, 1));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    public final BooleanSetting center = add(new BooleanSetting("Center", true));
    public final BooleanSetting disableOnJump = add(new BooleanSetting("Disable on jump", true));
    private int startY;

    public Surround() { super("Surround", "Otacza Twoje nogi obsydianem.", Category.COMBAT); }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        startY = mc.player.getBlockPos().getY();
        if (center.get()) {
            BlockPos b = mc.player.getBlockPos();
            mc.player.setPosition(b.getX() + 0.5, mc.player.getY(), b.getZ() + 0.5);
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        }
    }

    @Override
    public void onTick() {
        if (disableOnJump.get() && mc.player.getBlockPos().getY() > startY) { setEnabled(false); return; }
        int slot = BlockPlacer.findSlot(i -> i == Blocks.OBSIDIAN.asItem() || i == Blocks.CRYING_OBSIDIAN.asItem());
        if (slot < 0) return;
        BlockPos feet = mc.player.getBlockPos();
        int placed = 0;
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos p = feet.offset(d);
            if (placed >= perTick.get()) break;
            if (BlockPlacer.replaceable(p.down()) && BlockPlacer.canPlace(p.down(), Blocks.OBSIDIAN)
                    && BlockPlacer.place(p.down(), slot, rotate.get(), true)) placed++;
            if (BlockPlacer.canPlace(p, Blocks.OBSIDIAN) && BlockPlacer.place(p, slot, rotate.get(), true)) placed++;
        }
    }
}
