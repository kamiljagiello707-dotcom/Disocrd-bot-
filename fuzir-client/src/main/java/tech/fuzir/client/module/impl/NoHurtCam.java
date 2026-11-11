package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.util.Icons;

public class NoHurtCam extends Module {
    public NoHurtCam() {
        super("NoHurtCam", "Kamera nie trzęsie się po otrzymaniu obrażeń", Category.VISUALS, Icons.CAMERA);
    }
}
