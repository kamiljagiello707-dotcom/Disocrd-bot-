package dev.nova.client.gui;

/** Persisted position / collapsed state of a category panel. */
public class PanelState {
    public double x, y;
    public boolean open = true;

    public PanelState(double x, double y) { this.x = x; this.y = y; }
}
