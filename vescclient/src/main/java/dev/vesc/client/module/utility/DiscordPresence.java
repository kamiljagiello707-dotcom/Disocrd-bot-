package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.vesc.client.VescClient;
import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * DiscordRPC – status "W grze" na profilu Discord: logo klienta, nazwa, wersja, serwer i czas gry.
 * Łączy się z lokalnym klientem Discord przez IPC (named pipe na Windows, gniazdo Unix na Linux/macOS),
 * bez dodatkowych bibliotek. Nazwę i logo bierze z aplikacji Discord, której ID jest w vescclient/discord.json.
 */
public class DiscordPresence extends Module {
    public final ModeSetting badge = new ModeSetting("Plakietka", "Badge", "Premium", "Beta", "Free", "None");
    public final ModeSetting largeImage = new ModeSetting("Duży obraz", "Large Image", "Logo", "Skin");
    public final BoolSetting showServer = new BoolSetting("Pokaż serwer", "Show Server", true);
    public final BoolSetting showName = new BoolSetting("Pokaż nick", "Show Name", false);
    public final BoolSetting showTime = new BoolSetting("Pokaż czas", "Show Time", true);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final long start = System.currentTimeMillis() / 1000;
    private volatile Thread thread;
    private volatile boolean running;
    private volatile String status = "";
    private boolean warned;

    public DiscordPresence() {
        super("DiscordRPC", Category.UTILITY, "message", "Logo, nazwa i wersja klienta w statusie Discorda", "Client logo, name and version in your Discord status");
        place(Category.UTILITY, "Social");
        add(badge, largeImage, showServer, showName, showTime);
    }

    /** stan połączenia do wyświetlenia (np. w opisie) */
    public String status() { return status; }

    // ------------------------------------------------------------------ konfiguracja

    private static Path configFile() { return FabricLoader.getInstance().getGameDir().resolve("vescclient").resolve("discord.json"); }

    /** discord.json: appId (Application ID z Discord Developer Portal), largeImage, przycisk */
    private static JsonObject config() {
        Path p = configFile();
        try {
            if (Files.exists(p)) return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject o = new JsonObject();
            o.addProperty("appId", "");
            o.addProperty("largeImage", "logo");
            o.addProperty("buttonLabel", "");
            o.addProperty("buttonUrl", "");
            o.addProperty("_help", "appId: Application ID z https://discord.com/developers/applications (nazwa aplikacji = tytuł w statusie). " +
                    "W Rich Presence > Art Assets wgraj logo pod nazwą 'logo'. Przycisk pokazuje się, gdy buttonLabel i buttonUrl nie są puste.");
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(o), StandardCharsets.UTF_8);
            return o;
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString().trim() : ""; }

    // ------------------------------------------------------------------ cykl życia

    @Override protected void onEnable() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::loop, "VescClient-DiscordRPC");
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    @Override protected void onDisable() {
        running = false;
        Thread t = thread;
        if (t != null) t.interrupt();
    }

    /** wywoływane po wczytaniu konfiguracji (moduł mógł zostać włączony bez onEnable) */
    public void ensureRunning() { if (isEnabled() && !running) onEnable(); }

    private void loop() {
        while (running && isEnabled()) {
            JsonObject cfg = config();
            String appId = str(cfg, "appId");
            if (appId.isEmpty()) {
                status = "Brak appId w vescclient/discord.json";
                if (!warned) {
                    warned = true;
                    MinecraftClient.getInstance().execute(() -> VescClient.get().notifications.info("message", "Discord RPC",
                            "Wpisz Application ID w vescclient/discord.json"));
                }
                sleep(10_000);
                continue;
            }
            try (Ipc ipc = Ipc.open()) {
                ipc.send(0, "{\"v\":1,\"client_id\":\"" + appId.replace("\"", "") + "\"}");
                Frame ready = ipc.read();
                if (ready.op != 1 || !ready.json.contains("READY")) throw new IOException("handshake: " + ready.json);
                status = "Połączono";
                String last = null;
                long lastSent = 0;
                while (running && isEnabled()) {
                    String act = activity(cfg);
                    if (!act.equals(last) || System.currentTimeMillis() - lastSent > 60_000) {
                        ipc.send(1, "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":" + act + "},\"nonce\":\"" + UUID.randomUUID() + "\"}");
                        Frame r = ipc.read();
                        if (r.op == 2) throw new IOException("closed: " + r.json);
                        last = act;
                        lastSent = System.currentTimeMillis();
                    }
                    sleep(5_000);
                    cfg = config();
                }
                // wyczyść status przy wyłączeniu
                ipc.send(1, "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + "},\"nonce\":\"" + UUID.randomUUID() + "\"}");
                ipc.read();
            } catch (Exception e) {
                status = "Discord nie jest uruchomiony";
                VescClient.LOGGER.debug("Discord RPC: {}", e.toString());
                sleep(15_000);
            }
        }
        status = "";
        running = false;
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    /** treść statusu: tytuł = nazwa aplikacji Discord, niżej wersja/serwer, plakietka, czas */
    private String activity(JsonObject cfg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String where;
        if (mc.world == null) where = "W menu";
        else if (mc.isInSingleplayer()) where = "Singleplayer";
        else {
            ServerInfo si = mc.getCurrentServerEntry();
            where = showServer.get() && si != null ? si.address : "Multiplayer";
        }
        JsonObject a = new JsonObject();
        a.addProperty("details", "v" + VescClient.VERSION + "  •  Minecraft 1.21.1");
        String st = showServer.get() || mc.world == null ? where : (mc.world == null ? "W menu" : "W grze");
        if (!badge.get().equals("None")) st = badge.get() + "  •  " + st;
        a.addProperty("state", st);
        if (showTime.get()) { JsonObject ts = new JsonObject(); ts.addProperty("start", start); a.add("timestamps", ts); }
        JsonObject as = new JsonObject();
        String name = mc.getSession().getUsername();
        String img = largeImage.get().equals("Skin") ? "https://mc-heads.net/avatar/" + name + "/128" : str(cfg, "largeImage");
        if (img.isEmpty()) img = "logo";
        as.addProperty("large_image", img);
        as.addProperty("large_text", "VescClient " + VescClient.VERSION);
        if (showName.get()) { as.addProperty("small_image", "https://mc-heads.net/avatar/" + name + "/64"); as.addProperty("small_text", name); }
        a.add("assets", as);
        String bl = str(cfg, "buttonLabel"), bu = str(cfg, "buttonUrl");
        if (!bl.isEmpty() && bu.startsWith("https://")) {
            JsonArray btns = new JsonArray();
            JsonObject b = new JsonObject();
            b.addProperty("label", bl.length() > 32 ? bl.substring(0, 32) : bl);
            b.addProperty("url", bu);
            btns.add(b);
            a.add("buttons", btns);
        }
        return a.toString();
    }

    // ------------------------------------------------------------------ IPC

    private record Frame(int op, String json) {}

    /** połączenie IPC z Discordem: ramka = op (int32 LE) + długość (int32 LE) + JSON */
    private static abstract class Ipc implements Closeable {
        abstract void write(byte[] b) throws IOException;
        abstract void readFully(byte[] b) throws IOException;

        void send(int op, String json) throws IOException {
            byte[] p = json.getBytes(StandardCharsets.UTF_8);
            ByteBuffer bb = ByteBuffer.allocate(8 + p.length).order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(op).putInt(p.length).put(p);
            write(bb.array());
        }

        Frame read() throws IOException {
            byte[] h = new byte[8];
            readFully(h);
            ByteBuffer bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
            int op = bb.getInt(), len = bb.getInt();
            if (len < 0 || len > 1 << 20) throw new IOException("bad frame");
            byte[] p = new byte[len];
            readFully(p);
            return new Frame(op, new String(p, StandardCharsets.UTF_8));
        }

        static Ipc open() throws IOException {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
            IOException last = new IOException("Discord IPC not found");
            for (int i = 0; i < 10; i++) {
                try {
                    if (windows) return new PipeIpc(new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"));
                    for (String dir : unixDirs()) {
                        Path p = Path.of(dir, "discord-ipc-" + i);
                        if (!Files.exists(p)) continue;
                        SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                        ch.connect(UnixDomainSocketAddress.of(p));
                        return new SocketIpc(ch);
                    }
                } catch (IOException e) { last = e; }
            }
            throw last;
        }

        private static java.util.List<String> unixDirs() {
            java.util.List<String> base = new java.util.ArrayList<>();
            for (String env : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) { String v = System.getenv(env); if (v != null) base.add(v); }
            base.add("/tmp");
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String b : base) { out.add(b); out.add(b + "/app/com.discordapp.Discord"); out.add(b + "/snap.discord"); }
            return out;
        }
    }

    private static final class PipeIpc extends Ipc {
        private final RandomAccessFile f;
        PipeIpc(RandomAccessFile f) { this.f = f; }
        @Override void write(byte[] b) throws IOException { f.write(b); }
        @Override void readFully(byte[] b) throws IOException { f.readFully(b); }
        @Override public void close() throws IOException { f.close(); }
    }

    private static final class SocketIpc extends Ipc {
        private final SocketChannel ch;
        SocketIpc(SocketChannel ch) { this.ch = ch; }
        @Override void write(byte[] b) throws IOException { ByteBuffer bb = ByteBuffer.wrap(b); while (bb.hasRemaining()) ch.write(bb); }
        @Override void readFully(byte[] b) throws IOException {
            ByteBuffer bb = ByteBuffer.wrap(b);
            while (bb.hasRemaining()) if (ch.read(bb) < 0) throw new IOException("EOF");
        }
        @Override public void close() throws IOException { ch.close(); }
    }
}
