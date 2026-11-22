package dev.with.client.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Co teraz gra (element HUD "Spotify"): tytuł, wykonawca, okładka, postęp i sterowanie.
 * Windows – systemowa sesja multimediów (Spotify, przeglądarka…), macOS – Spotify przez AppleScript,
 * Linux – playerctl (MPRIS). Opcjonalnie zsynchronizowany tekst piosenki z lrclib.net.
 */
public final class Media {
    private Media() {}

    public static volatile String title = "", artist = "";
    public static volatile double position, duration;
    public static volatile boolean playing, active;
    private static volatile long stamp;
    private static volatile String artSource = "";
    private static String loadedArt = "";
    public static final Identifier ART = Identifier.of("withclient", "dynamic/media_art");
    public static volatile boolean hasArt;
    private static NativeImageBackedTexture artTex;

    private record Line(double t, String text) {}
    private static volatile List<Line> lyrics = List.of();
    private static volatile String lyricsKey = "";

    private static Thread thread;
    private static volatile boolean running;
    private static Process ps;

    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WIN = OS.contains("win"), MAC = OS.contains("mac");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();

    /** pozycja z dociągnięciem czasu od ostatniego odczytu */
    public static double pos() {
        double p = position + (playing ? (System.currentTimeMillis() - stamp) / 1000.0 : 0);
        return duration > 0 ? Math.min(p, duration) : p;
    }

    /** aktualna linijka tekstu (albo "") */
    public static String lyric() {
        List<Line> l = lyrics;
        if (l.isEmpty()) return "";
        double p = pos() + 0.3;
        String cur = "";
        for (Line x : l) { if (x.t() <= p) cur = x.text(); else break; }
        return cur;
    }

    /** uruchamia odczyt w tle (wywoływane przy rysowaniu elementu) */
    public static synchronized void ensure() {
        if (running) return;
        running = true;
        thread = new Thread(Media::loop, "WithClient-Media");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean enabled() { Module m = Modules.get("Spotify"); return m != null && m.isEnabled(); }

    private static void loop() {
        try {
            while (running) {
                if (!enabled()) { stopProcess(); active = false; sleep(2000); continue; }
                try {
                    if (WIN) readWindows();
                    else if (MAC) parse(run(new String[]{"osascript", "-e", MAC_SCRIPT}), true);
                    else parse(run(new String[]{"playerctl", "metadata", "--format",
                            "{{title}}\t{{artist}}\t{{position}}\t{{mpris:length}}\t{{status}}\t{{mpris:artUrl}}"}), false);
                } catch (Exception e) { active = false; sleep(3000); }
                sleep(1000);
                updateLyrics();
            }
        } finally { stopProcess(); running = false; }
    }

    // ------------------------------------------------------------------ Windows (stały proces PowerShell)

    private static BufferedReader psOut;

    private static void readWindows() throws IOException {
        if (ps == null || !ps.isAlive()) {
            Path script = Files.createTempFile("withclient_media", ".ps1");
            Files.writeString(script, WIN_SCRIPT, StandardCharsets.UTF_8);
            ps = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script.toString())
                    .redirectErrorStream(false).start();
            psOut = new BufferedReader(new InputStreamReader(ps.getInputStream(), StandardCharsets.UTF_8));
        }
        String line = null;
        while (psOut.ready()) line = psOut.readLine();
        if (line == null) line = psOut.readLine();
        if (line == null) { stopProcess(); return; }
        if (!line.startsWith("M\t")) { active = false; return; }
        String[] p = line.split("\t", -1);
        if (p.length < 7) return;
        set(p[1], p[2], num(p[3]), num(p[4]), p[5].equalsIgnoreCase("Playing"), p[6].isEmpty() ? null : p[6]);
    }

    private static void stopProcess() {
        if (ps != null) { ps.destroy(); ps = null; psOut = null; }
    }

    private static final String WIN_SCRIPT = String.join("\n",
            "$ErrorActionPreference='SilentlyContinue'",
            "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8",
            "Add-Type -AssemblyName System.Runtime.WindowsRuntime",
            "$at=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'})[0]",
            "function Aw($op,$t){$x=$at.MakeGenericMethod($t).Invoke($null,@($op));$x.Wait(-1)|Out-Null;$x.Result}",
            "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]|Out-Null",
            "[Windows.Storage.Streams.IRandomAccessStreamWithContentType,Windows.Storage.Streams,ContentType=WindowsRuntime]|Out-Null",
            "$m=Aw ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
            "$last=''",
            "while($true){",
            " $s=$m.GetCurrentSession()",
            " if($s){",
            "  $p=Aw ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
            "  $t=$s.GetTimelineProperties(); $st=[string]$s.GetPlaybackInfo().PlaybackStatus",
            "  $key=\"$($p.Title)|$($p.Artist)\"; $art=''",
            "  if($key -ne $last -and $p.Thumbnail){",
            "   $f=Join-Path $env:TEMP ('withclient_art_'+[Math]::Abs($key.GetHashCode())+'.img')",
            "   $rs=Aw ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])",
            "   $ns=[System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($rs)",
            "   $fs=[System.IO.File]::Create($f); $ns.CopyTo($fs); $fs.Close(); $ns.Close(); $art=$f; $last=$key }",
            "  $pos=$t.Position.TotalSeconds",
            "  if($st -eq 'Playing'){ $pos+=([DateTimeOffset]::Now-$t.LastUpdatedTime).TotalSeconds }",
            "  $ti=\"$($p.Title)\" -replace \"`t\",' '; $ar=\"$($p.Artist)\" -replace \"`t\",' '",
            "  [Console]::Out.WriteLine(\"M`t$ti`t$ar`t$([string]::Format([Globalization.CultureInfo]::InvariantCulture,'{0:0.00}',$pos))`t$([string]::Format([Globalization.CultureInfo]::InvariantCulture,'{0:0.00}',$t.EndTime.TotalSeconds))`t$st`t$art\")",
            " } else { [Console]::Out.WriteLine('N') }",
            " [Console]::Out.Flush()",
            " Start-Sleep -Milliseconds 900",
            "}");

    // ------------------------------------------------------------------ macOS / Linux

    private static final String MAC_SCRIPT = String.join("\n",
            "if application \"Spotify\" is running then",
            " tell application \"Spotify\"",
            "  set t to current track",
            "  return (name of t) & tab & (artist of t) & tab & (player position as string) & tab & ((duration of t) / 1000 as string) & tab & (player state as string) & tab & (artwork url of t)",
            " end tell",
            "end if");

    private static void parse(String out, boolean mac) {
        if (out == null || out.isBlank()) { active = false; return; }
        String[] p = out.trim().split("\t", -1);
        if (p.length < 6) { active = false; return; }
        double pos = num(p[2]), dur = num(p[3]);
        if (!mac) { pos /= 1e6; dur /= 1e6; }
        String state = p[4].toLowerCase(Locale.ROOT);
        set(p[0], p[1], pos, dur, state.contains("play"), p[5].isEmpty() ? null : p[5]);
    }

    private static void set(String t, String a, double pos, double dur, boolean play, String art) {
        title = t; artist = a; position = pos; duration = dur; playing = play; stamp = System.currentTimeMillis();
        active = !t.isBlank();
        if (art != null && !art.equals(artSource)) { artSource = art; loadArt(art); }
    }

    private static double num(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return 0; }
    }

    private static String run(String[] cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            if (!p.waitFor(3, TimeUnit.SECONDS)) { p.destroyForcibly(); return null; }
            if (p.exitValue() != 0) return null;
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { return null; }
    }

    // ------------------------------------------------------------------ sterowanie

    public static void playPause() { control(179, "playpause", "play-pause"); playing = !playing; stamp = System.currentTimeMillis(); }
    public static void next() { control(176, "next track", "next"); }
    public static void previous() { control(177, "previous track", "previous"); }

    private static void control(int vk, String mac, String linux) {
        Thread t = new Thread(() -> {
            if (WIN) run(new String[]{"powershell", "-NoProfile", "-Command", "(New-Object -ComObject WScript.Shell).SendKeys([char]" + vk + ")"});
            else if (MAC) run(new String[]{"osascript", "-e", "tell application \"Spotify\" to " + mac});
            else run(new String[]{"playerctl", linux});
        }, "WithClient-MediaCtl");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------ okładka

    private static void loadArt(String src) {
        try {
            byte[] data;
            if (src.startsWith("http")) {
                HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(src)).timeout(Duration.ofSeconds(8)).build(), HttpResponse.BodyHandlers.ofByteArray());
                data = r.body();
            } else {
                Path p = src.startsWith("file://") ? Path.of(URI.create(src)) : Path.of(src);
                data = Files.readAllBytes(p);
            }
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return;
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(img, "png", png);
            byte[] bytes = png.toByteArray();
            MinecraftClient.getInstance().execute(() -> {
                try {
                    NativeImage ni = NativeImage.read(new ByteArrayInputStream(bytes));
                    if (artTex != null) artTex.close();
                    artTex = new NativeImageBackedTexture(ni);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(ART, artTex);
                    hasArt = true;
                    loadedArt = src;
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ tekst piosenki (lrclib.net)

    private static void updateLyrics() {
        Module m = Modules.get("Spotify");
        boolean want = m != null && m.setting("Tekst piosenki") instanceof Setting.Bool b && b.get();
        String key = title + "|" + artist;
        if (!want || !active || key.equals(lyricsKey)) return;
        lyricsKey = key;
        lyrics = List.of();
        try {
            String url = "https://lrclib.net/api/get?track_name=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                    + "&artist_name=" + URLEncoder.encode(artist, StandardCharsets.UTF_8)
                    + (duration > 0 ? "&duration=" + Math.round(duration) : "");
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "WithClient (Minecraft mod)").build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return;
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return;
            List<Line> out = new ArrayList<>();
            for (String l : o.get("syncedLyrics").getAsString().split("\n")) {
                int close = l.indexOf(']');
                if (!l.startsWith("[") || close < 0) continue;
                String[] ts = l.substring(1, close).split(":");
                if (ts.length != 2) continue;
                try {
                    double t = Integer.parseInt(ts[0]) * 60 + Double.parseDouble(ts[1]);
                    out.add(new Line(t, l.substring(close + 1).trim()));
                } catch (NumberFormatException ignored) {}
            }
            if (key.equals(title + "|" + artist)) lyrics = out;
        } catch (Exception ignored) {}
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}
