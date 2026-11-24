package dev.with.client.visual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import org.lwjgl.glfw.GLFW;

/**
 * Wszystko, co zmienia widok kamery i świata po stronie klienta (tylko wygląd):
 * Zoom, CustomFOV, Fullbright, TimeChanger, Weather, Ambience, ViewModel, SwingAnimation.
 * Wywoływane z mixinów.
 */
public final class View {
    private View() {}

    public static KeyBinding ZOOM;
    private static float zoom = 1f, zoomLevel = 4f;
    private static boolean zoomToggled, zoomPrev, smoothSaved, smoothWas;
    private static long lastFrame;
    private static float fb, weatherW;
    private static ItemStack swingItem = ItemStack.EMPTY;

    public static void init() {
        ZOOM = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.withclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.withclient"));
        ClientTickEvents.END_CLIENT_TICK.register(View::tick);
    }

    private static void tick(MinecraftClient mc) {
        while (ZOOM.wasPressed()) if (V.bo("Zoom", "Przełączanie")) zoomToggled = !zoomToggled;
        if (!V.on("Zoom")) zoomToggled = false;
        boolean active = zoomActive();
        if (active && !zoomPrev) {
            zoomLevel = V.num("Zoom", "Siła", 4);
            if (V.bo("Zoom", "Kinowa kamera")) { smoothWas = mc.options.smoothCameraEnabled; mc.options.smoothCameraEnabled = true; smoothSaved = true; }
        }
        if (!active && zoomPrev && smoothSaved) { mc.options.smoothCameraEnabled = smoothWas; smoothSaved = false; }
        zoomPrev = active;

        float t = V.on("Fullbright") ? 1 : 0;
        fb = V.bo("Fullbright", "Płynne przejście") ? fb + (t - fb) * 0.16f : t;
        if (Math.abs(fb - t) < 0.004f) fb = t;

        float w = V.on("Weather") ? 1 : 0;
        weatherW += (w - weatherW) * 0.06f;
        if (Math.abs(weatherW - w) < 0.004f) weatherW = w;
    }

    // ------------------------------------------------------------------ Zoom / FOV

    public static boolean zoomActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!V.on("Zoom") || mc.player == null || ZOOM == null) return false;
        return V.bo("Zoom", "Przełączanie") ? zoomToggled : ZOOM.isPressed() && mc.currentScreen == null;
    }

    /** animowane powiększenie (1 = brak); liczone w skali logarytmicznej, więc wjazd jest równy */
    private static float zoomFactor() {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float target = zoomActive() ? Math.max(1, zoomLevel) : 1;
        if (!V.bo("Zoom", "Płynnie")) zoom = target;
        else {
            double lz = Math.log(zoom), lt = Math.log(target);
            zoom = (float) Math.exp(lz + (lt - lz) * (1 - Math.exp(-dt * 13)));
            if (Math.abs(zoom - target) < 0.002f) zoom = target;
        }
        return zoom;
    }

    public static float zoom() { return zoom; }

    public static double fov(double v, float mult) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (V.on("CustomFOV")) {
            int opt = mc.options.getFov().getValue();
            v *= V.num("CustomFOV", "FOV", 100) / Math.max(1, opt);
            if (mult > 0.05f) {
                if (V.bo("CustomFOV", "Bez efektów")) v /= mult;
                else if (V.bo("CustomFOV", "Stałe przy sprincie") && mult > 1) v /= mult;
            }
        }
        return v / zoomFactor();
    }

    public static boolean hideHand() { return zoom > 1.03f && V.on("Zoom") && V.bo("Zoom", "Ukryj rękę"); }

    /** kółko myszy w trakcie zoomu zmienia przybliżenie */
    public static boolean scroll(double v) {
        if (!zoomActive() || !V.bo("Zoom", "Scroll") || v == 0) return false;
        zoomLevel = MathHelper.clamp(zoomLevel * (float) (v > 0 ? 1.18 : 1 / 1.18), 1.2f, 60f);
        return true;
    }

    /** mniejsza czułość myszy przy przybliżeniu */
    public static double sensitivity() {
        return zoom > 1.01f && V.on("Zoom") && V.bo("Zoom", "Niższa czułość") ? 1.0 / zoom : 1.0;
    }

    // ------------------------------------------------------------------ Fullbright

    public static float gamma(float orig) {
        if (fb <= 0 || !V.mode("Fullbright", "Tryb", "Gamma").equals("Gamma")) return orig;
        float g = 1 + 14 * V.num("Fullbright", "Gamma", 100) / 100f;
        return orig + (Math.max(orig, g) - orig) * fb;
    }

    public static float nightVision() {
        if (fb <= 0 || !V.mode("Fullbright", "Tryb", "Gamma").equals("Night vision")) return 0;
        return fb * V.num("Fullbright", "Gamma", 100) / 100f;
    }

    // ------------------------------------------------------------------ Czas / pogoda / kolory nieba

    public static long timeOfDay(long orig) {
        double t = (V.num("TimeChanger", "Godzina", 12) - 6) * 1000.0;
        if (V.bo("TimeChanger", "Cykl")) t += Util.getMeasuringTimeMs() / 1000.0 * 100 * V.num("TimeChanger", "Prędkość", 1);
        long day = Math.floorDiv(orig, 24000L);
        return day * 24000L + Math.floorMod((long) t, 24000L);
    }

    public static boolean weatherActive() { return weatherW > 0; }

    public static float rain(float orig) {
        String m = V.mode("Weather", "Tryb", "Clear");
        float k = m.equals("Clear") ? 0 : V.num("Weather", "Intensywność", 100) / 100f;
        return orig + (k - orig) * weatherW;
    }

    public static float thunder(float orig) {
        float k = V.mode("Weather", "Tryb", "Clear").equals("Thunder") ? V.num("Weather", "Intensywność", 100) / 100f : 0;
        return orig + (k - orig) * weatherW;
    }

    public static Biome.Precipitation precipitation(Biome.Precipitation orig) {
        if (weatherW <= 0) return orig;
        return switch (V.mode("Weather", "Tryb", "Clear")) {
            case "Snow" -> Biome.Precipitation.SNOW;
            case "Rain", "Thunder" -> Biome.Precipitation.RAIN;
            default -> orig;
        };
    }

    /** jasność dnia 0..1 (tak jak liczy ją Minecraft dla nieba) */
    public static float daylight(ClientWorld w, float td) {
        return MathHelper.clamp(MathHelper.cos(w.getSkyAngle(td) * 6.2831855f) * 2 + 0.5f, 0, 1);
    }

    private static int ambience(String color, int def, float off) {
        return V.bo("Ambience", "Tęcza") ? V.rainbow(0.4f, off) : V.color("Ambience", color, def);
    }

    public static Vec3d sky(Vec3d orig, ClientWorld w, float td) {
        if (!V.on("Ambience") || !V.bo("Ambience", "Kolor nieba")) return orig;
        int c = ambience("Niebo", 0xFF9D8CFF, 0);
        float f = daylight(w, td);
        float rain = w.getRainGradient(td);
        float k = f * (1 - rain * 0.6f);
        return new Vec3d(V.r(c) * k, V.g(c) * k, V.b(c) * k);
    }

    public static Vec3d clouds(Vec3d orig, ClientWorld w, float td) {
        if (!V.on("Ambience") || !V.bo("Ambience", "Kolor chmur")) return orig;
        int c = ambience("Chmury", 0xFFF3E8FF, 0.1f);
        float f = daylight(w, td) * 0.9f + 0.1f;
        return new Vec3d(V.r(c) * f, V.g(c) * f, V.b(c) * f);
    }

    /** kolor mgły i tła (null = zostaw jak w grze) */
    public static float[] fogColor(Camera cam, ClientWorld w, float td) {
        if (cam.getSubmersionType() != CameraSubmersionType.NONE || blind(cam)) return null;
        if (Sky.active(w)) return Sky.horizon(w, td);
        if (!V.on("Ambience") || !V.bo("Ambience", "Kolor mgły")) return null;
        int c = ambience("Mgła", 0xFFE0C8FF, 0.05f);
        float f = daylight(w, td) * 0.94f + 0.06f;
        float rain = w.getRainGradient(td);
        f *= 1 - rain * 0.5f;
        return new float[]{V.r(c) * f, V.g(c) * f, V.b(c) * f};
    }

    private static boolean blind(Camera cam) {
        return cam.getFocusedEntity() instanceof LivingEntity le && (le.hasStatusEffect(StatusEffects.BLINDNESS) || le.hasStatusEffect(StatusEffects.DARKNESS));
    }

    /** NoRender → Mgła: tylko zwykła mgła terenu (nie pod wodą, nie przy ślepocie) */
    public static boolean noFog(Camera cam, BackgroundRenderer.FogType type) {
        return type == BackgroundRenderer.FogType.FOG_TERRAIN && cam.getSubmersionType() == CameraSubmersionType.NONE && !blind(cam) && V.hide("NoRender", "Mgła");
    }

    // ------------------------------------------------------------------ ViewModel / SwingAnimation

    public static void viewModel(MatrixStack ms, Hand hand, Arm main, ItemStack item) {
        swingItem = item;
        if (!V.on("ViewModel")) return;
        boolean right = (hand == Hand.MAIN_HAND ? main : main.getOpposite()) == Arm.RIGHT;
        String sfx = right ? "" : " lewa";
        ms.translate(V.num("ViewModel", "X" + sfx, 0), V.num("ViewModel", "Y" + sfx, 0), -V.num("ViewModel", "Z" + sfx, 0));
    }

    public static void viewScale(MatrixStack ms) {
        if (!V.on("ViewModel")) return;
        float s = V.num("ViewModel", "Skala", 1);
        if (s != 1) ms.scale(s, s, s);
    }

    private static boolean swingFor(ItemStack st) {
        return V.on("SwingAnimation") && (V.bo("SwingAnimation", "Zawsze") || st.getItem() instanceof SwordItem);
    }

    public static int swingDuration(int orig, LivingEntity e) {
        if (e != MinecraftClient.getInstance().player || !swingFor(e.getMainHandStack())) return orig;
        return Math.max(1, Math.round(orig / V.num("SwingAnimation", "Prędkość", 1)));
    }

    /** własne animacje machnięcia; false = zostaw animację z gry */
    public static boolean swing(MatrixStack ms, Arm arm, float p) {
        if (!swingFor(swingItem)) return false;
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(p * p * MathHelper.PI);
        float g = MathHelper.sin(MathHelper.sqrt(p) * MathHelper.PI);
        switch (V.mode("SwingAnimation", "Styl", "1.7")) {
            case "Smooth" -> {
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 45));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-g * 55));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -g * 25));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45));
            }
            case "Spin" -> {
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 45));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-p * 360));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45));
            }
            case "Slide" -> {
                ms.translate(0, -g * 0.08f, -g * 0.18f);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45 - g * 40)));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-g * 25));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45));
            }
            case "Push" -> {
                ms.translate(0, g * 0.04f, -g * 0.38f);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-g * 14));
            }
            case "Chop" -> {
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 45));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-f * 115 + g * 10));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45));
                ms.translate(0, -g * 0.08f, 0);
            }
            default -> { // "1.7": niżej i z większym łukiem, jak w starych wersjach
                ms.translate(i * 0.03f, 0.05f, 0.02f);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45 + f * -25)));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -25));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -90));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45));
            }
        }
        return true;
    }
}
