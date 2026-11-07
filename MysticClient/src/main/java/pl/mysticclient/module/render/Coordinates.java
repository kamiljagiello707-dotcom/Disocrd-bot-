package pl.mysticclient.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;
import pl.mysticclient.util.UI.Weight;

public class Coordinates extends Module {
    private final BoolSetting nether = add(new BoolSetting("Koordynaty Netheru", true));

    public Coordinates() {
        super("Coordinates", "Pokazuje koordynaty i kierunek.", Category.RENDER);
        hideFromArrayList();
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (nullCheck() || mc.getDebugHud().shouldShowDebugHud() || mc.currentScreen instanceof ChatScreen) return;
        int px = mc.player.getBlockX(), py = mc.player.getBlockY(), pz = mc.player.getBlockZ();
        String[][] cells;
        String dir = mc.player.getHorizontalFacing().asString();
        dir = Character.toUpperCase(dir.charAt(0)) + dir.substring(1);
        if (nether.get()) {
            boolean inNether = mc.world.getRegistryKey() == net.minecraft.world.World.NETHER;
            int nx = inNether ? px * 8 : px / 8, nz = inNether ? pz * 8 : pz / 8;
            cells = new String[][]{{"X", "" + px}, {"Y", "" + py}, {"Z", "" + pz}, {inNether ? "OW" : "NE", nx + " " + nz}, {"", dir}};
        } else {
            cells = new String[][]{{"X", "" + px}, {"Y", "" + py}, {"Z", "" + pz}, {"", dir}};
        }
        float w = 12, h = 30;
        float[] cw = new float[cells.length];
        for (int i = 0; i < cells.length; i++) {
            cw[i] = (cells[i][0].isEmpty() ? 0 : UI.width(cells[i][0], 11, Weight.SEMIBOLD) + 5) + UI.width(cells[i][1], 12, Weight.MEDIUM);
            w += cw[i] + (i > 0 ? 17 : 0);
        }
        w += 12;
        float x = 12, y = UI.screenH() - h - 12;
        ModernHud.card(x, y, w, h, 10);
        float tx = x + 12;
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                UI.rect(tx + 8, y + 9, 1, h - 18, UI.a(Theme.text(), 0.15f));
                tx += 17;
            }
            if (!cells[i][0].isEmpty()) {
                UI.textMid(cells[i][0], tx, y + h / 2f, 11, Weight.SEMIBOLD, Theme.dim());
                tx += UI.width(cells[i][0], 11, Weight.SEMIBOLD) + 5;
            }
            UI.textMid(cells[i][1], tx, y + h / 2f, 12, Weight.MEDIUM, Theme.text());
            tx += UI.width(cells[i][1], 12, Weight.MEDIUM);
        }
    }
}
