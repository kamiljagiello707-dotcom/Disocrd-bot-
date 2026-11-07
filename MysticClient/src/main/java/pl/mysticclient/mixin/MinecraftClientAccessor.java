package pl.mysticclient.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    int mystic$getItemUseCooldown();

    @Accessor("itemUseCooldown")
    void mystic$setItemUseCooldown(int value);
}
