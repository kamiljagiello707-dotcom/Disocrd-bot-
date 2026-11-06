package pl.mystic.client.module.modules;

import pl.mystic.client.mixin.MinecraftClientAccessor;
import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class FastPlace extends Module {
    public FastPlace() { super("FastPlace", "Brak opóźnienia przy stawianiu bloków", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN); }

    @Override
    public void onTick() { ((MinecraftClientAccessor) mc).mystic$setItemUseCooldown(0); }
}
