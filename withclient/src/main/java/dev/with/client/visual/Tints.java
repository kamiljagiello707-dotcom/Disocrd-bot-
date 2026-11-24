package dev.with.client.visual;

import dev.with.client.mixin.OverlayTextureAccessor;
import dev.with.client.render.R2D;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Locale;

/** Kolory: peleryna (Cape), połysk zaklęć (ItemGlint), błysk obrażeń (HitColor), kolor nicku w TAB (NameColor). */
public final class Tints {
    private Tints() {}

    private static final Identifier[] GLINT = {Identifier.ofVanilla("textures/misc/enchanted_glint_item.png"), Identifier.ofVanilla("textures/misc/enchanted_glint_entity.png")};
    private static final NativeImage[] glintSrc = new NativeImage[2];
    private static final NativeImageBackedTexture[] glintTex = new NativeImageBackedTexture[2];
    private static int glintApplied, hitApplied;
    private static boolean glintOn, hitOn;
    private static final int VANILLA_HURT = -1308622593;

    public static void init() { ClientTickEvents.END_CLIENT_TICK.register(Tints::tick); }

    // ------------------------------------------------------------------ Cape

    public static Identifier cape() {
        if (!V.on("Cape")) return null;
        return R2D.id("textures/capes/" + V.mode("Cape", "Peleryna", "WithClient").toLowerCase(Locale.ROOT) + ".png");
    }

    // ------------------------------------------------------------------ NameColor

    public static Text name(PlayerListEntry e, Text orig) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!V.on("NameColor") || mc.player == null || e.getProfile() == null || !e.getProfile().getId().equals(mc.player.getUuid())) return null;
        String s = orig.getString();
        String mode = V.mode("NameColor", "Tryb", "Gradient");
        int c1 = V.color("NameColor", "Kolor", 0xFFFFFFFF), c2 = V.color("NameColor", "Kolor 2", 0xFFB8B8C0);
        boolean anim = V.bo("NameColor", "Animacja"), bold = V.bo("NameColor", "Pogrubienie");
        float time = (System.currentTimeMillis() % 100_000L) / 1000f;
        MutableText out = Text.empty();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            float t = n <= 1 ? 0 : i / (float) (n - 1);
            int c = switch (mode) {
                case "Static" -> c1;
                case "Rainbow" -> V.rainbow(1.2f, t * 0.6f - (anim ? time * 0.18f : 0));
                default -> {
                    float k = anim ? 0.5f + 0.5f * (float) Math.sin((t * 2 - time * 1.6f) * Math.PI * 0.5) : t;
                    yield R2D.blend(c1, c2, k);
                }
            };
            int rgb = c & 0xFFFFFF;
            out.append(Text.literal(String.valueOf(s.charAt(i))).styled(st -> st.withColor(rgb).withBold(bold)));
        }
        return out;
    }

    // ------------------------------------------------------------------ tick: ItemGlint + HitColor

    private static void tick(MinecraftClient mc) {
        try { glint(mc); } catch (Exception ignored) {}
        try { hitColor(mc); } catch (Exception ignored) {}
    }

    private static int colorOf(String mod, int def) {
        return V.mode(mod, "Tryb", "Static").equals("Rainbow") ? (V.rainbow(1, 0) & 0xFFFFFF) | (V.color(mod, "Kolor", def) & 0xFF000000) : V.color(mod, "Kolor", def);
    }

    private static void glint(MinecraftClient mc) throws Exception {
        if (!V.on("ItemGlint")) {
            if (glintOn) {
                for (int i = 0; i < 2; i++) { mc.getTextureManager().destroyTexture(GLINT[i]); glintTex[i] = null; }
                glintOn = false;
            }
            return;
        }
        int c = colorOf("ItemGlint", 0xFFFFFFFF);
        if (glintOn && c == glintApplied) return;
        for (int i = 0; i < 2; i++) {
            if (glintSrc[i] == null) try (InputStream in = mc.getResourceManager().open(GLINT[i])) { glintSrc[i] = NativeImage.read(in); }
            NativeImage src = glintSrc[i];
            if (glintTex[i] == null) {
                glintTex[i] = new NativeImageBackedTexture(new NativeImage(src.getWidth(), src.getHeight(), false));
                mc.getTextureManager().registerTexture(GLINT[i], glintTex[i]);
            }
            NativeImage img = glintTex[i].getImage();
            float tr = V.r(c), tg = V.g(c), tb = V.b(c), k = 0.35f + 0.65f * V.a(c);
            for (int y = 0; y < src.getHeight(); y++)
                for (int x = 0; x < src.getWidth(); x++) {
                    int p = src.getColor(x, y);
                    int r = p & 0xFF, g = (p >> 8) & 0xFF, b = (p >> 16) & 0xFF, a = p >>> 24;
                    float lum = Math.min(255, (r * 0.3f + g * 0.45f + b * 0.25f) * 1.35f) * k;
                    img.setColor(x, y, a << 24 | (int) (lum * tb) << 16 | (int) (lum * tg) << 8 | (int) (lum * tr));
                }
            glintTex[i].upload();
            glintTex[i].setFilter(true, false);
        }
        glintApplied = c;
        glintOn = true;
    }

    private static void hitColor(MinecraftClient mc) {
        boolean want = V.on("HitColor");
        if (!want && !hitOn) return;
        NativeImageBackedTexture tex = ((OverlayTextureAccessor) mc.gameRenderer.getOverlayTexture()).withclient$texture();
        NativeImage img = tex.getImage();
        if (img == null) return;
        int abgr;
        if (want) {
            int c = colorOf("HitColor", 0x66B4A0FF);
            if (hitOn && c == hitApplied) return;
            int ta = (int) ((1 - V.a(c) * 0.85f) * 255);
            abgr = ta << 24 | (c & 0xFF) << 16 | (c & 0xFF00) | ((c >> 16) & 0xFF);
            hitApplied = c;
            hitOn = true;
        } else {
            abgr = VANILLA_HURT;
            hitOn = false;
        }
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColor(x, y, abgr);
        tex.upload();
    }
}
