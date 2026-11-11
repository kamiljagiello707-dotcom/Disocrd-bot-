package tech.fuzir.client.module.impl;

import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleList extends HudModule {
    public final BoolSetting icons = add(new BoolSetting("Ikony", true));
    public final BoolSetting bar = add(new BoolSetting("Pasek z boku", true));
    public final BoolSetting suffixes = add(new BoolSetting("Dopiski", true));
    private static final float ROW = 15;

    public ModuleList() {
        super("Lista modułów", "Włączone moduły przy krawędzi ekranu", Icons.LIST, 1f, 0f);
        setEnabledQuiet(true);
    }

    private List<Module> rows(Canvas cv) {
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (!m.showInList()) continue;
            if (m.listAnim.to(m.isEnabled() ? 1 : 0, 13) > 0.005f) out.add(m);
        }
        out.sort(Comparator.comparingDouble((Module m) -> -rowWidth(cv, m)));
        return out;
    }

    private float rowWidth(Canvas cv, Module m) {
        String s = m.suffix();
        float w = 8 + cv.width(m.name, F.REGULAR) + 8;
        if (s != null && suffixes.on()) w += 4 + cv.width(s, F.REGULAR);
        if (icons.on()) w += 14;
        return w;
    }

    @Override
    public float[] size(Canvas cv) {
        List<Module> rows = rows(cv);
        if (rows.isEmpty()) return Hud.editing() ? new float[]{90, ROW} : null;
        float w = 0, h = 0;
        for (Module m : rows) {
            w = Math.max(w, rowWidth(cv, m));
            h += (ROW + 2) * Anim.easeOutCubic(m.listAnim.get());
        }
        return new float[]{w, Math.max(ROW, h)};
    }

    @Override
    public void draw(Canvas cv, float x, float y, float w, float h, boolean right) {
        List<Module> rows = rows(cv);
        if (rows.isEmpty()) {
            Hud.panel(cv, x, y, w, h, 5);
            cv.textCenter("Lista modułów", x + w / 2, y + h / 2, Hud.DIM, F.SMALL);
            return;
        }
        float ry = y;
        for (Module m : rows) {
            float a = Anim.easeOutCubic(m.listAnim.get());
            float rw = rowWidth(cv, m);
            float slide = (1 - a) * (rw + 12);
            float rx = right ? x + w - rw + slide : x - slide;
            float prev = cv.alpha;
            cv.alpha = prev * Math.min(1, a * 1.2f);
            cv.shadow(rx, ry + 1, rw, ROW, 5, 6, 0x50000000);
            cv.round(rx, ry, rw, ROW, 5, Hud.PANEL);
            if (bar.on()) {
                float bx = right ? rx + rw - 3.5f : rx + 1.5f;
                cv.round(bx, ry + 3.5f, 2, ROW - 7, 1, Hud.WHITE);
            }
            float tx = rx + 8;
            float cy = ry + ROW / 2;
            if (icons.on()) {
                cv.icon(m.icon, tx + 4, cy, Hud.WHITE, F.ICON_SM);
                tx += 14;
            }
            cv.textMid(m.name, tx, cy, Hud.TEXT, F.REGULAR);
            String s = m.suffix();
            if (s != null && suffixes.on()) cv.textMid(s, tx + cv.width(m.name, F.REGULAR) + 4, cy, Hud.MUTED, F.REGULAR);
            cv.alpha = prev;
            ry += (ROW + 2) * a;
        }
    }
}
