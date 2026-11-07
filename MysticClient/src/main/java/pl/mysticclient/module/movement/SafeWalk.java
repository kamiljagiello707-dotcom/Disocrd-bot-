package pl.mysticclient.module.movement;

import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;

public class SafeWalk extends Module {
    public SafeWalk() {
        super("SafeWalk", "Nie pozwala spaść z krawędzi bloku (jak skradanie).", Category.MOVEMENT);
    }
}
