package dev.with.client.discord;

import com.google.gson.JsonObject;
import dev.with.client.WithClient;
import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.module.Setting;
import dev.with.client.ui.Ui;
import net.minecraft.SharedConstants;
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
 * Status w Discordzie (Rich Presence): zamiast "Minecraft" widać aplikację WithClient z logo,
 * wersją klienta i Minecrafta, miejscem gry (menu / świat / serwer) i czasem gry.
 * Łączy się z Discordem przez IPC (named pipe na Windows, gniazdo Unix na Linux/macOS), bez bibliotek.
 * Nazwę ("WithClient") i logo bierze z aplikacji Discord, której ID wpisuje się w module DiscordRPC.
 */
public final class DiscordRpc {
    private DiscordRpc() {}

    private static final long START = System.currentTimeMillis() / 1000;
    private static volatile String status = "";
    private static volatile boolean connected;

    public static String status() { return status; }
    public static boolean connected() { return connected; }

    public static void init() {
        Thread t = new Thread(DiscordRpc::loop, "WithClient-DiscordRPC");
        t.setDaemon(true);
        t.start();
    }

    private static Module mod() { return Modules.get("DiscordRPC"); }
    private static boolean enabled() { Module m = mod(); return m != null && m.isEnabled(); }
    private static String text(String s) { Module m = mod(); return m != null && m.setting(s) instanceof Setting.Text t ? t.get().trim() : ""; }
    private static boolean bo(String s) { Module m = mod(); return m != null && m.setting(s) instanceof Setting.Bool b && b.get(); }
    private static String mode(String s, String d) { Module m = mod(); return m != null && m.setting(s) instanceof Setting.Mode o ? o.get() : d; }

    private static void loop() {
        while (true) {
            if (!enabled()) { connected = false; status = Ui.t("wyłączony", "off"); sleep(3000); continue; }
            String appId = text("ID aplikacji").replaceAll("[^0-9]", "");
            if (appId.isEmpty()) { connected = false; status = Ui.t("wpisz ID aplikacji", "enter application ID"); sleep(3000); continue; }
            try (Ipc ipc = Ipc.open()) {
                ipc.send(0, "{\"v\":1,\"client_id\":\"" + appId + "\"}");
                Frame ready = ipc.read();
                if (ready.op != 1 || !ready.json.contains("READY")) throw new IOException("handshake: " + ready.json);
                connected = true;
                status = Ui.t("połączono", "connected");
                String last = null;
                long lastSent = 0;
                while (enabled() && appId.equals(text("ID aplikacji").replaceAll("[^0-9]", ""))) {
                    String act = activity();
                    if (!act.equals(last) || System.currentTimeMillis() - lastSent > 60_000) {
                        ipc.send(1, "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":" + act + "},\"nonce\":\"" + UUID.randomUUID() + "\"}");
                        Frame r = ipc.read();
                        if (r.op == 2) throw new IOException("closed: " + r.json);
                        if (r.json.contains("\"evt\":\"ERROR\"")) status = Ui.t("błąd: sprawdź ID", "error: check the ID");
                        last = act;
                        lastSent = System.currentTimeMillis();
                    }
                    sleep(4000);
                }
                ipc.send(1, "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + "},\"nonce\":\"" + UUID.randomUUID() + "\"}");
                ipc.read();
            } catch (Exception e) {
                connected = false;
                status = Ui.t("Discord nie działa", "Discord not running");
                sleep(15_000);
            }
        }
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    /** treść statusu: tytuł = nazwa aplikacji Discord (WithClient), niżej wersje, miejsce gry, czas */
    static String activity() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String mcv = SharedConstants.getGameVersion().getName();
        String where;
        if (mc.world == null) where = Ui.t("W menu głównym", "In the main menu");
        else if (mc.isInSingleplayer()) where = Ui.t("Tryb jednoosobowy", "Singleplayer");
        else {
            ServerInfo si = mc.getCurrentServerEntry();
            where = bo("Pokaż serwer") && si != null ? Ui.t("Gra na ", "Playing on ") + si.address : Ui.t("Tryb wieloosobowy", "Multiplayer");
        }
        String custom = text("Opis");
        JsonObject a = new JsonObject();
        a.addProperty("details", "WithClient " + WithClient.VERSION + "  ·  Minecraft " + mcv);
        a.addProperty("state", custom.isEmpty() ? where : custom);
        if (bo("Pokaż czas")) { JsonObject ts = new JsonObject(); ts.addProperty("start", START); a.add("timestamps", ts); }
        JsonObject as = new JsonObject();
        String name = mc.getSession().getUsername();
        as.addProperty("large_image", mode("Duży obraz", "Logo").equals("Skin") ? "https://mc-heads.net/avatar/" + name + "/128" : "logo");
        as.addProperty("large_text", "WithClient " + WithClient.VERSION + " · Minecraft " + mcv);
        if (bo("Pokaż nick")) { as.addProperty("small_image", "https://mc-heads.net/avatar/" + name + "/64"); as.addProperty("small_text", name); }
        a.add("assets", as);
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
