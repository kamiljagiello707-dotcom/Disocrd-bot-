package dev.razer.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.razer.client.RazerClient;
import dev.razer.client.lang.Lang;
import dev.razer.client.module.Module;
import dev.razer.client.render.Fonts;
import dev.razer.client.render.Fonts.Font;
import dev.razer.client.render.R2D;
import dev.razer.client.ui.Anim;
import dev.razer.client.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HUD RazerClient 2.0: przeciągalne elementy (edytor HUD) w kompaktowym stylu "embed".
 */
public class Hud {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    // wspólny styl
    public static final float R = 8f, PADX = 8, HEAD = 16, ROW = 10.5f, NAME = 0.66f, ICON = 6.5f;

    public final Watermark watermark = new Watermark();
    public final ModuleList moduleList = new ModuleList();
    public final Keybinds keybinds = new Keybinds();
    public final TargetHud target = new TargetHud();
    public final Armor armor = new Armor();
    public final Potions potions = new Potions();
    public final Coords coords = new Coords();
    public final Info info = new Info();
    public final List<HudElement> elements = List.of(watermark, moduleList, keybinds, target, potions, armor, info, coords);

    private double lastX, lastZ, speed;
    private long lastSpeedTime;

    public HudElement byId(String id) {
        for (HudElement e : elements) if (e.id.equals(id)) return e;
        return null;
    }

    public void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        R2D.alpha = 1f;
        updateSpeed(mc);
        for (HudElement e : elements) {
            float v = e.visible.animate(e.enabled.get() && e.hasContent() ? 1 : 0);
            if (v < 0.01f) continue;
            float[] p = e.position(sw, sh, this);
            e.lastX = p[0]; e.lastY = p[1];
            R2D.alpha = Anim.easeOutCubic(v);
            ctx.getMatrices().push();
            float s = 0.92f + 0.08f * Anim.easeOutCubic(v);
            ctx.getMatrices().translate(p[0] + e.w / 2f, p[1] + e.h / 2f, 0);
            ctx.getMatrices().scale(s, s, 1);
            ctx.getMatrices().translate(-(p[0] + e.w / 2f), -(p[1] + e.h / 2f), 0);
            e.render(ctx, p[0], p[1], false);
            ctx.getMatrices().pop();
        }
        R2D.alpha = 1f;
        RazerClient.get().notifications.render(ctx, sw, sh);
    }

    private void updateSpeed(MinecraftClient mc) {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastSpeedTime >= 250) {
            double dx = mc.player.getX() - lastX, dz = mc.player.getZ() - lastZ;
            double dt = (now - lastSpeedTime) / 1000.0;
            if (lastSpeedTime != 0 && dt < 2) speed = Math.sqrt(dx * dx + dz * dz) / dt;
            lastX = mc.player.getX(); lastZ = mc.player.getZ(); lastSpeedTime = now;
        }
    }

    // ================================================================== styl

    public static void panel(DrawContext ctx, float x, float y, float w, float h) { panel(ctx, x, y, w, h, R); }

    public static void panel(DrawContext ctx, float x, float y, float w, float h, float r) {
        R2D.shadow(ctx, x, y, w, h, 8, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, r, Theme.hudBg());
        R2D.roundOutline(ctx, x, y, w, h, r, 0.5f, Theme.stroke());
    }

    /** cienki pasek akcentu z gradientem u góry panelu */
    public static void accentLine(DrawContext ctx, float x, float y, float w) {
        if (Theme.accentPreset() == Theme.Accent.MONO) return;
        float lw = Math.min(w - 2 * R - 4, 40);
        R2D.gradientH(ctx, x + w / 2f - lw / 2f, y + 0.5f, lw / 2f, 1.2f, R2D.withAlpha(Theme.accentWave(0), 0f), Theme.accentWave(1.5f));
        R2D.gradientH(ctx, x + w / 2f, y + 0.5f, lw / 2f, 1.2f, Theme.accentWave(1.5f), R2D.withAlpha(Theme.accentWave(3f), 0f));
    }

    public static float header(DrawContext ctx, float x, float y, float w, String icon, String title, String count) {
        accentLine(ctx, x, y, w);
        R2D.icon(ctx, icon, x + PADX, y + HEAD / 2f - 3.75f, 7.5f, Theme.accentWave(0));
        Fonts.drawV(ctx, title, x + PADX + 11, y + HEAD / 2f + 0.5f, Theme.text(), Font.BOLD, 0.72f);
        if (count != null)
            Fonts.drawV(ctx, count, x + w - PADX - Fonts.width(count, Font.SEMIBOLD, 0.62f), y + HEAD / 2f + 0.5f, Theme.textFaint(), Font.SEMIBOLD, 0.62f);
        return y + HEAD;
    }

    private static void row(DrawContext ctx, float x, float y, float w, String icon, String label, String value, int iconCol, int labelCol, int valueCol) {
        R2D.icon(ctx, icon, x + PADX, y + ROW / 2f - ICON / 2f, ICON, iconCol);
        Fonts.drawV(ctx, label, x + PADX + 11, y + ROW / 2f + 0.5f, labelCol, Font.SEMIBOLD, NAME);
        if (value != null)
            Fonts.drawV(ctx, value, x + w - PADX - Fonts.width(value, Font.SEMIBOLD, NAME), y + ROW / 2f + 0.5f, valueCol, Font.SEMIBOLD, NAME);
    }

    private static float rowWidth(String label, String value) {
        return Fonts.width(label, Font.SEMIBOLD, NAME) + (value != null ? Fonts.width(value, Font.SEMIBOLD, NAME) + 12 : 0) + PADX * 2 + 11;
    }

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    // ================================================================== elementy

    public class Watermark extends HudElement {
        Watermark() { super("watermark", "hud.el.watermark", "logo", true, 160, 15); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) { return new float[]{sw - w - 6, 6}; }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            MinecraftClient mc = mc();
            String[] parts = {"RazerClient", mc.getCurrentFps() + " " + Lang.get("wm.fps"), LocalTime.now().format(TIME), mc.getSession().getUsername()};
            Font[] fonts = {Font.BOLD, Font.SEMIBOLD, Font.SEMIBOLD, Font.SEMIBOLD};
            float s = 0.7f, pad = 8, dotGap = 5, iconSize = 8.5f;
            float ww = pad + iconSize + 4;
            for (int i = 0; i < parts.length; i++) {
                ww += Fonts.width(parts[i], fonts[i], s);
                if (i < parts.length - 1) ww += dotGap * 2 + 2;
            }
            ww += pad + Fonts.width("2.0", Font.BOLD, 0.5f) + 3;
            w = ww; h = 17;
            panel(ctx, x, y, w, h, h / 2f);
            accentLine(ctx, x, y, w);
            float cx = x + pad;
            R2D.icon(ctx, "logo", cx, y + (h - iconSize) / 2f, iconSize, Theme.accentWave(0));
            cx += iconSize + 4;
            for (int i = 0; i < parts.length; i++) {
                Fonts.drawV(ctx, parts[i], cx, y + h / 2f + 0.5f, i == 0 ? Theme.text() : Theme.textDim(), fonts[i], s);
                cx += Fonts.width(parts[i], fonts[i], s);
                if (i == 0) {
                    Fonts.draw(ctx, "2.0", cx + 1.5f, y + 4.2f, Theme.accentWave(1), Font.BOLD, 0.5f);
                    cx += Fonts.width("2.0", Font.BOLD, 0.5f) + 3;
                }
                if (i < parts.length - 1) {
                    cx += dotGap;
                    R2D.circle(ctx, cx + 1, y + h / 2f, 0.9f, Theme.textFaint());
                    cx += 2 + dotGap;
                }
            }
        }
    }

    public class ModuleList extends HudElement {
        private final Anim aw = new Anim(84, 12), ah = new Anim(30, 12);
        ModuleList() { super("modules", "hud.el.modules", "list", true, 84, 30); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) { return new float[]{6, 6}; }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            RazerClient rc = RazerClient.get();
            List<Module> shown = new ArrayList<>();
            for (Module m : rc.modules.all()) if (m.hudAnim.animate(m.isEnabled() ? 1 : 0) > 0.01f) shown.add(m);
            shown.sort(Comparator.comparingDouble(m -> -Fonts.width(m.getName(), Font.SEMIBOLD, NAME)));
            float tw = 84;
            for (Module m : shown) tw = Math.max(tw, rowWidth(m.getName(), null));
            float contentH = 0;
            for (Module m : shown) contentH += ROW * Anim.easeOutCubic(m.hudAnim.raw());
            if (shown.isEmpty()) contentH = ROW;
            w = aw.animate(tw); h = ah.animate(HEAD + contentH + 4);

            panel(ctx, x, y, w, h);
            float cy = header(ctx, x, y, w, "list", Lang.get("hud.modules"), String.valueOf(rc.modules.enabledCount()));
            R2D.scissor(ctx, x, cy, w, h - HEAD);
            if (shown.isEmpty()) Fonts.drawV(ctx, Lang.get("hud.nomodules"), x + PADX, cy + ROW / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.6f);
            int i = 0;
            for (Module m : shown) {
                float a = Anim.easeOutCubic(m.hudAnim.raw());
                float prev = R2D.alpha;
                R2D.alpha = prev * a;
                float ox = (1 - a) * -12;
                R2D.icon(ctx, m.getIcon(), x + PADX + ox, cy + ROW / 2f - ICON / 2f, ICON, Theme.accentWave(i * 0.5f));
                Fonts.drawV(ctx, m.getName(), x + PADX + 11 + ox, cy + ROW / 2f + 0.5f, Theme.text(), Font.SEMIBOLD, NAME);
                R2D.alpha = prev;
                cy += ROW * a;
                i++;
            }
            R2D.endScissor(ctx);
        }
    }

    public class Keybinds extends HudElement {
        private final Anim aw = new Anim(84, 12), ah = new Anim(30, 12);
        Keybinds() { super("keybinds", "hud.el.keybinds", "keyboard", true, 84, 30); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) {
            HudElement ml = hud.moduleList;
            if (ml.enabled.get() && !ml.custom) return new float[]{6, ml.position(sw, sh, hud)[1] + ml.h + 4};
            return new float[]{6, 6};
        }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            RazerClient rc = RazerClient.get();
            List<Module> bound = new ArrayList<>();
            for (Module m : rc.modules.all()) if (m.hasKey()) bound.add(m);
            float tw = 84;
            for (Module m : bound) tw = Math.max(tw, rowWidth(m.getName(), Keys.name(m.getKey())));
            w = aw.animate(tw); h = ah.animate(HEAD + Math.max(1, bound.size()) * ROW + 4);

            panel(ctx, x, y, w, h);
            float cy = header(ctx, x, y, w, "keyboard", Lang.get("hud.keybinds"), String.valueOf(bound.size()));
            R2D.scissor(ctx, x, cy, w, h - HEAD);
            if (bound.isEmpty()) Fonts.drawV(ctx, Lang.get("hud.nobinds"), x + PADX, cy + ROW / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.6f);
            for (Module m : bound) {
                float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);
                row(ctx, x, cy, w, m.getIcon(), m.getName(), Keys.name(m.getKey()),
                        R2D.blend(Theme.textFaint(), Theme.accent(), on), R2D.blend(Theme.textFaint(), Theme.text(), on),
                        R2D.blend(Theme.textFaint(), Theme.textDim(), on));
                cy += ROW;
            }
            R2D.endScissor(ctx);
        }
    }

    /** TargetHUD – informacje o bycie, na którego patrzysz (tylko wyświetlanie) */
    public class TargetHud extends HudElement {
        private LivingEntity last;
        private long lastSeen;
        private final Anim health = new Anim(1, 10);
        TargetHud() { super("target", "hud.el.target", "targethud", true, 124, 38); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) { return new float[]{sw / 2f + 16, sh / 2f + 16}; }

        private LivingEntity current() {
            Entity e = mc().targetedEntity;
            if (e instanceof LivingEntity le && le.isAlive()) { last = le; lastSeen = System.currentTimeMillis(); }
            if (last != null && (!last.isAlive() || System.currentTimeMillis() - lastSeen > 1500)) last = null;
            return last;
        }

        @Override public boolean hasContent() { return current() != null; }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            LivingEntity t = current();
            if (t == null && preview) t = mc().player;
            w = 138; h = 44;
            float r = 12;
            panel(ctx, x, y, w, h, r);
            accentLine(ctx, x, y, w);
            if (t == null) return;
            // zaokrąglona głowa
            float hs = 30, hx = x + 7, hy = y + 7;
            R2D.roundRect(ctx, hx - 1, hy - 1, hs + 2, hs + 2, 9, Theme.toggleOff());
            if (t instanceof AbstractClientPlayerEntity p) {
                net.minecraft.util.Identifier skin = p.getSkinTextures().texture();
                R2D.roundTexture(ctx, skin, hx, hy, hs, hs, 8, 8 / 64f, 8 / 64f, 16 / 64f, 16 / 64f, 0xFFFFFFFF);
                R2D.roundTexture(ctx, skin, hx - 0.8f, hy - 0.8f, hs + 1.6f, hs + 1.6f, 8.5f, 40 / 64f, 8 / 64f, 48 / 64f, 16 / 64f, 0xFFFFFFFF);
            } else {
                R2D.icon(ctx, "targethud", hx + 8, hy + 8, 14, Theme.textDim());
            }
            // trafienie – czerwony błysk na głowie
            if (t.hurtTime > 0) R2D.roundRect(ctx, hx, hy, hs, hs, 8, R2D.withAlpha(0xFFFF3B3B, t.hurtTime / 10f * 0.45f));

            float tx = hx + hs + 8;
            float tw = x + w - 9 - tx;
            String name = Fonts.trim(t.getName().getString(), Font.BOLD, 0.78f, tw);
            Fonts.draw(ctx, name, tx, y + 8, Theme.text(), Font.BOLD, 0.78f);
            float hp = t.getHealth() + t.getAbsorptionAmount(), max = Math.max(1, t.getMaxHealth());
            float dist = mc().player != null && t != mc().player ? mc().player.distanceTo(t) : 0;
            // pigułki z informacjami
            String hpS = String.format(Locale.ROOT, "%.1f", hp), dS = String.format(Locale.ROOT, "%.1fm", dist);
            float px = tx;
            px = pill(ctx, px, y + 18.5f, "heart", hpS) + 4;
            pill(ctx, px, y + 18.5f, "coords", dS);
            // pasek zdrowia
            float pct = health.animate(Math.min(1, hp / max));
            float by = y + h - 9.5f, bh = 4;
            R2D.roundRect(ctx, tx, by, tw, bh, bh / 2f, Theme.toggleOff());
            int c1 = Theme.accentPreset() == Theme.Accent.MONO ? Theme.accent() : Theme.accentWave(0);
            int c2 = Theme.accentPreset() == Theme.Accent.MONO ? Theme.textDim() : Theme.accentWave(2);
            float fw = Math.max(bh, tw * pct);
            R2D.scissor(ctx, tx, by - 1, fw, bh + 2);
            R2D.roundRect(ctx, tx, by, fw, bh, bh / 2f, c1);
            R2D.gradientH(ctx, tx + bh / 2f, by, Math.max(0, fw - bh), bh, c1, R2D.blend(c1, c2, pct));
            R2D.circle(ctx, tx + fw - bh / 2f, by + bh / 2f, bh / 2f, R2D.blend(c1, c2, pct));
            R2D.endScissor(ctx);
        }

        private float pill(DrawContext ctx, float x, float y, String icon, String text) {
            float pw = Fonts.width(text, Font.SEMIBOLD, 0.56f) + 15, ph = 9.5f;
            R2D.roundRect(ctx, x, y, pw, ph, ph / 2f, Theme.toggleOff());
            R2D.icon(ctx, icon, x + 3.5f, y + 2.25f, 5, Theme.accentWave(0));
            Fonts.drawV(ctx, text, x + 11, y + ph / 2f + 0.4f, Theme.textDim(), Font.SEMIBOLD, 0.56f);
            return x + pw;
        }
    }

    public class Armor extends HudElement {
        Armor() { super("armor", "hud.el.armor", "shirt", false, 84, 24); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) { return new float[]{sw / 2f + 96, sh - h - 2}; }

        @Override public boolean hasContent() { return mc().player != null; }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            PlayerEntity p = mc().player;
            List<ItemStack> items = new ArrayList<>();
            if (p != null) {
                for (int i = 3; i >= 0; i--) items.add(p.getInventory().getArmorStack(i));
                items.add(p.getMainHandStack());
            }
            w = 6 + items.size() * 18 + 2; h = 24;
            if (items.isEmpty()) w = 84;
            panel(ctx, x, y, w, h);
            accentLine(ctx, x, y, w);
            for (int i = 0; i < items.size(); i++) {
                float ix = x + 5 + i * 18, iy = y + 4;
                ItemStack st = items.get(i);
                if (st.isEmpty()) { R2D.roundRect(ctx, ix + 2, iy + 2, 12, 12, 3, Theme.toggleOff()); continue; }
                ctx.draw();
                ctx.getMatrices().push();
                ctx.getMatrices().translate(ix, iy, 0);
                ctx.drawItem(st, 0, 0);
                ctx.drawItemInSlot(mc().textRenderer, st, 0, 0);
                ctx.getMatrices().pop();
                ctx.draw();
            }
        }
    }

    public class Potions extends HudElement {
        private final Anim ah = new Anim(30, 12);
        Potions() { super("potions", "hud.el.potions", "potion", true, 100, 30); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) {
            HudElement wm = hud.watermark;
            float top = wm.enabled.get() && !wm.custom ? wm.position(sw, sh, hud)[1] + wm.h + 4 : 6;
            return new float[]{sw - w - 6, top};
        }

        @Override public boolean hasContent() { return mc().player != null && !mc().player.getStatusEffects().isEmpty(); }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            List<String[]> rows = new ArrayList<>();
            List<StatusEffectInstance> effects = new ArrayList<>();
            if (mc().player != null) effects.addAll(mc().player.getStatusEffects());
            for (StatusEffectInstance e : effects) {
                String name = e.getEffectType().value().getName().getString() + (e.getAmplifier() > 0 ? " " + roman(e.getAmplifier() + 1) : "");
                String dur = e.isInfinite() ? "∞" : String.format("%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
                rows.add(new String[]{name, dur});
            }
            if (rows.isEmpty() && preview) rows.add(new String[]{Lang.pl() ? "Szybkość II" : "Speed II", "1:30"});
            float tw = 100;
            for (String[] r : rows) tw = Math.max(tw, rowWidth(r[0], r[1]));
            w = tw; h = ah.animate(HEAD + Math.max(1, rows.size()) * ROW + 4);
            panel(ctx, x, y, w, h);
            float cy = header(ctx, x, y, w, "potion", Lang.get("hud.potions"), String.valueOf(effects.size()));
            R2D.scissor(ctx, x, cy, w, h - HEAD);
            for (int i = 0; i < rows.size(); i++) {
                if (i < effects.size()) {
                    StatusEffectInstance e = effects.get(i);
                    ctx.draw();
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(x + PADX - 1, cy + ROW / 2f - 4.25f, 0);
                    ctx.getMatrices().scale(0.95f, 0.95f, 1);
                    RenderSystem.enableBlend();
                    ctx.drawSprite(0, 0, 0, 9, 9, mc().getStatusEffectSpriteManager().getSprite(e.getEffectType()), 1, 1, 1, R2D.alpha);
                    ctx.getMatrices().pop();
                    ctx.draw();
                    Fonts.drawV(ctx, rows.get(i)[0], x + PADX + 11, cy + ROW / 2f + 0.5f, Theme.text(), Font.SEMIBOLD, NAME);
                    Fonts.drawV(ctx, rows.get(i)[1], x + w - PADX - Fonts.width(rows.get(i)[1], Font.SEMIBOLD, NAME), cy + ROW / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, NAME);
                } else {
                    row(ctx, x, cy, w, "potion", rows.get(i)[0], rows.get(i)[1], Theme.accent(), Theme.text(), Theme.textDim());
                }
                cy += ROW;
            }
            R2D.endScissor(ctx);
        }

        private String roman(int n) {
            String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
            return n < r.length ? r[n] : String.valueOf(n);
        }
    }

    public class Coords extends HudElement {
        Coords() { super("coords", "hud.el.coords", "coords", true, 110, 40); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) { return new float[]{6, Math.max(sh * 0.45f, sh - h - 120)}; }

        @Override public boolean hasContent() { return mc().player != null; }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            PlayerEntity p = mc().player;
            String xyz = p == null ? "0, 64, 0" : String.format("%d, %d, %d", p.getBlockX(), p.getBlockY(), p.getBlockZ());
            String other = null, otherLabel = null;
            if (p != null && mc().world != null) {
                if (mc().world.getRegistryKey() == World.OVERWORLD) { otherLabel = "Nether"; other = String.format("%d, %d", p.getBlockX() / 8, p.getBlockZ() / 8); }
                else if (mc().world.getRegistryKey() == World.NETHER) { otherLabel = "Overworld"; other = String.format("%d, %d", p.getBlockX() * 8, p.getBlockZ() * 8); }
            }
            String facing = p == null ? "N" : dir(p.getHorizontalFacing().asString());
            float tw = Math.max(rowWidth("XYZ", xyz), rowWidth(Lang.get("hud.facing"), facing));
            if (other != null) tw = Math.max(tw, rowWidth(otherLabel, other));
            w = Math.max(100, tw);
            int rows = other != null ? 3 : 2;
            h = HEAD + rows * ROW + 4;
            panel(ctx, x, y, w, h);
            float cy = header(ctx, x, y, w, "coords", Lang.get("hud.coords"), null);
            row(ctx, x, cy, w, "move", "XYZ", xyz, Theme.accent(), Theme.textDim(), Theme.text()); cy += ROW;
            row(ctx, x, cy, w, "compass", Lang.get("hud.facing"), facing, Theme.accent(), Theme.textDim(), Theme.text()); cy += ROW;
            if (other != null) row(ctx, x, cy, w, "world", otherLabel, other, Theme.accent(), Theme.textDim(), Theme.text());
        }

        private String dir(String f) {
            boolean pl = Lang.pl();
            return switch (f) {
                case "north" -> pl ? "Północ (-Z)" : "North (-Z)";
                case "south" -> pl ? "Południe (+Z)" : "South (+Z)";
                case "west" -> pl ? "Zachód (-X)" : "West (-X)";
                default -> pl ? "Wschód (+X)" : "East (+X)";
            };
        }
    }

    public class Info extends HudElement {
        Info() { super("info", "hud.el.info", "activity", false, 100, 40); }

        @Override protected float[] defaultPos(int sw, int sh, Hud hud) {
            HudElement c = hud.coords;
            float bottom = c.enabled.get() && !c.custom ? c.position(sw, sh, hud)[1] - 4 : sh - 120;
            return new float[]{6, bottom - h};
        }

        @Override public boolean hasContent() { return mc().player != null; }

        @Override public void render(DrawContext ctx, float x, float y, boolean preview) {
            MinecraftClient mc = mc();
            int ping = 0;
            if (mc.getNetworkHandler() != null && mc.player != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            String server;
            if (mc.isInSingleplayer()) server = Lang.get("profile.singleplayer");
            else { ServerInfo si = mc.getCurrentServerEntry(); server = si != null ? si.address : "-"; }
            long sess = (System.currentTimeMillis() - RazerClient.get().startTime) / 1000;
            String[][] rows = {
                    {"wifi", "Ping", ping + " ms"},
                    {"gauge", Lang.get("hud.speed"), String.format(Locale.ROOT, "%.1f b/s", speed)},
                    {"world", Lang.get("profile.server"), server},
                    {"clock", Lang.get("profile.session"), String.format("%02d:%02d", sess / 3600, (sess / 60) % 60)},
            };
            float tw = 100;
            for (String[] r : rows) tw = Math.max(tw, rowWidth(r[1], r[2]));
            w = tw; h = HEAD + rows.length * ROW + 4;
            panel(ctx, x, y, w, h);
            float cy = header(ctx, x, y, w, "activity", Lang.get("hud.info"), null);
            for (String[] r : rows) { row(ctx, x, cy, w, r[0], r[1], r[2], Theme.accent(), Theme.textDim(), Theme.text()); cy += ROW; }
        }
    }
}
