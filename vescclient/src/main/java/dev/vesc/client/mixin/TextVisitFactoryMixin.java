package dev.vesc.client.mixin;

import dev.vesc.client.module.Legit;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** NameProtect: podmienia Twój nick w każdym tekście rysowanym na ekranie. */
@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    @ModifyVariable(method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static String vesc$protect(String text) { return Legit.protect(text); }
}
