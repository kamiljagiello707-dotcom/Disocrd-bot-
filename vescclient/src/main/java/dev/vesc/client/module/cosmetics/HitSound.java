package dev.vesc.client.module.cosmetics;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** HitSound – dźwięk przy każdym uderzeniu gracza / moba. Dźwięk wybierasz w ustawieniach. */
public class HitSound extends Module {
    public final ModeSetting sound = new ModeSetting("Dźwięk", "Sound",
            "Vesc", "Pop", "Glass", "Coin", "Laser", "Bubble", "Thump",
            "Bell", "Pling", "Chime", "XP", "Amethyst", "Crit");
    public final NumberSetting volume = new NumberSetting("Głośność", "Volume", 80, 0, 100, 5);
    public final NumberSetting pitch = new NumberSetting("Wysokość", "Pitch", 1.0, 0.5, 2.0, 0.05);
    public final BoolSetting randomPitch = new BoolSetting("Losowa wysokość", "Random pitch", true);
    public final BoolSetting playersOnly = new BoolSetting("Tylko gracze", "Players only", false);

    public HitSound() {
        super("HitSound", Category.UTILITY, "sound", "Dźwięk przy każdym uderzeniu", "Sound on every hit");
        place(Category.COSMETICS, "Sounds");
        add(sound, volume, pitch, randomPitch, playersOnly);
    }

    private Identifier id() {
        return switch (sound.get()) {
            case "Vesc" -> Identifier.of("vescclient", "hit.vesc");
            case "Pop" -> Identifier.of("vescclient", "hit.pop");
            case "Glass" -> Identifier.of("vescclient", "hit.glass");
            case "Coin" -> Identifier.of("vescclient", "hit.coin");
            case "Laser" -> Identifier.of("vescclient", "hit.laser");
            case "Bubble" -> Identifier.of("vescclient", "hit.bubble");
            case "Thump" -> Identifier.of("vescclient", "hit.thump");
            case "Bell" -> Identifier.ofVanilla("block.note_block.bell");
            case "Pling" -> Identifier.ofVanilla("block.note_block.pling");
            case "Chime" -> Identifier.ofVanilla("block.note_block.chime");
            case "XP" -> Identifier.ofVanilla("entity.experience_orb.pickup");
            case "Amethyst" -> Identifier.ofVanilla("block.amethyst_block.hit");
            default -> Identifier.ofVanilla("entity.player.attack.crit");
        };
    }

    /** odtwarza wybrany dźwięk (też jako podgląd w GUI) */
    public void play() {
        float p = pitch.get().floatValue();
        if (randomPitch.get()) p *= 0.92f + (float) Math.random() * 0.16f;
        float v = volume.get().floatValue() / 100f;
        if (v <= 0) return;
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(id()), p, v));
    }
}
