package dev.vesca.client;

import dev.vesca.client.render.R2D;
import net.fabricmc.api.ClientModInitializer;

/** VescaClient – na razie: własne menu główne "Aurora". */
public class VescaClient implements ClientModInitializer {
    public static final String VERSION = "1.1.0";

    @Override
    public void onInitializeClient() {
        R2D.initShaders();
    }
}
