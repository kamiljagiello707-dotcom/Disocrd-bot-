package dev.vesc.gui;

import dev.vesc.gui.render.R2D;
import net.fabricmc.api.ClientModInitializer;

/** VescClient GUI – na razie: własne menu główne "Aurora". */
public class VescGui implements ClientModInitializer {
    public static final String VERSION = "1.0.0";

    @Override
    public void onInitializeClient() {
        R2D.initShaders();
    }
}
