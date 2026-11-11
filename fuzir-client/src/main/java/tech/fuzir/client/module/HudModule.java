package tech.fuzir.client.module;

import tech.fuzir.client.util.Canvas;

/** Element HUD, który można przesuwać w edytorze HUD. */
public abstract class HudModule extends Module {
    /** Pozycja jako ułamek wolnego miejsca na ekranie (0 = lewo/góra, 1 = prawo/dół). */
    public float fx, fy;
    public final float defaultFx, defaultFy;
    /** Ostatnie wymiary i pozycja w jednostkach HUD (do edytora). */
    public float lastX, lastY, lastW, lastH;

    protected HudModule(String name, String description, char icon, float fx, float fy) {
        super(name, description, Category.HUD, icon);
        this.fx = this.defaultFx = fx;
        this.fy = this.defaultFy = fy;
    }

    /** Wymiary elementu: {szerokość, wysokość}. */
    public abstract float[] size(Canvas cv);

    /** Rysuje element; {@code right} = element przy prawej krawędzi, treść wyrównana do prawej. */
    public abstract void draw(Canvas cv, float x, float y, float w, float h, boolean right);

    /** Czy pokazać element w edytorze, nawet jeśli nie ma nic do wyświetlenia. */
    public boolean previewInEditor() {
        return true;
    }
}
