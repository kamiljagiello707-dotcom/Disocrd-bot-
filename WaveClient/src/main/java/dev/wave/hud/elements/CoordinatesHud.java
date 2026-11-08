package dev.wave.hud.elements;

import dev.wave.hud.HudElement;
import dev.wave.setting.BooleanSetting;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.World;

import java.util.Locale;

public class CoordinatesHud extends HudElement {
    public final BooleanSetting decimals = new BooleanSetting("Decimals", false);
    public final BooleanSetting direction = new BooleanSetting("Direction", true);
    public final BooleanSetting otherDimension = new BooleanSetting("Nether Coords", "Show the matching Nether/Overworld position", true);

    public CoordinatesHud() {
        super("Coordinates", "Shows your position");
        addSettings(decimals, direction, otherDimension, background);
        setEnabledSilently(true);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { x = 4; y = sh - 22; }

    private String fmt(double d) { return decimals.isOn() ? String.format(Locale.ROOT, "%.1f", d) : String.valueOf((int) Math.floor(d)); }

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        double px = 0, py = 0, pz = 0;
        String dir = "North";
        boolean nether = false;
        if (mc.player != null) {
            px = mc.player.getX(); py = mc.player.getY(); pz = mc.player.getZ();
            String n = mc.player.getHorizontalFacing().asString();
            dir = Character.toUpperCase(n.charAt(0)) + n.substring(1);
            nether = mc.world != null && mc.world.getRegistryKey() == World.NETHER;
        }
        String line1 = "§fXYZ §7" + fmt(px) + " " + fmt(py) + " " + fmt(pz);
        if (direction.isOn()) line1 += " §8(§f" + dir + "§8)";
        String line2 = null;
        if (otherDimension.isOn()) {
            double f = nether ? 8 : 0.125;
            line2 = (nether ? "§fOverworld §7" : "§fNether §7") + fmt(px * f) + " " + fmt(py) + " " + fmt(pz * f);
        }
        float w = Math.max(RenderUtil.width(line1), line2 == null ? 0 : RenderUtil.width(line2)) + 10;
        float h = line2 == null ? 14 : 24;
        resize(w, h);
        if (background.isOn()) RenderUtil.round(c, x, y, w, h, 3, Theme.HUD_BG);
        boolean right = rightAligned();
        RenderUtil.text(c, line1, right ? x + w - 5 - RenderUtil.width(line1) : x + 5, y + 3, Theme.TEXT, true);
        if (line2 != null) RenderUtil.text(c, line2, right ? x + w - 5 - RenderUtil.width(line2) : x + 5, y + 13, Theme.TEXT, true);
    }
}
