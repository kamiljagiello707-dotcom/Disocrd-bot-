package tech.fuzir.client.module.impl;

import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Watermark extends HudModule {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public final ModeSetting style = add(new ModeSetting("Styl", "Pełny", "Pełny", "Minimalny"));
    public final BoolSetting fps = add(new BoolSetting("FPS", true));
    public final BoolSetting time = add(new BoolSetting("Godzina", true));
    public final BoolSetting nick = add(new BoolSetting("Nick", true));

    public Watermark() {
        super("Watermark", "Logo klienta, FPS i godzina", Icons.TYPE, 0f, 0f);
        setEnabledQuiet(true);
    }

    private List<String> parts() {
        List<String> p = new ArrayList<>();
        if (style.is("Minimalny")) return p;
        if (nick.on()) p.add(mc.getSession().getUsername());
        if (fps.on()) p.add(mc.getCurrentFps() + " fps");
        if (time.on()) p.add(LocalTime.now().format(TIME));
        return p;
    }

    @Override
    public float[] size(Canvas cv) {
        float w = 6 + 14 + 6 + cv.width("Fuzir", F.BOLD) + 8;
        for (String s : parts()) w += 11 + cv.width(s, F.REGULAR);
        return new float[]{w, 24};
    }

    @Override
    public void draw(Canvas cv, float x, float y, float w, float h, boolean right) {
        Hud.panel(cv, x, y, w, h, 8);
        float cy = y + h / 2;
        cv.round(x + 6, cy - 7, 14, 14, 4.5f, Hud.WHITE);
        cv.textCenter("F", x + 13.2f, cy, 0xFF000000, F.BOLD);
        float tx = x + 26;
        cv.textMid("Fuzir", tx, cy, Hud.WHITE, F.BOLD);
        tx += cv.width("Fuzir", F.BOLD);
        for (String s : parts()) {
            cv.circle(tx + 5.5f, cy, 1.1f, Hud.DIM);
            cv.textMid(s, tx + 11, cy, Hud.MUTED, F.REGULAR);
            tx += 11 + cv.width(s, F.REGULAR);
        }
    }
}
