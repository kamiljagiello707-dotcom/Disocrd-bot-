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

import java.util.List;

/** Panel z modułami, które mają przypisany klawisz: ikona | nazwa | klawisz. */
public class KeyBinds extends HudModule {
    public final BoolSetting onlyActive = add(new BoolSetting("Tylko włączone", false));
    public final BoolSetting showKey = add(new BoolSetting("Pokaż klawisz", true));
    private static final float HEAD = 22, ROW = 17, PAD = 7;

    public KeyBinds() {
        super("KeyBinds", "Lista modułów z przypisanym klawiszem", Icons.KEYBOARD, 0f, 0.28f);
        setEnabledQuiet(true);
    }

    private List<Module> rows() {
        return ModuleManager.all().stream()
                .filter(m -> m.hasToggle() && m.bind.isSet() && (!onlyActive.on() || m.isEnabled() || m.bindAnim.get() > 0.01f))
                .toList();
    }

    @Override
    public float[] size(Canvas cv) {
        List<Module> rows = rows();
        float w = 130;
        for (Module m : rows) {
            float kw = showKey.on() ? cv.width(m.bind.keyName(), F.SMALL) + 18 : 0;
            w = Math.max(w, PAD + 14 + 7 + cv.width(m.name, F.REGULAR) + 12 + kw + PAD);
        }
        float h = HEAD + 6 + (rows.isEmpty() ? ROW : rows.size() * ROW) + 4;
        return new float[]{w, h};
    }

    @Override
    public void draw(Canvas cv, float x, float y, float w, float h, boolean right) {
        Hud.panel(cv, x, y, w, h, 9);
        // nagłówek
        cv.icon(Icons.KEYBOARD, x + PAD + 5, y + HEAD / 2 + 0.5f, Hud.WHITE, F.ICON_SM);
        cv.textMid("Keybinds", x + PAD + 15, y + HEAD / 2 + 0.5f, Hud.WHITE, F.BOLD);
        List<Module> rows = rows();
        long on = rows.stream().filter(Module::isEnabled).count();
        cv.textRight(on + "/" + rows.size(), x + w - PAD, y + HEAD / 2 + 0.5f, Hud.DIM, F.SMALL);
        // separator
        cv.hline(x + PAD, y + HEAD, w - PAD * 2, 0x1CFFFFFF);

        float ry = y + HEAD + 5;
        if (rows.isEmpty()) {
            cv.textMid("Przypisz klawisz w menu", x + PAD, ry + ROW / 2, Hud.DIM, F.SMALL);
            return;
        }
        long now = System.currentTimeMillis();
        for (Module m : rows) {
            float a = m.bindAnim.to(m.isEnabled() ? 1 : 0, 14);
            float cy = ry + ROW / 2;
            // błysk po wciśnięciu klawisza
            float flash = 1f - Math.min(1f, (now - m.toggledAt) / 650f);
            if (flash > 0) cv.round(x + 3, ry, w - 6, ROW, 5, Canvas.mulAlpha(0x2EFFFFFF, Anim.easeOutCubic(flash)));
            // ikona modułu
            cv.round(x + PAD, cy - 7, 14, 14, 4, Canvas.mix(0xFF1D1D21, Hud.WHITE, a));
            cv.icon(m.icon, x + PAD + 7, cy, Canvas.mix(0xFF7C7C83, 0xFF000000, a), F.ICON_SM);
            // nazwa
            cv.textMid(m.name, x + PAD + 21, cy, Canvas.mix(0xFF86868D, Hud.TEXT, a), F.REGULAR);
            if (m.holdMode()) {
                float nw = cv.width(m.name, F.REGULAR);
                cv.textMid("hold", x + PAD + 25 + nw, cy + 0.5f, Hud.DIM, F.SMALL);
            }
            // klawisz
            if (showKey.on()) {
                String k = m.bind.keyName();
                float kw = cv.width(k, F.SMALL) + 10;
                float kx = x + w - PAD - kw;
                cv.round(kx, cy - 6, kw, 12, 3.5f, Canvas.mix(0xFF19191C, 0xFF2A2A2F, a));
                cv.outline(kx, cy - 6, kw, 12, 3.5f, 1f, 0x14FFFFFF);
                cv.textCenter(k, kx + kw / 2, cy, Canvas.mix(0xFF8A8A90, Hud.WHITE, a), F.SMALL);
            }
            ry += ROW;
        }
    }
}
