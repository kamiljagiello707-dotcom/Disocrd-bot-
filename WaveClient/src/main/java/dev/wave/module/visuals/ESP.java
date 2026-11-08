package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ColorSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.util.ColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

public class ESP extends Module {
    public final ModeSetting colorMode = new ModeSetting("Color", "How outlines are coloured", "Static", "Static", "Rainbow", "Health", "Team");
    public final BooleanSetting players = new BooleanSetting("Players", true);
    public final BooleanSetting self = new BooleanSetting("Self", "Outline yourself in third person", false).visible(players::isOn);
    public final BooleanSetting hostiles = new BooleanSetting("Hostiles", true);
    public final BooleanSetting passives = new BooleanSetting("Passives", false);
    public final BooleanSetting items = new BooleanSetting("Items", true);
    public final ColorSetting playerColor = new ColorSetting("Player Color", 0xFFFFFFFF).visible(() -> colorMode.is("Static"));
    public final ColorSetting hostileColor = new ColorSetting("Hostile Color", 0xFFFF4040).visible(() -> colorMode.is("Static"));
    public final ColorSetting passiveColor = new ColorSetting("Passive Color", 0xFF60FF60).visible(() -> colorMode.is("Static"));
    public final ColorSetting itemColor = new ColorSetting("Item Color", 0xFFB0B0B0).visible(() -> colorMode.is("Static"));

    public ESP() {
        super("ESP", "Outlines entities through walls", Category.VISUALS);
        addSettings(colorMode, players, self, hostiles, passives, items, playerColor, hostileColor, passiveColor, itemColor);
    }

    @Override public String getSuffix() { return colorMode.get(); }

    public boolean shouldOutline(Entity e) {
        if (e == mc.player) return players.isOn() && self.isOn() && !mc.options.getPerspective().isFirstPerson();
        if (e instanceof PlayerEntity) return players.isOn();
        if (e instanceof Monster) return hostiles.isOn();
        if (e instanceof PassiveEntity) return passives.isOn();
        if (e instanceof ItemEntity) return items.isOn();
        return false;
    }

    public int getColor(Entity e) {
        switch (colorMode.get()) {
            case "Rainbow":
                return ColorUtil.rainbow(e.getId() * 120L, 0.7f, 1f, 1f) & 0xFFFFFF;
            case "Health":
                if (e instanceof LivingEntity le && le.getMaxHealth() > 0) {
                    float pct = Math.max(0, Math.min(1, le.getHealth() / le.getMaxHealth()));
                    return ColorUtil.lerp(0xFFFF3030, 0xFF30FF30, pct) & 0xFFFFFF;
                }
                break;
            case "Team":
                if (e.getScoreboardTeam() != null && e.getScoreboardTeam().getColor().getColorValue() != null)
                    return e.getScoreboardTeam().getColor().getColorValue();
                break;
            default:
                break;
        }
        if (e instanceof PlayerEntity) return playerColor.getColor() & 0xFFFFFF;
        if (e instanceof Monster) return hostileColor.getColor() & 0xFFFFFF;
        if (e instanceof PassiveEntity) return passiveColor.getColor() & 0xFFFFFF;
        return itemColor.getColor() & 0xFFFFFF;
    }
}
