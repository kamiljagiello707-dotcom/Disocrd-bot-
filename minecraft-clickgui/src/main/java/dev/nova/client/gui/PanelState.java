package dev.nova.client.gui;

/** Persisted position / collapsed state of a category panel. */
public class PanelState {
    public double x, y;
    public boolean open = true;
    /** False until loaded from config or auto-laid out for the current screen. */
    public boolean placed;
    /** When set, this panel sits right below the given panel and follows it until dragged. */
    public dev.nova.client.module.Category anchor;

    public PanelState(double x, double y) { this.x = x; this.y = y; }
}
