package tech.fuzir.client.module.impl;

import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;

import java.util.ArrayList;
import java.util.List;

public class Coordinates extends HudModule {
    public final BoolSetting nether = add(new BoolSetting("Przelicznik Netheru", true));
    public final BoolSetting direction = add(new BoolSetting("Kierunek", true));
    private static final float ROW = 15, PAD = 7;

    public Coordinates() {
        super("Koordynaty", "Pozycja XYZ i kierunek patrzenia", Icons.MAP_PIN, 0f, 1f);
        setEnabledQuiet(true);
    }

    private record Row(char icon, String label, String value) {
    }

    private static String f(double v) {
        return String.valueOf((int) Math.floor(v));
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (mc.player == null) return rows;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        rows.add(new Row(Icons.MAP_PIN, "XYZ", f(x) + "  " + f(y) + "  " + f(z)));
        if (nether.on() && mc.world != null) {
            boolean inNether = mc.world.getRegistryKey() == World.NETHER;
            double k = inNether ? 8 : 1 / 8.0;
            rows.add(new Row(Icons.LAYERS, inNether ? "Świat" : "Nether", f(x * k) + "  " + f(z * k)));
        }
        if (direction.on()) {
            Direction d = mc.player.getHorizontalFacing();
            String name = switch (d) {
                case NORTH -> "Północ  -Z";
                case SOUTH -> "Południe  +Z";
                case EAST -> "Wschód  +X";
                case WEST -> "Zachód  -X";
                default -> d.asString();
            };
            rows.add(new Row(Icons.COMPASS, "Kierunek", name));
        }
        return rows;
    }

    @Override
    public float[] size(Canvas cv) {
        List<Row> rows = rows();
        if (rows.isEmpty()) return null;
        float lw = 0, vw = 0;
        for (Row r : rows) {
            lw = Math.max(lw, cv.width(r.label, F.REGULAR));
            vw = Math.max(vw, cv.width(r.value, F.REGULAR));
        }
        return new float[]{PAD + 14 + lw + 12 + vw + PAD, rows.size() * ROW + 8};
    }

    @Override
    public void draw(Canvas cv, float x, float y, float w, float h, boolean right) {
        Hud.panel(cv, x, y, w, h, 8);
        float lw = 0;
        List<Row> rows = rows();
        for (Row r : rows) lw = Math.max(lw, cv.width(r.label, F.REGULAR));
        float ry = y + 4;
        for (Row r : rows) {
            float cy = ry + ROW / 2;
            cv.icon(r.icon, x + PAD + 4, cy, Hud.MUTED, F.ICON_SM);
            cv.textMid(r.label, x + PAD + 14, cy, Hud.MUTED, F.REGULAR);
            cv.textMid(r.value, x + PAD + 14 + lw + 12, cy, Hud.TEXT, F.REGULAR);
            ry += ROW;
        }
    }
}
