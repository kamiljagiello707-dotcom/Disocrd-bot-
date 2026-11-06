package pl.firen.client.module.modules;

import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;

public class NoHurtCam extends Module {
    public NoHurtCam() { super("NoHurtCam", "Wyłącza trzęsienie kamery po obrażeniach.", Category.RENDER); }

    public static boolean active() {
        var m = FirenClient.modules();
        return m != null && m.get(NoHurtCam.class).isEnabled();
    }
}
