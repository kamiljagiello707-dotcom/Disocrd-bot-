package dev.vesc.client.module.visuals;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * BlockOutline – Custom block outline.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class BlockOutline extends Module {
    public BlockOutline() {
        super("BlockOutline", Category.VISUALS, "i_outline", "Własny obrys bloku", "Custom block outline");
        place(Category.VISUALS, "Blocks");
        add(
                choice("Mode", "Outline", "Fill", "Both"),
                slider("Width", 2.0, 0.5, 5.0, 0.5),
                slider("Fill Opacity", 20, 0, 100, 5),
                toggle("Accent Color", true),
                toggle("Animated", true)
        );
    }
}
