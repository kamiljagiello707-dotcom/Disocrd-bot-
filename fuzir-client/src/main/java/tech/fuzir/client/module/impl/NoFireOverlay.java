package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.util.Icons;

public class NoFireOverlay extends Module {
    public NoFireOverlay() {
        super("NoFireOverlay", "Ukrywa ogień zasłaniający ekran", Category.VISUALS, Icons.FLAME);
    }
}
