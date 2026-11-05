package dev.edgelyrics;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny properties-file config: .minecraft/config/edgelyrics.properties */
public final class Config {
    public static boolean enabled = true;
    public static boolean right = true;   // false = top-left
    public static int width = 190;        // GUI-scaled pixels
    public static int margin = 4;
    public static int offsetMs = 0;       // + shows lyrics earlier, - later

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("edgelyrics.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                p.load(r);
            } catch (IOException e) {
                EdgeLyricsMod.LOG.warn("Could not read config", e);
            }
        }
        enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        right = !"left".equalsIgnoreCase(p.getProperty("corner", "right"));
        width = parse(p.getProperty("width"), 190);
        margin = parse(p.getProperty("margin"), 4);
        offsetMs = parse(p.getProperty("offsetMs"), 0);

        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("corner", right ? "right" : "left");
        p.setProperty("width", String.valueOf(width));
        p.setProperty("margin", String.valueOf(margin));
        p.setProperty("offsetMs", String.valueOf(offsetMs));
        try (Writer w = Files.newBufferedWriter(file)) {
            p.store(w, "Edge Lyrics - corner: left|right, offsetMs shifts lyric timing");
        } catch (IOException ignored) {
        }
    }

    private static int parse(String s, int def) {
        try {
            return s == null ? def : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Config() {}
}
