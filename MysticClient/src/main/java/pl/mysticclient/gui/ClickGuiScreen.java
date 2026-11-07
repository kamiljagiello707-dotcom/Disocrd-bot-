package pl.mysticclient.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import pl.mysticclient.MysticClient;
import pl.mysticclient.config.ConfigManager;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.misc.ClickGUI;
import pl.mysticclient.module.misc.ClientSettings;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.ModeSetting;
import pl.mysticclient.setting.NumberSetting;
import pl.mysticclient.setting.Setting;
import pl.mysticclient.util.NotificationManager;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;
import pl.mysticclient.util.UI.Icons;
import pl.mysticclient.util.UI.Weight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

public class ClickGuiScreen extends Screen {
    private enum Tab { CATEGORY, CONFIGS, THEME }

    // stan zapamiętany między otwarciami
    private static Tab tab = Tab.CATEGORY;
    private static Category category = Category.COMBAT;
    private static Module selected;

    // wymiary okna w dp
    private static final float W = 980, H = 600, SIDEBAR = 216, HEADER = 76, FOOTER = 44;

    private record Region(float x, float y, float w, float h, IntConsumer action) {}

    private final List<Region> regions = new ArrayList<>();
    private String search = "";
    private boolean searchFocused;
    private String configName = "";
    private boolean configFocused;
    private Module binding;
    private NumberSetting dragging;
    private float dragX, dragW;
    private float moduleScroll, moduleScrollTarget, settingsScroll, settingsScrollTarget;
    private float moduleContentH, moduleViewH, settingsContentH, settingsViewH;
    private float listX, listY, listW, listH, detX, detY, detW, detH;
    private float openAnim;
    private boolean closing;

    private float wx, wy;
    private float mx, my; // mysz w dp
    private float animScale = 1f;

    public ClickGuiScreen() {
        super(Text.literal("MysticClient"));
    }

    private static ClientSettings cs() { return ModuleManager.get(ClientSettings.class); }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        openAnim = cs().guiAnimations.get() ? 0f : 1f;
        closing = false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null && cs().guiBlur.get()) applyBlur();
        int a = (int) (openAnim * 255);
        ctx.fillGradient(0, 0, width, height, UI.withAlpha(0x000000, (int) (a * 0.55f)), UI.withAlpha(0x000000, (int) (a * 0.75f)));
    }

    // =================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        UI.Frame.tick();
        openAnim = UI.approach(openAnim, closing ? 0f : 1f, closing ? 18f : 11f);
        if (closing && openAnim < 0.02f) { super.close(); return; }
        renderBackground(ctx, mouseX, mouseY, delta);
        regions.clear();

        float scale = UI.autoScale() * 1.1f * cs().guiScale.getFloat();
        float realW = client.getWindow().getFramebufferWidth(), realH = client.getWindow().getFramebufferHeight();
        scale = Math.min(scale, Math.min(realW / (W + 32), realH / (H + 32)));
        UI.begin(ctx, scale);

        float sw = UI.screenW(), sh = UI.screenH();
        wx = (sw - W) / 2f;
        wy = (sh - H) / 2f;

        float ease = 1f - (float) Math.pow(1f - openAnim, 3);
        animScale = 0.96f + 0.04f * ease;
        float cx = sw / 2f, cy = sh / 2f;
        mx = cx + (UI.dp(mouseX) - cx) / animScale;
        my = cy + (UI.dp(mouseY) - cy) / animScale;

        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0);
        ms.scale(animScale, animScale, 1);
        ms.translate(-cx, -cy, 0);
        UI.alpha = ease;

        // okno
        UI.shadow(wx, wy + 14, W, H, 20, 46, Theme.shadow());
        UI.round(wx, wy, W, H, 18, Theme.bg());
        UI.outline(wx, wy, W, H, 18, 1, Theme.border());

        drawSidebar();
        drawHeader();
        switch (tab) {
            case CATEGORY -> drawModules();
            case CONFIGS -> drawConfigs();
            case THEME -> drawTheme();
        }
        drawFooter();

        ms.pop();
        UI.end();

        moduleScroll = UI.approach(moduleScroll, moduleScrollTarget, 18f);
        settingsScroll = UI.approach(settingsScroll, settingsScrollTarget, 18f);
        if (dragging != null) {
            double pct = Math.max(0, Math.min(1, (mx - dragX) / dragW));
            dragging.set(dragging.getMin() + (dragging.getMax() - dragging.getMin()) * pct);
        }
    }

    private void click(float x, float y, float w, float h, IntConsumer action) {
        regions.add(new Region(x, y, w, h, action));
    }

    private void clickClipped(float x, float y, float w, float h, float top, float bottom, IntConsumer action) {
        float t = Math.max(y, top), b = Math.min(y + h, bottom);
        if (b > t) regions.add(new Region(x, t, w, b - t, action));
    }

    private boolean hov(float x, float y, float w, float h) { return UI.hover(mx, my, x, y, w, h); }

    // =================================================================== sidebar

    private void drawSidebar() {
        float x = wx, y = wy;
        UI.round(x + 1, y + 1, SIDEBAR - 1, H - 2, 17, Theme.surface());
        UI.rect(x + SIDEBAR - 18, y + 1, 18, H - 2, Theme.surface());
        UI.rect(x + SIDEBAR, y + 1, 1, H - 2, Theme.border());

        // logo
        float lx = x + 20, ly = y + 22;
        UI.round(lx, ly, 34, 34, 10, Theme.accent());
        UI.icon(Icons.SPARKLES, lx + 17, ly + 17, 18, Theme.onAccent());
        UI.text("MysticClient", lx + 46, ly + 1, 15, Weight.SEMIBOLD, Theme.text());
        UI.text("Wersja " + MysticClient.VERSION_FULL, lx + 46, ly + 19, 11.5f, Weight.REGULAR, Theme.dim());

        float iy = y + 82;
        UI.text("MODUŁY", x + 22, iy, 10.5f, Weight.SEMIBOLD, Theme.dim());
        iy += 22;
        char[] icons = {Icons.SWORDS, Icons.FOOTPRINTS, Icons.EYE, Icons.USER, Icons.GLOBE, Icons.BOXES};
        for (int i = 0; i < Category.values().length; i++) {
            Category c = Category.values()[i];
            long on = ModuleManager.getByCategory(c).stream().filter(Module::isEnabled).count();
            boolean active = tab == Tab.CATEGORY && category == c && search.isEmpty();
            sideItem("cat" + i, icons[i], c.displayName, on > 0 ? String.valueOf(on) : null, x + 12, iy, active, b -> {
                tab = Tab.CATEGORY;
                category = c;
                search = "";
                searchFocused = false;
                selected = null;
                moduleScrollTarget = settingsScrollTarget = 0;
            });
            iy += 38;
        }
        iy += 10;
        UI.text("KLIENT", x + 22, iy, 10.5f, Weight.SEMIBOLD, Theme.dim());
        iy += 22;
        sideItem("configs", Icons.FOLDER, "Configi", null, x + 12, iy, tab == Tab.CONFIGS, b -> tab = Tab.CONFIGS);
        iy += 38;
        sideItem("theme", Icons.PALETTE, "Wygląd", null, x + 12, iy, tab == Tab.THEME, b -> tab = Tab.THEME);

        // profil
        float py = y + H - 66;
        UI.round(x + 12, py, SIDEBAR - 24, 52, 12, Theme.surface2());
        UI.outline(x + 12, py, SIDEBAR - 24, 52, 12, 1, Theme.border());
        String name = client.getSession().getUsername();
        UI.circle(x + 40, py + 26, 16, Theme.accent());
        UI.textCenter(name.substring(0, 1).toUpperCase(Locale.ROOT), x + 40, py + 26, 14, Weight.SEMIBOLD, Theme.onAccent());
        UI.text(UI.ellipsis(name, SIDEBAR - 90, 13, Weight.MEDIUM), x + 64, py + 10, 13, Weight.MEDIUM, Theme.text());
        UI.text(keyName(ModuleManager.get(ClickGUI.class).getKey()) + " · menu", x + 64, py + 28, 11, Weight.REGULAR, Theme.dim());
    }

    private void sideItem(String key, char icon, String label, String badge, float x, float y, boolean active, IntConsumer action) {
        float w = SIDEBAR - 24, h = 34;
        float hv = UI.anim("side_" + key, hov(x, y, w, h) && !active);
        float ac = UI.anim("sideA_" + key, active);
        if (ac > 0.01f) UI.round(x, y, w, h, 9, UI.a(Theme.surface3(), ac));
        else if (hv > 0.01f) UI.round(x, y, w, h, 9, UI.a(Theme.surface2(), hv));
        int c = UI.mix(UI.mix(Theme.sub(), Theme.text(), hv), Theme.text(), ac);
        UI.icon(icon, x + 20, y + h / 2f, 16, c);
        UI.textMid(label, x + 38, y + h / 2f, 13, active ? Weight.SEMIBOLD : Weight.MEDIUM, c);
        if (badge != null) {
            float bw = Math.max(20, UI.width(badge, 10.5f, Weight.SEMIBOLD) + 12);
            UI.round(x + w - bw - 8, y + 8, bw, 18, 9, active ? Theme.accent() : Theme.surface3());
            UI.textCenter(badge, x + w - bw / 2f - 8, y + 17, 10.5f, Weight.SEMIBOLD, active ? Theme.onAccent() : Theme.sub());
        }
        click(x, y, w, h, action);
    }

    // =================================================================== header / footer

    private void drawHeader() {
        float x = wx + SIDEBAR + 28, y = wy + 20;
        String title;
        String subtitle;
        if (tab == Tab.CATEGORY) {
            List<Module> mods = visibleModules();
            long on = mods.stream().filter(Module::isEnabled).count();
            title = search.isEmpty() ? category.displayName : "Wyniki wyszukiwania";
            subtitle = mods.size() + " " + plural(mods.size(), "moduł", "moduły", "modułów") + " · " + on + " " + plural((int) on, "aktywny", "aktywne", "aktywnych");
        } else if (tab == Tab.CONFIGS) {
            title = "Configi";
            subtitle = "Zapisuj i wczytuj swoje ustawienia";
        } else {
            title = "Wygląd";
            subtitle = "Motyw, skala i animacje interfejsu";
        }
        UI.text(title, x, y, 22, Weight.SEMIBOLD, Theme.text());
        UI.text(subtitle, x, y + 30, 12, Weight.REGULAR, Theme.sub());

        // wyszukiwarka
        float sw = 270, sh = 38;
        float sx = wx + W - sw - 24, sy = wy + 19;
        float f = UI.anim("searchFocus", searchFocused);
        float hv = UI.anim("searchHover", hov(sx, sy, sw, sh));
        UI.round(sx, sy, sw, sh, 10, UI.mix(Theme.surface(), Theme.surface2(), Math.max(hv, f)));
        UI.outline(sx, sy, sw, sh, 10, 1, UI.mix(Theme.border(), Theme.borderHi(), Math.max(f, hv * 0.5f)));
        UI.icon(Icons.SEARCH, sx + 19, sy + sh / 2f, 15, Theme.dim());
        boolean empty = search.isEmpty();
        String shown = empty ? "Szukaj modułu..." : search;
        UI.textMid(shown, sx + 36, sy + sh / 2f, 13, Weight.REGULAR, empty ? Theme.dim() : Theme.text());
        if (searchFocused && (System.currentTimeMillis() / 530) % 2 == 0) {
            float cxp = sx + 36 + (empty ? 0 : UI.width(search, 13) + 1);
            UI.rect(cxp, sy + 11, 1.5f, sh - 22, Theme.text());
        }
        if (empty && !searchFocused) {
            float kw = UI.width("Ctrl F", 10, Weight.MEDIUM) + 12;
            UI.round(sx + sw - kw - 9, sy + 9, kw, 20, 6, Theme.surface3());
            UI.textCenter("Ctrl F", sx + sw - kw / 2f - 9, sy + 19, 10, Weight.MEDIUM, Theme.dim());
        }
        click(sx, sy, sw, sh, b -> {
            searchFocused = true;
            configFocused = false;
            if (b == 1) search = "";
            tab = Tab.CATEGORY;
        });

        UI.rect(wx + SIDEBAR + 1, wy + HEADER, W - SIDEBAR - 2, 1, Theme.border());
    }

    private void drawFooter() {
        float y = wy + H - FOOTER;
        UI.rect(wx + SIDEBAR + 1, y, W - SIDEBAR - 2, 1, Theme.border());
        float x = wx + SIDEBAR + 28, cy = y + FOOTER / 2f;
        UI.circle(x + 3, cy, 3.5f, Theme.text());
        UI.circle(x + 3, cy, 7, UI.a(Theme.text(), 0.12f));
        long on = ModuleManager.getModules().stream().filter(m -> m.isEnabled() && m.showInArrayList()).count();
        long total = ModuleManager.getModules().stream().filter(Module::showInArrayList).count();
        UI.textMid(on + " / " + total + " modułów aktywnych", x + 16, cy, 11.5f, Weight.MEDIUM, Theme.sub());

        String cfg = "Config: " + ConfigManager.getCurrent();
        UI.textCenter(cfg, wx + SIDEBAR + (W - SIDEBAR) / 2f, cy, 11.5f, Weight.MEDIUM, Theme.dim());

        String key = keyName(ModuleManager.get(ClickGUI.class).getKey());
        float kw = UI.width(key, 10.5f, Weight.SEMIBOLD) + 14;
        float kx = wx + W - 28 - kw;
        UI.round(kx, cy - 10, kw, 20, 6, Theme.surface2());
        UI.outline(kx, cy - 10, kw, 20, 6, 1, Theme.border());
        UI.textCenter(key, kx + kw / 2f, cy, 10.5f, Weight.SEMIBOLD, Theme.sub());
        String brand = MysticClient.NAME + " " + MysticClient.VERSION_FULL;
        UI.textRight(brand, kx - 10, cy, 11.5f, Weight.MEDIUM, Theme.dim());
    }

    // =================================================================== moduły

    private List<Module> visibleModules() {
        if (!search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            return ModuleManager.getModules().stream()
                    .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        }
        return ModuleManager.getByCategory(category).stream()
                .sorted(Comparator.comparing((Module m) -> !m.favorite))
                .toList();
    }

    private void drawModules() {
        float bx = wx + SIDEBAR + 24, by = wy + HEADER + 20;
        float bw = W - SIDEBAR - 48, bh = H - HEADER - FOOTER - 40;
        listX = bx; listY = by; listW = 300; listH = bh;
        detX = bx + listW + 16; detY = by; detW = bw - listW - 16; detH = bh;

        List<Module> mods = visibleModules();
        if (selected == null || !mods.contains(selected)) selected = mods.isEmpty() ? null : mods.get(0);

        float top = listY, bottom = listY + listH;
        float rowH = 62, gap = 8;
        moduleViewH = listH;
        moduleContentH = mods.size() * (rowH + gap) - gap;
        clampScroll();

        UI.scissor(listX - 4, top, listW + 8, listH);
        float y = top - moduleScroll;
        for (Module m : mods) {
            if (y + rowH >= top && y <= bottom) moduleCard(m, listX, y, listW, rowH, top, bottom);
            y += rowH + gap;
        }
        if (mods.isEmpty()) {
            UI.icon(Icons.SEARCH, listX + listW / 2f, top + 60, 28, Theme.dim());
            UI.textCenter("Nic nie znaleziono", listX + listW / 2f, top + 96, 13, Weight.MEDIUM, Theme.sub());
        }
        UI.endScissor();
        // zanikanie przy krawędziach listy
        if (moduleScroll > 1) UI.gradientV(listX - 4, top, listW + 8, 16, Theme.bg(), Theme.bg() & 0xFFFFFF);
        if (moduleScroll < moduleContentH - moduleViewH - 1) UI.gradientV(listX - 4, bottom - 16, listW + 8, 16, Theme.bg() & 0xFFFFFF, Theme.bg());

        // panel szczegółów
        UI.round(detX, detY, detW, detH, 14, Theme.surface());
        UI.outline(detX, detY, detW, detH, 14, 1, Theme.border());
        if (selected != null) drawDetail(selected);
    }

    private void moduleCard(Module m, float x, float y, float w, float h, float top, float bottom) {
        boolean sel = m == selected;
        float hv = UI.anim("mod_" + m.getName(), hov(x, y, w, h) && my >= top && my < bottom);
        float sa = UI.anim("modSel_" + m.getName(), sel);
        UI.round(x, y, w, h, 12, UI.mix(UI.mix(Theme.surface(), Theme.surface2(), hv), Theme.surface2(), sa));
        UI.outline(x, y, w, h, 12, 1, UI.mix(Theme.border(), Theme.borderHi(), Math.max(sa, hv * 0.4f)));
        if (sa > 0.01f) UI.round(x + 1, y + 16, 3, h - 32, 1.5f, UI.a(Theme.accent(), sa));

        float tx = x + 18;
        UI.text(m.getName(), tx, y + 13, 13.5f, Weight.SEMIBOLD, Theme.text());
        float nameEnd = tx + UI.width(m.getName(), 13.5f, Weight.SEMIBOLD);
        if (m.favorite) {
            UI.icon(Icons.STAR, nameEnd + 11, y + 21, 11, Theme.sub());
            nameEnd += 18;
        }
        if (m.getKey() > 0) {
            String k = keyName(m.getKey());
            float kw = UI.width(k, 9.5f, Weight.SEMIBOLD) + 10;
            UI.round(nameEnd + 7, y + 13, kw, 16, 5, Theme.surface3());
            UI.textCenter(k, nameEnd + 7 + kw / 2f, y + 21, 9.5f, Weight.SEMIBOLD, Theme.sub());
        }
        UI.text(UI.ellipsis(m.getDescription(), w - 90, 11.5f, Weight.REGULAR), tx, y + 34, 11.5f, Weight.REGULAR, Theme.dim());

        m.toggleAnim = UI.approach(m.toggleAnim, m.isEnabled() ? 1f : 0f, 16f);
        float sx = x + w - 18 - 38, sy = y + h / 2f - 11;
        toggleSwitch(sx, sy, 38, 22, m.toggleAnim);
        clickClipped(sx - 4, sy - 4, 46, 30, top, bottom, b -> m.toggle());
        clickClipped(x, y, w, h, top, bottom, b -> {
            if (b == 1) m.toggle();
            else if (b == 2) binding = m;
            else if (selected != m) { selected = m; settingsScrollTarget = 0; settingsScroll = 0; }
        });
    }

    private void drawDetail(Module m) {
        float px = detX + 24, py = detY + 22;

        // przyciski w prawym górnym rogu (najpierw, żeby wiedzieć ile miejsca zostaje na nazwę)
        float bx = detX + detW - 24 - 46, by = py + 1;
        m.toggleAnim = UI.approach(m.toggleAnim, m.isEnabled() ? 1f : 0f, 16f);
        toggleSwitch(bx, by + 2, 46, 26, m.toggleAnim);
        click(bx, by, 46, 30, b -> m.toggle());

        boolean isBinding = binding == m;
        String bind = isBinding ? "Naciśnij…" : keyName(m.getKey());
        float bindW = UI.width(bind, 12, Weight.MEDIUM) + 40;
        float kx = bx - 10 - bindW;
        iconButton("bind", kx, by, bindW, 30, Icons.KEYBOARD, bind, isBinding, b -> binding = (b == 1 || isBinding) ? null : m);
        float rx = kx - 8 - 30;
        iconButton("reset", rx, by, 30, 30, Icons.RESET, null, false, b -> {
            m.getSettings().forEach(Setting::reset);
            NotificationManager.push(m.getName(), "Przywrócono domyślne ustawienia", true);
        });

        float avail = rx - 14 - px;
        String chip = m.getCategory().displayName;
        float chipW = UI.width(chip, 11, Weight.MEDIUM) + 16;
        float titleSize = 20;
        while (titleSize > 15 && UI.width(m.getName(), titleSize, Weight.SEMIBOLD) > avail) titleSize -= 0.5f;
        boolean showChip = UI.width(m.getName(), titleSize, Weight.SEMIBOLD) + 10 + chipW <= avail;
        String title = UI.ellipsis(m.getName(), avail, titleSize, Weight.SEMIBOLD);
        UI.textMid(title, px, py + 14, titleSize, Weight.SEMIBOLD, Theme.text());
        if (showChip) {
            float chipX = px + UI.width(title, titleSize, Weight.SEMIBOLD) + 10;
            UI.round(chipX, py + 3, chipW, 22, 11, Theme.surface3());
            UI.textCenter(chip, chipX + chipW / 2f, py + 14, 11, Weight.MEDIUM, Theme.sub());
        }

        float descMax = detW - 48;
        UI.text(UI.ellipsis(m.getDescription(), descMax, 12.5f, Weight.REGULAR), px, py + 36, 12.5f, Weight.REGULAR, Theme.sub());

        UI.rect(detX + 1, detY + 84, detW - 2, 1, Theme.border());

        float top = detY + 85, bottom = detY + detH - 2;
        settingsViewH = bottom - top;
        UI.scissor(detX + 1, top, detW - 2, settingsViewH);
        float y = top + 18 - settingsScroll;
        float startY = y;
        float sx = detX + 24, sw = detW - 48;

        List<Setting<?>> visible = m.getSettings().stream().filter(Setting::isVisible).toList();
        if (visible.isEmpty()) {
            UI.icon(Icons.SLIDERS, detX + detW / 2f, top + 70, 30, Theme.dim());
            UI.textCenter("Ten moduł nie ma ustawień", detX + detW / 2f, top + 108, 13.5f, Weight.MEDIUM, Theme.sub());
            UI.textCenter("PPM na liście = przełącz  ·  ŚPM = ustaw klawisz", detX + detW / 2f, top + 130, 11.5f, Weight.REGULAR, Theme.dim());
        } else {
            UI.text("USTAWIENIA", sx, y, 10.5f, Weight.SEMIBOLD, Theme.dim());
            y += 22;
            y = drawSettingsGroup(visible, sx, y, sw, top, bottom, m.getName());
        }
        settingsContentH = y - startY + 18;
        clampScroll();
        UI.endScissor();
    }

    /** Grupa ustawień w jednym kontenerze z separatorami (jak w nowoczesnych aplikacjach). */
    private float drawSettingsGroup(List<Setting<?>> settings, float sx, float y, float sw, float top, float bottom, String keyPrefix) {
        float total = 0;
        for (Setting<?> s : settings) total += rowHeight(s);
        UI.round(sx, y, sw, total, 12, Theme.bg());
        UI.outline(sx, y, sw, total, 12, 1, Theme.border());
        float ry = y;
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            float rh = rowHeight(s);
            if (i > 0) UI.rect(sx + 16, ry, sw - 32, 1, Theme.border());
            float ix = sx + 16, iw = sw - 32;
            if (s instanceof BoolSetting b) {
                UI.textMid(s.getName(), ix, ry + rh / 2f, 13, Weight.MEDIUM, Theme.text());
                b.anim = UI.approach(b.anim, b.get() ? 1f : 0f, 16f);
                toggleSwitch(ix + iw - 38, ry + rh / 2f - 11, 38, 22, b.anim);
                clickClipped(sx, ry, sw, rh, top, bottom, k -> b.toggle());
            } else if (s instanceof ModeSetting md) {
                UI.textMid(s.getName(), ix, ry + 22, 13, Weight.MEDIUM, Theme.text());
                UI.textRight(md.get(), ix + iw, ry + 22, 12, Weight.REGULAR, Theme.dim());
                float segY = ry + 38, segH = 32;
                UI.round(ix, segY, iw, segH, 9, Theme.surface2());
                List<String> modes = md.getModes();
                float segW = (iw - 6) / modes.size();
                int activeIdx = Math.max(0, modes.indexOf(md.get()));
                String key = keyPrefix + "_seg_" + s.getName();
                float pos = UI.anim(key, activeIdx, 18f);
                float ax = ix + 3 + pos * segW;
                UI.shadow(ax, segY + 4, segW, segH - 6, 7, 6, UI.withAlpha(0, Theme.light ? 25 : 70));
                UI.round(ax, segY + 3, segW, segH - 6, 7, Theme.accent());
                for (int i2 = 0; i2 < modes.size(); i2++) {
                    String mode = modes.get(i2);
                    float segX = ix + 3 + i2 * segW;
                    float closeness = Math.max(0, 1f - Math.abs(pos - i2));
                    if (closeness < 0.5f && hov(segX, segY, segW, segH)) UI.round(segX, segY + 3, segW, segH - 6, 7, Theme.surface3());
                    float fs = 12;
                    while (fs > 9 && UI.width(mode, fs, Weight.MEDIUM) > segW - 10) fs -= 0.5f;
                    UI.textCenter(UI.ellipsis(mode, segW - 8, fs, Weight.MEDIUM), segX + segW / 2f, segY + segH / 2f, fs, Weight.MEDIUM, UI.mix(Theme.sub(), Theme.onAccent(), closeness));
                    clickClipped(segX, segY, segW, segH, top, bottom, k -> md.set(mode));
                }
            } else if (s instanceof NumberSetting n) {
                UI.textMid(s.getName(), ix, ry + 22, 13, Weight.MEDIUM, Theme.text());
                String val = n.display();
                float vw = Math.max(36, UI.width(val, 11.5f, Weight.SEMIBOLD) + 16);
                UI.round(ix + iw - vw, ry + 11, vw, 22, 7, Theme.surface2());
                UI.outline(ix + iw - vw, ry + 11, vw, 22, 7, 1, Theme.border());
                UI.textCenter(val, ix + iw - vw / 2f, ry + 22, 11.5f, Weight.SEMIBOLD, Theme.text());
                float pct = (float) ((n.get() - n.getMin()) / (n.getMax() - n.getMin()));
                float trackY = ry + 48;
                String key = keyPrefix + "_sl_" + s.getName();
                float shown = UI.anim(key, pct, 22f);
                boolean active = dragging == n || hov(ix - 6, trackY - 10, iw + 12, 20);
                float act = UI.anim(key + "_h", active);
                UI.round(ix, trackY - 2, iw, 4, 2, Theme.surface3());
                UI.round(ix, trackY - 2, Math.max(4, iw * shown), 4, 2, Theme.accent());
                float kx = ix + iw * shown;
                float kr = 8 + act * 1.5f;
                UI.shadow(kx - kr, trackY - kr + 1, kr * 2, kr * 2, kr, 6, UI.withAlpha(0, 90));
                UI.circle(kx, trackY, kr, 0xFFFFFFFF);
                if (Theme.light) UI.circle(kx, trackY, kr - 3, Theme.accent());
                else UI.circle(kx, trackY, 3, 0xFF09090B);
                final float fx = ix, fw = iw;
                clickClipped(ix - 8, trackY - 12, iw + 16, 24, top, bottom, k -> {
                    dragging = n;
                    dragX = fx;
                    dragW = fw;
                });
            }
            ry += rh;
        }
        return y + total;
    }

    private static float rowHeight(Setting<?> s) {
        if (s instanceof ModeSetting) return 82;
        if (s instanceof NumberSetting) return 66;
        return 48;
    }

    // =================================================================== komponenty

    private void toggleSwitch(float x, float y, float w, float h, float t) {
        float e = t * t * (3 - 2 * t);
        int off = Theme.surface3();
        UI.round(x, y, w, h, h / 2f, UI.mix(off, Theme.accent(), e));
        if (t < 0.99f) UI.outline(x, y, w, h, h / 2f, 1, UI.a(Theme.borderHi(), 1f - e));
        float r = h / 2f - 3;
        float kx = x + 3 + r + (w - 6 - r * 2) * e;
        UI.shadow(kx - r, y + 3 + 1, r * 2, r * 2, r, 3, UI.withAlpha(0, 60));
        int knobOff = Theme.light ? 0xFFFFFFFF : 0xFF8A8A94;
        UI.circle(kx, y + h / 2f, r, UI.mix(knobOff, Theme.onAccent(), e));
    }

    private void iconButton(String key, float x, float y, float w, float h, char icon, String label, boolean active, IntConsumer action) {
        float hv = UI.anim("btn_" + key, hov(x, y, w, h));
        UI.round(x, y, w, h, 9, active ? Theme.surface3() : UI.mix(Theme.surface2(), Theme.surface3(), hv));
        UI.outline(x, y, w, h, 9, 1, active ? Theme.accent() : UI.mix(Theme.border(), Theme.borderHi(), hv));
        int c = active || hv > 0.5f ? Theme.text() : Theme.sub();
        if (label == null) {
            UI.icon(icon, x + w / 2f, y + h / 2f, 15, c);
        } else {
            UI.icon(icon, x + 17, y + h / 2f, 15, c);
            UI.textMid(label, x + 31, y + h / 2f, 12, Weight.MEDIUM, c);
        }
        click(x, y, w, h, action);
    }

    private void button(String key, float x, float y, float w, float h, char icon, String label, boolean primary, IntConsumer action) {
        float hv = UI.anim("pbtn_" + key, hov(x, y, w, h));
        if (primary) {
            UI.round(x, y, w, h, 10, UI.mix(Theme.accent(), UI.mix(Theme.accent(), Theme.sub(), 0.25f), hv));
        } else {
            UI.round(x, y, w, h, 10, UI.mix(Theme.surface2(), Theme.surface3(), hv));
            UI.outline(x, y, w, h, 10, 1, UI.mix(Theme.border(), Theme.borderHi(), hv));
        }
        int c = primary ? Theme.onAccent() : Theme.text();
        float tw = UI.width(label, 12.5f, Weight.SEMIBOLD) + (icon != 0 ? 22 : 0);
        float sx = x + (w - tw) / 2f;
        if (icon != 0) {
            UI.icon(icon, sx + 7, y + h / 2f, 14, c);
            sx += 22;
        }
        UI.textMid(label, sx, y + h / 2f, 12.5f, Weight.SEMIBOLD, c);
        click(x, y, w, h, action);
    }

    private void clampScroll() {
        moduleScrollTarget = Math.max(0, Math.min(moduleScrollTarget, Math.max(0, moduleContentH - moduleViewH)));
        settingsScrollTarget = Math.max(0, Math.min(settingsScrollTarget, Math.max(0, settingsContentH - settingsViewH)));
    }

    // =================================================================== configi

    private void drawConfigs() {
        float bx = wx + SIDEBAR + 24, by = wy + HEADER + 20;
        float bw = W - SIDEBAR - 48, bh = H - HEADER - FOOTER - 40;
        float lw = bw - 280 - 16;

        UI.text("ZAPISANE", bx, by, 10.5f, Weight.SEMIBOLD, Theme.dim());
        float y = by + 22;
        List<String> list = ConfigManager.list();
        for (String name : list) {
            if (y + 62 > by + bh) break;
            boolean cur = name.equals(ConfigManager.getCurrent());
            float hv = UI.anim("cfg_" + name, hov(bx, y, lw, 62));
            UI.round(bx, y, lw, 62, 12, UI.mix(Theme.surface(), Theme.surface2(), Math.max(hv, cur ? 1 : 0)));
            UI.outline(bx, y, lw, 62, 12, 1, cur ? Theme.borderHi() : Theme.border());
            UI.round(bx + 14, y + 14, 34, 34, 9, cur ? Theme.accent() : Theme.surface3());
            UI.icon(Icons.FOLDER, bx + 31, y + 31, 16, cur ? Theme.onAccent() : Theme.sub());
            UI.text(name, bx + 60, y + 14, 13.5f, Weight.SEMIBOLD, Theme.text());
            UI.text(cur ? "Aktywny config" : "Kliknij Wczytaj, aby użyć", bx + 60, y + 34, 11.5f, Weight.REGULAR, Theme.dim());

            float rx = bx + lw - 14;
            if (!cur) {
                iconButton("del_" + name, rx - 32, y + 15, 32, 32, Icons.TRASH, null, false, b -> {
                    ConfigManager.delete(name);
                    NotificationManager.push("Config", "Usunięto „" + name + "”", false);
                });
                rx -= 40;
                button("load_" + name, rx - 84, y + 15, 84, 32, Icons.DOWNLOAD, "Wczytaj", true, b -> {
                    ConfigManager.loadConfig(name);
                    ConfigManager.save();
                    NotificationManager.push("Config", "Wczytano „" + name + "”", true);
                });
            } else {
                float bw2 = UI.width("Aktywny", 11, Weight.SEMIBOLD) + 30;
                UI.round(rx - bw2, y + 20, bw2, 22, 11, Theme.surface3());
                UI.icon(Icons.CHECK, rx - bw2 + 13, y + 31, 12, Theme.text());
                UI.textMid("Aktywny", rx - bw2 + 22, y + 31, 11, Weight.SEMIBOLD, Theme.text());
            }
            y += 70;
        }

        // nowy config
        float rx = bx + lw + 16, rw = 280;
        UI.round(rx, by, rw, bh, 14, Theme.surface());
        UI.outline(rx, by, rw, bh, 14, 1, Theme.border());
        float fx = rx + 20, fw = rw - 40, fy = by + 20;
        UI.text("Nowy config", fx, fy, 15, Weight.SEMIBOLD, Theme.text());
        UI.text("Zapisuje moduły, bindy i ustawienia", fx, fy + 22, 11.5f, Weight.REGULAR, Theme.dim());
        fy += 52;
        float f = UI.anim("cfgFocus", configFocused);
        UI.round(fx, fy, fw, 40, 10, Theme.bg());
        UI.outline(fx, fy, fw, 40, 10, 1, UI.mix(Theme.border(), Theme.borderHi(), f));
        boolean empty = configName.isEmpty();
        UI.textMid(empty ? "Nazwa configu" : configName, fx + 14, fy + 20, 13, Weight.REGULAR, empty ? Theme.dim() : Theme.text());
        if (configFocused && (System.currentTimeMillis() / 530) % 2 == 0) {
            UI.rect(fx + 14 + (empty ? 0 : UI.width(configName, 13) + 1), fy + 12, 1.5f, 16, Theme.text());
        }
        click(fx, fy, fw, 40, b -> { configFocused = true; searchFocused = false; });
        fy += 52;
        button("create", fx, fy, fw, 40, Icons.PLUS, "Utwórz config", true, b -> {
            if (configName.isBlank()) { configFocused = true; return; }
            try {
                String n = ConfigManager.sanitize(configName);
                ConfigManager.saveConfig(n);
                ConfigManager.loadConfig(n);
                ConfigManager.save();
                NotificationManager.push("Config", "Utworzono „" + n + "”", true);
                configName = "";
                configFocused = false;
            } catch (Exception ignored) {}
        });
        fy += 52;
        UI.rect(fx, fy, fw, 1, Theme.border());
        fy += 16;
        button("saveCur", fx, fy, fw, 40, Icons.SAVE, "Zapisz aktualny", false, b -> {
            ConfigManager.save();
            NotificationManager.push("Config", "Zapisano „" + ConfigManager.getCurrent() + "”", true);
        });
        fy += 48;
        button("openDir", fx, fy, fw, 40, Icons.FOLDER_OPEN, "Otwórz folder", false, b -> Util.getOperatingSystem().open(ConfigManager.getDir().toFile()));
    }

    // =================================================================== wygląd

    private void drawTheme() {
        float bx = wx + SIDEBAR + 24, by = wy + HEADER + 20;
        float bw = W - SIDEBAR - 48;
        UI.text("MOTYW", bx, by, 10.5f, Weight.SEMIBOLD, Theme.dim());
        float cy = by + 22;
        float cw = (bw - 16) / 2f, ch = 150;
        themeCard(bx, cy, cw, ch, false);
        themeCard(bx + cw + 16, cy, cw, ch, true);

        float y = cy + ch + 28;
        UI.text("INTERFEJS", bx, y, 10.5f, Weight.SEMIBOLD, Theme.dim());
        y += 22;
        ClientSettings c = cs();
        List<Setting<?>> list = List.of(c.guiAnimations, c.guiBlur, c.guiScale, c.hudScale);
        float top = wy + HEADER + 1, bottom = wy + H - FOOTER;
        drawSettingsGroup(list, bx, y, bw, top, bottom, "theme");
    }

    private void themeCard(float x, float y, float w, float h, boolean light) {
        boolean sel = Theme.light == light;
        float hv = UI.anim("theme_" + light, hov(x, y, w, h));
        UI.round(x, y, w, h, 14, Theme.surface());
        UI.outline(x, y, w, h, 14, sel ? 2 : 1, sel ? Theme.accent() : UI.mix(Theme.border(), Theme.borderHi(), hv));

        // miniatura okna
        int bg = light ? 0xFFFFFFFF : 0xFF09090B, side = light ? 0xFFF4F4F5 : 0xFF141416, line = light ? 0xFFE4E4E7 : 0xFF232327;
        int fg = light ? 0xFF09090B : 0xFFFAFAFA, mid = light ? 0xFFD4D4D8 : 0xFF3F3F46;
        float px = x + 14, py = y + 14, pw = w - 28, ph = 88;
        UI.round(px, py, pw, ph, 9, bg);
        UI.outline(px, py, pw, ph, 9, 1, line);
        UI.round(px + 1, py + 1, 60, ph - 2, 8, side);
        UI.round(px + 10, py + 10, 14, 14, 4, fg);
        for (int i = 0; i < 4; i++) UI.round(px + 10, py + 32 + i * 12, 40 - i * 6, 5, 2.5f, i == 0 ? fg : mid);
        for (int i = 0; i < 3; i++) {
            float ry = py + 12 + i * 24;
            UI.round(px + 72, ry, pw - 84, 18, 5, side);
            UI.round(px + 80, ry + 6.5f, 40 + i * 10, 5, 2.5f, mid);
            UI.round(px + pw - 36, ry + 4, 18, 10, 5, i == 1 ? mid : fg);
        }
        UI.textMid(light ? "Jasny" : "Ciemny", x + 16, y + h - 24, 13.5f, Weight.SEMIBOLD, Theme.text());
        if (sel) {
            UI.circle(x + w - 26, y + h - 24, 10, Theme.accent());
            UI.icon(Icons.CHECK, x + w - 26, y + h - 24, 12, Theme.onAccent());
        } else {
            UI.outline(x + w - 36, y + h - 34, 20, 20, 10, 1.5f, Theme.borderHi());
        }
        click(x, y, w, h, b -> {
            Theme.light = light;
            ConfigManager.save();
        });
    }

    // =================================================================== input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        searchFocused = false;
        configFocused = false;
        Module bindingBefore = binding;
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (UI.hover(mx, my, r.x, r.y, r.w, r.h)) {
                r.action.accept(button);
                if (bindingBefore != null && binding == bindingBefore) binding = null;
                return true;
            }
        }
        binding = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == Tab.CATEGORY) {
            if (UI.hover(mx, my, listX, listY, listW, listH)) moduleScrollTarget -= (float) verticalAmount * 40;
            else if (UI.hover(mx, my, detX, detY, detW, detH)) settingsScrollTarget -= (float) verticalAmount * 40;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                binding.setKey(GLFW.GLFW_KEY_UNKNOWN);
                NotificationManager.push(binding.getName(), "Usunięto klawisz", false);
            } else {
                binding.setKey(keyCode);
                NotificationManager.push(binding.getName(), "Klawisz: " + keyName(keyCode), true);
            }
            binding = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            tab = Tab.CATEGORY;
            searchFocused = true;
            configFocused = false;
            return true;
        }
        if (searchFocused || configFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = configFocused = false;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (searchFocused && !search.isEmpty()) search = search.substring(0, search.length() - 1);
                if (configFocused && !configName.isEmpty()) configName = configName.substring(0, configName.length() - 1);
            }
            return true;
        }
        if (keyCode == ModuleManager.get(ClickGUI.class).getKey() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && search.length() < 24 && chr >= 32) {
            search += chr;
            moduleScrollTarget = 0;
            return true;
        }
        if (configFocused && configName.length() < 24 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-')) {
            configName += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        ConfigManager.save();
        if (cs().guiAnimations.get() && !closing) {
            closing = true;
        } else {
            super.close();
        }
    }

    private static String plural(int n, String one, String few, String many) {
        if (n == 1) return one;
        int m10 = n % 10, m100 = n % 100;
        return (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) ? few : many;
    }

    public static String keyName(int key) {
        if (key <= 0) return "Brak";
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
            default -> {
                String n = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
                yield n.length() > 12 ? n.substring(0, 12) : n.toUpperCase(Locale.ROOT);
            }
        };
    }
}
