package dev.vesc.client.module.cosmetics;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import dev.vesc.client.VescClient;
import dev.vesc.client.render.R2D;
import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;
import dev.vesc.client.ui.Theme;
import net.minecraft.item.*;

/** ToolColor – własny kolor mieczy, siekier, kilofów itd. (kosmetyka, widoczna u Ciebie). */
public class ToolColor extends Module {
    public final ModeSetting color = new ModeSetting("Kolor", "Color",
            "Purple", "White", "Black", "Blue", "Red", "Green", "Gold", "Pink", "Cyan", "Accent", "Rainbow");
    public final NumberSetting intensity = new NumberSetting("Intensywność", "Intensity", 80, 10, 100, 5);
    public final BoolSetting glint = new BoolSetting("Połysk", "Glint", false);
    public final BoolSetting swords = new BoolSetting("Miecze", "Swords", true);
    public final BoolSetting tools = new BoolSetting("Narzędzia", "Tools", true);
    public final BoolSetting weapons = new BoolSetting("Inna broń", "Other Weapons", true);

    public ToolColor() {
        super("ToolColor", Category.VISUALS, "pickaxe", "Własny kolor narzędzi i mieczy", "Custom color for tools and swords");
        place(Category.COSMETICS, "Items");
        add(color, intensity, glint, swords, tools, weapons);
    }

    public static ToolColor get() {
        VescClient rc = VescClient.get();
        if (rc == null || rc.modules == null) return null;
        return rc.modules.get("ToolColor") instanceof ToolColor t && t.isEnabled() ? t : null;
    }

    public boolean affects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item i = stack.getItem();
        if (i instanceof SwordItem) return swords.get();
        if (i instanceof MiningToolItem) return tools.get();
        if (i instanceof TridentItem || i instanceof MaceItem || i instanceof BowItem || i instanceof CrossbowItem) return weapons.get();
        return false;
    }

    /** kolor ARGB, którym mnożona jest tekstura przedmiotu */
    public int tint() {
        int c = switch (color.get()) {
            case "White" -> 0xFFFFFFFF;
            case "Black" -> 0xFF2A2A30;
            case "Blue" -> 0xFF4D8BFF;
            case "Red" -> 0xFFFF4D4D;
            case "Green" -> 0xFF4DFF88;
            case "Gold" -> 0xFFFFC94D;
            case "Pink" -> 0xFFFF6BC8;
            case "Cyan" -> 0xFF4DE8FF;
            case "Accent" -> Theme.accentWave(0);
            case "Rainbow" -> 0xFF000000 | java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000) / 4000f, 0.7f, 1f);
            default -> 0xFFB46BFF;
        };
        // alfa 200..254 = sygnał dla shadera VescClient: zamień na odcienie szarości i pokoloruj (siła = intensywność)
        float k = intensity.get().floatValue() / 100f;
        int alpha = 200 + Math.round(54 * k);
        return (alpha << 24) | (c & 0xFFFFFF);
    }
}
