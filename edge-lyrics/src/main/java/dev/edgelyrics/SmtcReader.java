package dev.edgelyrics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Runs smtc.ps1 and turns its JSON lines into Snapshots. Windows only. */
public final class SmtcReader implements Runnable {
    public static volatile Snapshot current = Snapshot.NONE;

    private Process process;
    private String lastKey = "";
    private double fallbackPos = 0;
    private long lastNanos = System.nanoTime();

    public void start() {
        Thread t = new Thread(this, "EdgeLyrics-SMTC");
        t.setDaemon(true);
        t.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (process != null) process.destroyForcibly();
        }));
    }

    @Override
    public void run() {
        while (true) {
            try {
                Path script = Files.createTempFile("edgelyrics", ".ps1");
                script.toFile().deleteOnExit();
                try (InputStream in = SmtcReader.class.getResourceAsStream("/smtc.ps1")) {
                    Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
                }
                process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                        "-ExecutionPolicy", "Bypass", "-File", script.toString())
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) handle(line.trim());
                }
            } catch (Exception e) {
                EdgeLyricsMod.LOG.warn("SMTC reader error", e);
            }
            current = Snapshot.NONE;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void handle(String line) {
        if (line.isEmpty() || line.charAt(0) != '{') return;
        try {
            JsonObject o = JsonParser.parseString(line).getAsJsonObject();
            long now = System.nanoTime();
            if (!bool(o, "active")) {
                current = Snapshot.NONE;
                return;
            }
            Snapshot s = new Snapshot(true, bool(o, "playing"), str(o, "title"), str(o, "artist"),
                    str(o, "album"), num(o, "position"), num(o, "duration"), now);

            // Some pages don't report a timeline; then keep our own clock for the track.
            if (s.duration() <= 0) {
                double dt = (now - lastNanos) / 1_000_000_000.0;
                if (!s.key().equals(lastKey)) fallbackPos = 0;
                else if (s.playing()) fallbackPos += dt;
                s = new Snapshot(true, s.playing(), s.title(), s.artist(), s.album(),
                        fallbackPos, 0, now);
            }
            lastNanos = now;
            lastKey = s.key();
            current = s;
            LyricsManager.onSnapshot(s);
        } catch (Exception ignored) {
        }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    private static double num(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? 0 : e.getAsDouble();
    }

    private static boolean bool(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }
}
