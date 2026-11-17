package dev.vesca.client;

import dev.vesca.client.render.R2D;
import dev.vesca.client.ui.Welcome;
import net.fabricmc.api.ClientModInitializer;

/** VescaClient – biało-czarne menu, listy serwerów i światów, ekran wczytywania i powitanie na serwerach. */
public class VescaClient implements ClientModInitializer {
    public static final String VERSION = "1.2.0";

    @Override
    public void onInitializeClient() {
        R2D.initShaders();
        Welcome.init();
    }
}
