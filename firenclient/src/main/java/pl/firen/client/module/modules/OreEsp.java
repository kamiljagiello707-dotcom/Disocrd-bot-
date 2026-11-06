package pl.firen.client.module.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Render3D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Podświetla rudy w okolicy (skanowanie po kawałku co tick, bez lagów). */
public class OreEsp extends Module {
    public final NumberSetting radius = add(new NumberSetting("Radius", 24, 8, 48, 4));
    public final BooleanSetting diamond = add(new BooleanSetting("Diamond", true));
    public final BooleanSetting debris = add(new BooleanSetting("Ancient debris", true));
    public final BooleanSetting emerald = add(new BooleanSetting("Emerald", true));
    public final BooleanSetting gold = add(new BooleanSetting("Gold", true));
    public final BooleanSetting iron = add(new BooleanSetting("Iron", false));
    public final BooleanSetting redstone = add(new BooleanSetting("Redstone", false));
    public final BooleanSetting lapis = add(new BooleanSetting("Lapis", false));
    public final BooleanSetting coal = add(new BooleanSetting("Coal", false));

    private record Ore(BlockPos pos, int color) {}
    private List<Ore> visible = new ArrayList<>();
    private final List<Ore> building = new ArrayList<>();
    private int sliceX = Integer.MIN_VALUE;
    private BlockPos origin;

    public OreEsp() { super("OreESP", "Pokazuje rudy przez ściany (diamenty, debris…).", Category.WORLD); }

    @Override public String getSuffix() { return String.valueOf(visible.size()); }

    @Override
    public void onDisable() {
        visible = new ArrayList<>();
        building.clear();
        sliceX = Integer.MIN_VALUE;
    }

    private Map<Block, Integer> wanted() {
        Map<Block, Integer> m = new HashMap<>();
        if (diamond.get()) { m.put(Blocks.DIAMOND_ORE, 0xFF4DE8FF); m.put(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF4DE8FF); }
        if (debris.get()) m.put(Blocks.ANCIENT_DEBRIS, 0xFFB5651D);
        if (emerald.get()) { m.put(Blocks.EMERALD_ORE, 0xFF3DDC97); m.put(Blocks.DEEPSLATE_EMERALD_ORE, 0xFF3DDC97); }
        if (gold.get()) { m.put(Blocks.GOLD_ORE, 0xFFFFD24D); m.put(Blocks.DEEPSLATE_GOLD_ORE, 0xFFFFD24D); m.put(Blocks.NETHER_GOLD_ORE, 0xFFFFD24D); }
        if (iron.get()) { m.put(Blocks.IRON_ORE, 0xFFE0B49A); m.put(Blocks.DEEPSLATE_IRON_ORE, 0xFFE0B49A); }
        if (redstone.get()) { m.put(Blocks.REDSTONE_ORE, 0xFFFF3B3B); m.put(Blocks.DEEPSLATE_REDSTONE_ORE, 0xFFFF3B3B); }
        if (lapis.get()) { m.put(Blocks.LAPIS_ORE, 0xFF3B6BFF); m.put(Blocks.DEEPSLATE_LAPIS_ORE, 0xFF3B6BFF); }
        if (coal.get()) { m.put(Blocks.COAL_ORE, 0xFF444444); m.put(Blocks.DEEPSLATE_COAL_ORE, 0xFF444444); }
        return m;
    }

    @Override
    public void onTick() {
        int r = radius.get().intValue();
        if (sliceX == Integer.MIN_VALUE) {
            origin = mc.player.getBlockPos();
            sliceX = -r;
            building.clear();
        }
        Map<Block, Integer> wanted = wanted();
        BlockPos.Mutable mp = new BlockPos.Mutable();
        int bottom = mc.world.getBottomY(), top = bottom + mc.world.getHeight();
        // 3 plasterki X na tick
        for (int s = 0; s < 3 && sliceX <= r; s++, sliceX++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -r; dy <= r; dy++) {
                    int y = origin.getY() + dy;
                    if (y < bottom || y >= top) continue;
                    mp.set(origin.getX() + sliceX, y, origin.getZ() + dz);
                    Integer c = wanted.get(mc.world.getBlockState(mp).getBlock());
                    if (c != null && building.size() < 3000) building.add(new Ore(mp.toImmutable(), c));
                }
            }
        }
        if (sliceX > r) {
            visible = new ArrayList<>(building);
            sliceX = Integer.MIN_VALUE;
        }
    }

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        if (!isEnabled() || ctx.matrixStack() == null || visible.isEmpty()) return;
        Vec3d cam = ctx.camera().getPos().negate();
        Render3D.begin();
        for (Ore o : visible) {
            Box b = new Box(o.pos).offset(cam);
            Render3D.boxFill(ctx.matrixStack(), b, 0x38000000 | (o.color & 0xFFFFFF));
            Render3D.boxLines(ctx.matrixStack(), b, o.color, 1.5f);
        }
        Render3D.end();
    }
}
