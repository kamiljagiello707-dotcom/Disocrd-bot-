package pl.mysticclient.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {
    @Mutable
    @Accessor("value")
    void mystic$setValue(T value);
}
