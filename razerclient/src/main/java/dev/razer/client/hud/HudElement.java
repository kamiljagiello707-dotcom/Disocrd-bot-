package dev.razer.client.hud;

import com.google.gson.JsonObject;
import dev.razer.client.lang.Lang;
import dev.razer.client.setting.BoolSetting;
import dev.razer.client.ui.Anim;
import net.minecraft.client.gui.DrawContext;

/**
 * Element HUD, który można przeciągać w edytorze HUD.
 * Pozycja zapisywana jest względem najbliższego rogu ekranu, więc działa przy każdej rozdzielczości.
 */
public abstract class HudElement {
    public final String id, icon;
    private final String nameKey;
    public final BoolSetting enabled;
    public final Anim visible = new Anim(0, 12);

    /** rozmiar z ostatniej klatki */
    public float w, h;
    /** czy użytkownik przesunął element */
    public boolean custom;
    private boolean anchorRight, anchorBottom;
    private float offX, offY;
    /** ostatnia narysowana pozycja */
    public float lastX, lastY;

    protected HudElement(String id, String nameKey, String icon, boolean on, float w, float h) {
        this.id = id; this.nameKey = nameKey; this.icon = icon; this.w = w; this.h = h;
        this.enabled = new BoolSetting(id, id, on);
    }

    public String getName() { return Lang.get(nameKey); }
    public String getDesc() { return Lang.get(nameKey + ".desc"); }

    /** domyślna pozycja (lewy górny róg) */
    protected abstract float[] defaultPos(int sw, int sh, Hud hud);

    /** czy element ma teraz coś do pokazania (np. TargetHUD bez celu = nie) */
    public boolean hasContent() { return true; }

    /** rysuje element w (x, y) i ustawia w/h; preview = podgląd w edytorze */
    public abstract void render(DrawContext ctx, float x, float y, boolean preview);

    public float[] position(int sw, int sh, Hud hud) {
        float x, y;
        if (custom) {
            x = anchorRight ? sw - offX - w : offX;
            y = anchorBottom ? sh - offY - h : offY;
        } else {
            float[] d = defaultPos(sw, sh, hud);
            x = d[0]; y = d[1];
        }
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        return new float[]{x, y};
    }

    public void setPosition(float x, float y, int sw, int sh) {
        custom = true;
        anchorRight = x + w / 2f > sw / 2f;
        anchorBottom = y + h / 2f > sh / 2f;
        offX = anchorRight ? sw - x - w : x;
        offY = anchorBottom ? sh - y - h : y;
    }

    public void resetPosition() { custom = false; }

    public JsonObject save() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled.get());
        o.addProperty("custom", custom);
        o.addProperty("right", anchorRight);
        o.addProperty("bottom", anchorBottom);
        o.addProperty("x", offX);
        o.addProperty("y", offY);
        return o;
    }

    public void load(JsonObject o) {
        try {
            enabled.set(o.get("enabled").getAsBoolean());
            custom = o.get("custom").getAsBoolean();
            anchorRight = o.get("right").getAsBoolean();
            anchorBottom = o.get("bottom").getAsBoolean();
            offX = o.get("x").getAsFloat();
            offY = o.get("y").getAsFloat();
        } catch (Exception ignored) {}
    }
}
