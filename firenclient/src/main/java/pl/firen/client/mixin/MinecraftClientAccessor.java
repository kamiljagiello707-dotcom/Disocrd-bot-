package pl.firen.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    void firen$setItemUseCooldown(int value);

    @Accessor("itemUseCooldown")
    int firen$getItemUseCooldown();

    @Invoker("doAttack")
    boolean firen$doAttack();

    @Invoker("doItemUse")
    void firen$doItemUse();
}
