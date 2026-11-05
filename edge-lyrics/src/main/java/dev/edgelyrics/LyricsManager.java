package dev.edgelyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fetches time-synced (LRC) lyrics from lrclib.net - free, no API key. */
public final class LyricsManager {
    public record Line(double time, String text) {}

    public static volatile List<Line> lines = List.of();
    public static volatile String status = "";   // "", "loading", "none", "ok"

    private static volatile String currentKey = "";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();
    private static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");

    public static void onSnapshot(Snapshot s) {
        if (s.title().isBlank() || s.key().equals(currentKey)) return;
        currentKey = s.key();
        lines = List.of();
        status = "loading";
        Thread t = new Thread(() -> fetch(s), "EdgeLyrics-Fetch");
        t.setDaemon(true);
        t.start();
    }

    private static void fetch(Snapshot s) {
        String key = s.key();
        try {
            String synced = exact(s);
            if (synced == null) synced = search(s);
            if (!key.equals(currentKey)) return; // song changed while we were fetching
            if (synced == null || synced.isBlank()) {
                status = "none";
                return;
            }
            lines = parse(synced);
            status = lines.isEmpty() ? "none" : "ok";
        } catch (Exception e) {
            if (key.equals(currentKey)) status = "none";
            EdgeLyricsMod.LOG.warn("Lyrics fetch failed", e);
        }
    }

    private static String exact(Snapshot s) throws Exception {
        StringBuilder u = new StringBuilder("https://lrclib.net/api/get?artist_name=")
                .append(enc(s.artist())).append("&track_name=").append(enc(s.title()));
        if (!s.album().isBlank()) u.append("&album_name=").append(enc(s.album()));
        if (s.duration() > 0) u.append("&duration=").append(Math.round(s.duration()));
        String body = get(u.toString());
        if (body == null) return null;
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        return synced(o);
    }

    private static String search(Snapshot s) throws Exception {
        String body = get("https://lrclib.net/api/search?q=" + enc(s.artist() + " " + s.title()));
        if (body == null) return null;
        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
        for (JsonElement e : arr) {
            String l = synced(e.getAsJsonObject());
            if (l != null && !l.isBlank()) return l;
        }
        return null;
    }

    private static String synced(JsonObject o) {
        JsonElement e = o.get("syncedLyrics");
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "EdgeLyrics-FabricMod/1.0")
                .GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return res.statusCode() == 200 ? res.body() : null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static List<Line> parse(String lrc) {
        List<Line> out = new ArrayList<>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = TS.matcher(raw);
            List<Double> times = new ArrayList<>();
            int end = 0;
            while (m.find()) {
                times.add(Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2)));
                end = m.end();
            }
            if (times.isEmpty()) continue;
            String text = raw.substring(end).trim();
            for (double t : times) out.add(new Line(t, text));
        }
        out.sort(Comparator.comparingDouble(Line::time));
        return List.copyOf(out);
    }

    private LyricsManager() {}
}
