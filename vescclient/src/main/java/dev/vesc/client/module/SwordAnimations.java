package dev.vesc.client.module;

import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** SwordAnimations – własne animacje machnięcia mieczem z pierwszej osoby (tylko wygląd). */
public class SwordAnimations extends Module {
    public final ModeSetting mode = new ModeSetting("Animacja", "Animation", "Smooth", "Slide", "Swipe", "Stab", "Spin", "Chop");
    public final NumberSetting strength = new NumberSetting("Siła", "Strength", 1.0, 0.3, 2.0, 0.05);
    public final BoolSetting onlySwords = new BoolSetting("Tylko miecze", "Only Swords", true);

    public SwordAnimations() {
        super("SwordAnimations", Category.CAMERA, "i_hand", "Własne animacje machania mieczem", "Custom sword swing animations");
        add(mode, strength, onlySwords);
    }

    public boolean applies(ItemStack st) { return isEnabled() && (!onlySwords.get() || st.getItem() instanceof SwordItem); }

    /** zastępuje vanilla applySwingOffset */
    public void apply(MatrixStack m, Arm arm, float p) {
        float i = arm == Arm.RIGHT ? 1 : -1, k = strength.get().floatValue();
        float s = MathHelper.sin(MathHelper.sqrt(p) * (float) Math.PI);   // szybki start, płynny koniec
        float s2 = MathHelper.sin(p * p * (float) Math.PI);
        float lin = MathHelper.sin(p * (float) Math.PI);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 45));
        switch (mode.get()) {
            case "Slide" -> {
                m.translate(-0.08f * i * s * k, 0.06f * s * k, -0.12f * s * k);
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -35 * s * k));
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55 * s * k));
            }
            case "Swipe" -> {
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -70 * s * k));
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25 * s2 * k));
            }
            case "Stab" -> {
                m.translate(0, -0.05f * lin * k, -0.45f * lin * k);
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20 * lin * k));
            }
            case "Spin" -> m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-360 * p));
            case "Chop" -> {
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100 * s * k));
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -10 * s2 * k));
            }
            default -> { // Smooth
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -12 * s * k));
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60 * s * k));
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -10 * s2 * k));
            }
        }
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45));
    }
}
