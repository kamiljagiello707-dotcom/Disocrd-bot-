package dev.vesc.client.module;

import dev.vesc.client.VescClient;
import dev.vesc.client.ui.Theme;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Logika modułów legit / kosmetycznych: Zoom, FreeLook, Fullbright, NoHurtCam, NoFire, BlockOutline,
 * TimeChanger, Weather, Sprint, AutoRespawn, NameProtect, AutoGG, ChatSuffix, MiddleClick, AutoReconnect.
 * Wszystko działa wyłącznie po stronie klienta (wizualnie / wygoda), bez przewagi nad innymi graczami.
 */
public final class Legit {
    private Legit() {}

    public static KeyBinding zoomKey, freeLookKey;

    static Module m(String name) { return VescClient.get().modules.get(name); }
    static boolean on(String name) { Module x = m(name); return x != null && x.isEnabled(); }

    // ------------------------------------------------------------------ init

    public static void init() {
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vescclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.vescclient"));
        freeLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vescclient.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "category.vescclient"));

        WorldRenderEvents.BLOCK_OUTLINE.register((w, o) -> {
            if (!on("BlockOutline")) return true;
            drawOutline(w.matrixStack(), w.consumers(), o.blockPos(), o.blockState().getOutlineShape(w.world(), o.blockPos(), ShapeContext.of(o.entity())),
                    o.cameraX(), o.cameraY(), o.cameraZ());
            return false;
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(Legit::chatSuffix);
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay) autoGG(msg.getString()); });

        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> {
            ServerInfo info = c.getCurrentServerEntry();
            if (info != null) lastServer = info;
            reconnectAttempts = 0;
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof DisconnectedScreen) reconnectScreen(client, screen, w, h);
        });
    }

    // ------------------------------------------------------------------ tick

    private static boolean wasSprint, wasZoomCine, cineBefore, freeToggled, freeKeyHeld, midHeld;
    private static Perspective perspectiveBefore;
    private static int deathTicks = -1;

    public static void tick(MinecraftClient mc) {
        if (m("FakePlayer") instanceof FakePlayer fp) fp.tick(mc);

        // Sprint – dokładnie jak wciśnięty klawisz sprintu (vanilla nadal pilnuje głodu, ślepoty itd.)
        boolean sprint = on("Sprint") && mc.player != null && mc.currentScreen == null;
        if (sprint) {
            Module s = m("Sprint");
            boolean fwd = !s.bool("Only Forward") || mc.player.input.movementForward > 0;
            boolean sneak = s.bool("Stop On Sneak") && mc.player.isSneaking();
            mc.options.sprintKey.setPressed(fwd && !sneak);
            wasSprint = true;
        } else if (wasSprint) {
            wasSprint = false;
            mc.options.sprintKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), KeyBindingHelper.getBoundKeyOf(mc.options.sprintKey).getCode()));
        }

        // Zoom – kamera filmowa podczas przybliżenia
        boolean cine = zooming() && m("Zoom").bool("Cinematic Camera");
        if (cine && !wasZoomCine) { cineBefore = mc.options.smoothCameraEnabled; mc.options.smoothCameraEnabled = true; }
        if (!cine && wasZoomCine) mc.options.smoothCameraEnabled = cineBefore;
        wasZoomCine = cine;
        if (!zooming()) zoomScroll = 1;

        tickFreeLook(mc);

        // AutoRespawn
        if (on("AutoRespawn") && mc.currentScreen instanceof DeathScreen && mc.player != null) {
            Module r = m("AutoRespawn");
            if (deathTicks < 0) {
                deathTicks = 0;
                BlockPos p = mc.player.getBlockPos();
                String coords = p.getX() + ", " + p.getY() + ", " + p.getZ();
                if (r.bool("Show Death Coords")) VescClient.get().notifications.info("i_skull", "You died", coords);
                if (r.bool("Coords In Chat")) mc.inGameHud.getChatHud().addMessage(Text.literal("§9[Vesc]§7 Death point: §f" + coords));
            }
            if (deathTicks++ >= r.num("Delay")) { mc.player.requestRespawn(); mc.setScreen(null); deathTicks = -1; }
        } else deathTicks = -1;

        // MiddleClick – dodaj / usuń znajomego
        boolean mid = mc.currentScreen == null && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (mid && !midHeld && on("MiddleClick") && mc.targetedEntity != null) {
            Module mc2 = m("MiddleClick");
            if (mc.targetedEntity instanceof PlayerEntity || !mc2.bool("Only Players")) {
                String n = mc.targetedEntity.getName().getString();
                var fr = VescClient.get().friends;
                boolean added;
                if (fr.is(n)) { fr.remove(n); added = false; } else { fr.add(n); added = true; }
                VescClient.get().config.saveClient();
                if (mc2.bool("Notify")) VescClient.get().notifications.info("player", added ? "Friend added" : "Friend removed", n);
            }
        }
        midHeld = mid;

        tickGG(mc);
        tickTime();
        tickFullbright();
    }

    // ------------------------------------------------------------------ Zoom

    private static float zoomAnim = 1, zoomScroll = 1;
    private static long zoomLast = System.nanoTime();

    public static boolean zooming() {
        return on("Zoom") && zoomKey != null && zoomKey.isPressed() && MinecraftClient.getInstance().currentScreen == null;
    }

    /** mnożnik FOV (wywoływany co klatkę z GameRenderer.getFov) */
    public static double fovDivisor() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - zoomLast) / 1e9f);
        zoomLast = now;
        float target = zooming() ? (float) (m("Zoom").num("Zoom") * zoomScroll) : 1f;
        if (on("Zoom") && !m("Zoom").bool("Smooth")) zoomAnim = target;
        else zoomAnim += (target - zoomAnim) * (1 - (float) Math.exp(-14 * dt));
        if (Math.abs(zoomAnim - target) < 0.001f) zoomAnim = target;
        return zoomAnim;
    }

    /** @return true gdy scroll został zużyty przez zoom */
    public static boolean zoomScroll(double v) {
        if (!zooming() || !m("Zoom").bool("Scroll Zoom") || v == 0) return false;
        zoomScroll = MathHelper.clamp(zoomScroll * (v > 0 ? 1.15f : 1 / 1.15f), 0.35f, 4f);
        return true;
    }

    public static boolean hideHand() { return zooming() && m("Zoom").bool("Hide Hand"); }

    // ------------------------------------------------------------------ FreeLook

    public static float freeYaw, freePitch;
    private static boolean freeActive;

    public static boolean freeLook() { return freeActive; }

    private static void tickFreeLook(MinecraftClient mc) {
        boolean want = false;
        if (on("FreeLook") && mc.player != null) {
            boolean down = freeLookKey.isPressed() && mc.currentScreen == null;
            if (m("FreeLook").mode("Mode").equals("Toggle")) {
                if (down && !freeKeyHeld) freeToggled = !freeToggled;
                want = freeToggled;
            } else want = down;
            freeKeyHeld = down;
        } else freeToggled = false;

        if (want && !freeActive) {
            freeYaw = mc.player.getYaw();
            freePitch = mc.player.getPitch();
            perspectiveBefore = mc.options.getPerspective();
            if (m("FreeLook").bool("Third Person") && perspectiveBefore.isFirstPerson()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (!want && freeActive && perspectiveBefore != null) {
            mc.options.setPerspective(perspectiveBefore);
        }
        freeActive = want;
    }

    /** @return true gdy ruch myszy obraca tylko kamerę */
    public static boolean freeLookMouse(double dx, double dy) {
        if (!freeActive) return false;
        Module f = m("FreeLook");
        double s = f.num("Sensitivity") * 0.15;
        freeYaw += (float) (dx * s);
        freePitch = MathHelper.clamp(freePitch + (float) (dy * s * (f.bool("Invert Pitch") ? -1 : 1)), -90f, 90f);
        return true;
    }

    // ------------------------------------------------------------------ NoHurtCam / NoFire

    public static float hurtCamStrength() { return on("NoHurtCam") ? (float) (m("NoHurtCam").num("Strength") / 100.0) : 1f; }
    public static boolean noBobbing() { return on("NoHurtCam") && m("NoHurtCam").bool("No Bobbing"); }
    public static boolean noTotem(net.minecraft.item.ItemStack s) { return on("NoHurtCam") && m("NoHurtCam").bool("No Totem Animation") && s.isOf(Items.TOTEM_OF_UNDYING); }

    public static boolean noFire() { return on("NoFire"); }
    public static float fireOffset() { return noFire() ? (float) (1 - m("NoFire").num("Height")) * 0.6f : 0f; }
    public static float fireAlpha(float vanilla) { return noFire() ? (float) (m("NoFire").num("Opacity") / 100.0) : vanilla; }
    public static boolean hideFire(MinecraftClient mc) {
        return noFire() && m("NoFire").bool("Hide In Water") && mc.player != null && mc.player.isTouchingWater();
    }

    // ------------------------------------------------------------------ Fullbright

    private static float fb;
    private static long fbLast = System.nanoTime();

    private static void tickFullbright() {
        long now = System.nanoTime();
        float dt = Math.min(0.25f, (now - fbLast) / 1e9f);
        fbLast = now;
        float target = on("Fullbright") ? 1 : 0;
        if (on("Fullbright") && !m("Fullbright").bool("Fade") || !on("Fullbright") && fb < 0.01f) fb = target;
        else fb += (target - fb) * Math.min(1, dt * 6);
    }

    public static float gamma(float vanilla) {
        if (fb <= 0 || !m("Fullbright").mode("Mode").equals("Gamma")) return vanilla;
        float t = 1 + (float) (m("Fullbright").num("Gamma") / 100.0) * 15f;
        return MathHelper.lerp(fb, vanilla, Math.max(vanilla, t));
    }

    public static boolean nightVision() { return on("Fullbright") && m("Fullbright").mode("Mode").equals("Night Vision"); }
    public static float nightVisionStrength() { return fb; }

    // ------------------------------------------------------------------ TimeChanger / Weather

    private static float timeAnim = -1;

    private static void tickTime() {
        if (!on("TimeChanger")) { timeAnim = -1; return; }
        float target = timeTarget();
        if (timeAnim < 0 || !m("TimeChanger").bool("Animate")) { timeAnim = target; return; }
        float d = target - timeAnim;
        if (Math.abs(d) < 5) timeAnim = target; else timeAnim += d * 0.12f;
    }

    private static float timeTarget() {
        return switch (m("TimeChanger").mode("Time")) {
            case "Sunset" -> 12500;
            case "Night" -> 14500;
            case "Midnight" -> 18000;
            case "Custom" -> (float) m("TimeChanger").num("Custom Time");
            default -> 6000;
        };
    }

    /** @return czas dnia lub -1 gdy moduł wyłączony */
    public static long timeOfDay() { return on("TimeChanger") && timeAnim >= 0 ? (long) timeAnim : -1; }

    public static float rain(float vanilla) {
        if (!on("Weather")) return vanilla;
        return m("Weather").mode("Weather").equals("Clear") ? 0 : (float) (m("Weather").num("Intensity") / 100.0);
    }

    public static float thunder(float vanilla) {
        if (!on("Weather")) return vanilla;
        return m("Weather").mode("Weather").equals("Thunder") ? (float) (m("Weather").num("Intensity") / 100.0) : 0;
    }

    // ------------------------------------------------------------------ BlockOutline

    private static void drawOutline(MatrixStack ms, net.minecraft.client.render.VertexConsumerProvider vcp, BlockPos pos, VoxelShape shape, double cx, double cy, double cz) {
        if (shape.isEmpty() || vcp == null) return;
        Module b = m("BlockOutline");
        float t = (System.currentTimeMillis() % 100000) / 1000f;
        int col = b.bool("Accent Color") ? (b.bool("Animated") ? Theme.accentWave(t * 0.6f) : Theme.accent()) : 0xFFFFFFFF;
        float r = (col >> 16 & 255) / 255f, g = (col >> 8 & 255) / 255f, bl = (col & 255) / 255f;
        double ox = pos.getX() - cx, oy = pos.getY() - cy, oz = pos.getZ() - cz;
        String mode = b.mode("Mode");
        if (!mode.equals("Outline")) {
            float a = (float) (b.num("Fill Opacity") / 100.0);
            if (b.bool("Animated")) a *= 0.75f + 0.25f * MathHelper.sin(t * 4);
            for (Box box : shape.getBoundingBoxes()) {
                Box bb = box.offset(ox, oy, oz).expand(0.002);
                DebugRenderer.drawBox(ms, vcp, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, r, g, bl, a);
            }
        }
        if (!mode.equals("Fill")) {
            VertexConsumer lines = vcp.getBuffer(RenderLayer.getLines());
            int passes = Math.max(1, (int) Math.round(b.num("Width") * 2) - 1);
            for (int i = 0; i < passes; i++) {
                double o = i == 0 ? 0 : 0.0016 * ((i + 1) / 2) * (i % 2 == 0 ? 1 : -1);
                WorldRenderer.drawShapeOutline(ms, lines, shape, ox + o, oy + o, oz + o, r, g, bl, 1f, true);
            }
        }
    }

    // ------------------------------------------------------------------ NameProtect

    public static String protect(String s) {
        if (s == null || s.isEmpty() || VescClient.get() == null || VescClient.get().modules == null || !on("NameProtect")) return s;
        Module np = m("NameProtect");
        String me = MinecraftClient.getInstance().getSession().getUsername();
        String repl = switch (np.mode("Replace With")) { case "Player" -> "Player"; case "Stars" -> "*".repeat(me.length()); default -> "You"; };
        if (s.contains(me)) s = s.replace(me, repl);
        if (np.bool("Friends")) for (String f : VescClient.get().friends.all()) if (!f.isEmpty() && s.contains(f)) s = s.replace(f, "Friend");
        return s;
    }

    /** nick do wyświetlania w GUI/HUD klienta */
    public static String name() { return protect(MinecraftClient.getInstance().getSession().getUsername()); }

    // ------------------------------------------------------------------ ChatSuffix

    private static final String SMALL_FROM = "abcdefghijklmnopqrstuvwxyz", SMALL_TO = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀѕᴛᴜᴠᴡxʏᴢ";

    private static String chatSuffix(String msg) {
        if (!on("ChatSuffix") || msg.startsWith("/")) return msg;
        Module c = m("ChatSuffix");
        String suf = c.mode("Suffix");
        if (c.bool("Small Font")) {
            StringBuilder sb = new StringBuilder();
            for (char ch : suf.toLowerCase(Locale.ROOT).toCharArray()) {
                int i = SMALL_FROM.indexOf(ch);
                sb.append(i >= 0 ? SMALL_TO.charAt(i) : ch);
            }
            suf = sb.toString();
        }
        String out = msg + " " + suf;
        return out.length() > 256 ? msg : out;
    }

    // ------------------------------------------------------------------ AutoGG

    private static final String[] WIN = {"you won", "you win", "victory!", "winner:", "won the game", "#1 victory", "1st place"};
    private static final String[] END = {"game over", "1st killer", "play again", "game ended", "the game has ended"};
    private static long ggAt = -1, ggLast;

    private static void autoGG(String text) {
        if (!on("AutoGG")) return;
        String s = text.toLowerCase(Locale.ROOT);
        boolean hit = false;
        for (String w : WIN) hit |= s.contains(w);
        if (!m("AutoGG").bool("Only Win")) for (String w : END) hit |= s.contains(w);
        if (!hit || System.currentTimeMillis() - ggLast < 15000) return;
        ggLast = System.currentTimeMillis();
        ggAt = ggLast + (long) (m("AutoGG").num("Delay") * 1000);
    }

    private static void tickGG(MinecraftClient mc) {
        if (ggAt < 0 || System.currentTimeMillis() < ggAt) return;
        ggAt = -1;
        if (on("AutoGG") && mc.player != null && mc.getNetworkHandler() != null) mc.getNetworkHandler().sendChatMessage(m("AutoGG").mode("Message"));
    }

    // ------------------------------------------------------------------ AutoReconnect

    private static ServerInfo lastServer;
    private static int reconnectAttempts;

    private static void reconnectScreen(MinecraftClient mc, net.minecraft.client.gui.screen.Screen screen, int w, int h) {
        if (!on("AutoReconnect") || lastServer == null) return;
        Module r = m("AutoReconnect");
        ServerInfo info = lastServer;
        boolean auto = reconnectAttempts < (int) r.num("Attempts");
        int[] ticks = {(int) (r.num("Delay") * 20)};
        Runnable go = () -> {
            reconnectAttempts++;
            ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(info.address), info, false, null);
        };
        ButtonWidget btn = ButtonWidget.builder(Text.literal("Reconnect"), b -> go.run()).dimensions(w / 2 - 100, h - 32, 200, 20).build();
        if (r.bool("Show Button") || auto) Screens.getButtons(screen).add(btn);
        btn.visible = r.bool("Show Button") || auto;
        ScreenEvents.afterTick(screen).register(s -> {
            if (!auto) { btn.setMessage(Text.literal("Reconnect")); return; }
            btn.setMessage(Text.literal("Reconnect (" + (ticks[0] + 19) / 20 + "s)"));
            if (--ticks[0] <= 0 && mc.currentScreen == s) go.run();
        });
    }
}
