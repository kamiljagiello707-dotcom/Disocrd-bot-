package dev.mustic.client.util;

import dev.mustic.client.module.ModuleManager;
import dev.mustic.client.module.modules.ClickGuiModule;

public final class Theme {
    public static final int BACKGROUND = 0xF0101016;
    public static final int MODULE = 0xE6141420;
    public static final int MODULE_HOVER = 0xE61E1E2E;
    public static final int SETTING = 0xE60C0C12;
    public static final int TEXT = 0xFFEDEDF5;
    public static final int TEXT_DIM = 0xFF8C8CA0;

    private Theme() {}

    /** Kolor akcentu z offsetem (do gradientow / tęczy). */
    public static int accent(long offset) {
        ClickGuiModule gui = ModuleManager.get(ClickGuiModule.class);
        return switch (gui.theme.get()) {
            case "Rainbow" -> ColorUtil.rainbow(offset, 0.65f, 1f);
            case "Ocean" -> ColorUtil.wave(0xFF00C6FF, 0xFF0072FF, offset);
            case "Blood" -> ColorUtil.wave(0xFFFF1E3C, 0xFF8E0E2A, offset);
            case "Mint" -> ColorUtil.wave(0xFF00F5A0, 0xFF00B37A, offset);
            case "Sunset" -> ColorUtil.wave(0xFFFF9A3C, 0xFFFF2E63, offset);
            default -> ColorUtil.wave(0xFFB44CFF, 0xFFFF4CD2, offset); // Mustic
        };
    }

    public static int accent() {
        return accent(0);
    }
}
