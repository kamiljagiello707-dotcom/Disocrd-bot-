package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoReconnect – Reconnects automatically.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class AutoReconnect extends Module {
    public AutoReconnect() {
        super("AutoReconnect", Category.UTILITY, "refresh", "Automatyczne łączenie", "Reconnects automatically");
        place(Category.UTILITY, "Server");
        add(
                slider("Delay", 5, 1, 30, 1),
                slider("Attempts", 5, 1, 50, 1),
                toggle("Show Button", true)
        );
    }
}
