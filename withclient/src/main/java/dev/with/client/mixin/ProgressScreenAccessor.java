package dev.with.client.mixin;

import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProgressScreen.class)
public interface ProgressScreenAccessor {
    @Accessor("title") Text withclient$title();
    @Accessor("task") Text withclient$task();
    @Accessor("progress") int withclient$progress();
}
