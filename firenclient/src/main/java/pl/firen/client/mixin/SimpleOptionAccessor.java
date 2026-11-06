package pl.firen.client.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor {
    /** Ustawia wartość z pominięciem walidacji (np. jasność > 100%). */
    @Accessor("value")
    void firen$setValueRaw(Object value);
}
