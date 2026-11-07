package pl.mysticclient.module.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.ModeSetting;
import pl.mysticclient.setting.NumberSetting;
import pl.mysticclient.util.FriendManager;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;
import pl.mysticclient.util.UI.Weight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Nowoczesne tabliczki nad głowami.
 * Pozycja głowy jest rzutowana na ekran w czasie renderu świata, a sama tabliczka rysowana
 * rendererem UI (czcionka Inter, zaokrąglenia, cień) – ostra i czytelna z każdej odległości.
 */
public class NameTags extends Module {
    public final BoolSetting players = add(new BoolSetting("Gracze", true));
    public final BoolSetting mobs = add(new BoolSetting("Moby", false));
    public final BoolSetting healthBar = add(new BoolSetting("Pasek zdrowia", true));
    public final ModeSetting healthColor = add(new ModeSetting("Kolor zdrowia", "Kolorowy", "Kolorowy", "Biały")).visibleWhen(healthBar::get);
    public final BoolSetting itemStack = add(new BoolSetting("Przedmiot w ręce", true));
    public final BoolSetting armor = add(new BoolSetting("Zbroja", true));
    public final BoolSetting distance = add(new BoolSetting("Dystans", false));
    public final BoolSetting background = add(new BoolSetting("Tło", true));
    public final NumberSetting scale = add(new NumberSetting("Skala", 1.0, 0.5, 2.0, 0.05));
    public final NumberSetting range = add(new NumberSetting("Zasięg", 64.0, 16.0, 256.0, 4.0));

    private record Tag(LivingEntity entity, float x, float y, double dist) {}

    private final List<Tag> tags = new ArrayList<>();

    public NameTags() {
        super("NameTags", "Nowoczesne, ładne i czytelne tabliczki nad głowami.", Category.RENDER);
    }

    public boolean isValidType(Entity e) {
        if (!(e instanceof LivingEntity) || e == mc.player) return false;
        if (e instanceof PlayerEntity p) return players.get() && !p.isSpectator();
        return mobs.get();
    }

    /** Używane przez mixin – ukrywa waniliowy nick, gdy rysujemy własny. */
    public boolean replacesVanilla(Entity e) {
        return isEnabled() && isValidType(e) && mc.player != null && mc.player.distanceTo(e) <= range.get();
    }

    // ---------------------------------------------------------------- 3D: rzutowanie pozycji

    @Override
    public void onRender3D(WorldRenderContext ctx) {
        tags.clear();
        if (nullCheck()) return;
        float tickDelta = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        Matrix4f viewProj = new Matrix4f(ctx.projectionMatrix()).mul(ctx.positionMatrix());
        float fbW = mc.getWindow().getFramebufferWidth(), fbH = mc.getWindow().getFramebufferHeight();

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidType(entity)) continue;
            LivingEntity living = (LivingEntity) entity;
            if (!living.isAlive() || living.isDead() || living.getHealth() <= 0) continue;

            double dist = mc.player.distanceTo(living);
            if (dist > range.get()) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - cam.x;
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - cam.y + entity.getHeight() + 0.35;
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - cam.z;

            Vector4f clip = new Vector4f((float) x, (float) y, (float) z, 1f).mul(viewProj);
            if (clip.w <= 0.05f) continue; // za kamerą
            float ndcX = clip.x / clip.w, ndcY = clip.y / clip.w;
            if (Math.abs(ndcX) > 1.3f || Math.abs(ndcY) > 1.3f) continue;
            float sx = (ndcX + 1f) / 2f * fbW;
            float sy = (1f - ndcY) / 2f * fbH;
            tags.add(new Tag(living, sx, sy, dist));
        }
        // dalsze rysujemy najpierw, żeby bliższe były na wierzchu
        tags.sort(Comparator.comparingDouble((Tag t) -> t.dist).reversed());
    }

    // ---------------------------------------------------------------- 2D: rysowanie

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (nullCheck() || tags.isEmpty()) return;
        for (Tag t : tags) {
            if (t.entity.isRemoved()) continue;
            // piksele -> dp; mniejsze z odległością, ale zawsze czytelne
            float f = (float) (scale.get() * MathHelper.clamp(1.25 - t.dist / 60.0, 0.7, 1.15));
            drawTag(ctx, t.entity, t.x / UI.u, t.y / UI.u, f, t.dist);
        }
    }

    private void drawTag(DrawContext ctx, LivingEntity entity, float cx, float bottomY, float f, double dist) {
        String name = entity.getName().getString();
        boolean friend = entity instanceof PlayerEntity && FriendManager.isFriend(name);

        float hp = entity.getHealth() + entity.getAbsorptionAmount();
        float maxHp = entity.getMaxHealth();
        float healthPct = MathHelper.clamp(entity.getHealth() / maxHp, 0f, 1f);
        String hpText = String.format(Locale.ROOT, "%.1f", hp);
        String distText = distance.get() ? String.format(Locale.ROOT, "%.0fm", dist) : null;

        float nameSize = 13 * f, smallSize = 11.5f * f;
        float padX = 9 * f, gap = 7 * f;
        float badgeW = friend ? UI.width("F", 10 * f, Weight.SEMIBOLD) + 10 * f : 0;
        float nameW = UI.width(name, nameSize, Weight.SEMIBOLD);
        float hpW = UI.width(hpText, smallSize, Weight.SEMIBOLD) + 11 * f; // + serduszko
        float distW = distText != null ? UI.width(distText, smallSize, Weight.MEDIUM) + gap : 0;

        float w = padX * 2 + (friend ? badgeW + gap : 0) + nameW + gap + hpW + distW;
        float h = (healthBar.get() ? 28 : 24) * f;
        float x = cx - w / 2f, y = bottomY - h;

        if (background.get()) {
            UI.shadow(x, y + 2 * f, w, h, 8 * f, 8 * f, UI.withAlpha(0, Theme.light ? 30 : 80));
            UI.round(x, y, w, h, 8 * f, Theme.glass());
            UI.outline(x, y, w, h, 8 * f, 1, Theme.glassBorder());
        }

        float midY = y + (healthBar.get() ? 12 * f : h / 2f);
        float tx = x + padX;
        if (friend) {
            UI.round(tx, midY - 7 * f, badgeW, 14 * f, 4 * f, Theme.accent());
            UI.textCenter("F", tx + badgeW / 2f, midY, 10 * f, Weight.SEMIBOLD, Theme.onAccent());
            tx += badgeW + gap;
        }
        UI.textMid(name, tx, midY, nameSize, Weight.SEMIBOLD, Theme.text());
        tx += nameW + gap;
        int hpCol = healthColor.is("Biały") ? Theme.text() : getHealthColor(healthPct);
        UI.textMid(hpText, tx, midY, smallSize, Weight.SEMIBOLD, hpCol);
        UI.icon(UI.Icons.HEART, tx + UI.width(hpText, smallSize, Weight.SEMIBOLD) + 6.5f * f, midY, 10 * f, hpCol);
        tx += hpW;
        if (distText != null) {
            tx += gap;
            UI.textMid(distText, tx, midY, smallSize, Weight.MEDIUM, Theme.dim());
        }

        if (healthBar.get()) {
            float barX = x + padX, barW = w - padX * 2, barY = y + h - 7 * f, barH = 2.5f * f;
            UI.round(barX, barY, barW, barH, barH / 2f, UI.a(Theme.text(), 0.15f));
            UI.round(barX, barY, Math.max(barH, barW * healthPct), barH, barH / 2f, hpCol);
        }

        // przedmioty nad tabliczką: zbroja + przedmiot w ręce
        List<ItemStack> items = new ArrayList<>();
        if (itemStack.get() && !entity.getMainHandStack().isEmpty()) items.add(entity.getMainHandStack());
        if (armor.get()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack s = entity.getEquippedStack(slot);
                if (!s.isEmpty()) items.add(s);
            }
        }
        if (itemStack.get() && !entity.getOffHandStack().isEmpty()) items.add(entity.getOffHandStack());
        if (!items.isEmpty()) {
            float itemSize = 18 * f, itemGap = 2 * f;
            float total = items.size() * itemSize + (items.size() - 1) * itemGap;
            float ix = cx - total / 2f, iy = y - itemSize - 4 * f;
            for (ItemStack s : items) {
                var ms = ctx.getMatrices();
                ms.push();
                ms.translate(ix, iy, 0);
                ms.scale(itemSize / 16f, itemSize / 16f, 1f);
                ctx.drawItem(s, 0, 0);
                ms.pop();
                if (s.isDamageable()) {
                    float pct = 1f - s.getDamage() / (float) s.getMaxDamage();
                    UI.round(ix + 2 * f, iy + itemSize, itemSize - 4 * f, 2 * f, f, UI.a(Theme.text(), 0.2f));
                    UI.round(ix + 2 * f, iy + itemSize, Math.max(2 * f, (itemSize - 4 * f) * pct), 2 * f, f, Theme.text());
                }
                ix += itemSize + itemGap;
            }
        }
    }

    private int getHealthColor(float pct) {
        if (pct > 0.6f) return 0xFF2ECC71;
        if (pct > 0.3f) return 0xFFF1C40F;
        return 0xFFE74C3C;
    }
}
