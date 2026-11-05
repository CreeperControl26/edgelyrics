package dev.edgelyrics;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public final class LyricsHud {
    private static final int BG = 0xA0101010;
    private static final int DIM = 0xFF8A8A8A;
    private static final int CUR = 0xFF1ED760; // Spotify green
    private static final int HEAD = 0xFFCCCCCC;

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        if (!Config.enabled) return;
        Snapshot s = SmtcReader.current;
        if (!s.active() || s.title().isBlank()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int pad = 4;
        int w = Config.width;
        int rowH = font.lineHeight + 3;
        int h = pad * 2 + rowH * 4;
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = Config.right ? sw - w - Config.margin : Config.margin;
        int y = Config.margin;
        int inner = w - pad * 2;

        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + 2, y + h, CUR);

        String head = s.artist().isBlank() ? s.title() : s.title() + " - " + s.artist();
        text(g, font, fit(font, (s.playing() ? "" : "|| ") + head, inner), x + pad + 2, y + pad, HEAD);

        String prev = "", cur = "", next = "";
        switch (LyricsManager.status) {
            case "loading" -> cur = "Loading lyrics...";
            case "none" -> cur = "No synced lyrics found";
            case "ok" -> {
                List<LyricsManager.Line> ls = LyricsManager.lines;
                double pos = s.positionNow() + Config.offsetMs / 1000.0;
                int idx = -1;
                for (int i = 0; i < ls.size(); i++) {
                    if (ls.get(i).time() <= pos) idx = i; else break;
                }
                if (idx >= 1) prev = ls.get(idx - 1).text();
                if (idx >= 0) cur = ls.get(idx).text();
                if (idx + 1 < ls.size()) next = ls.get(idx + 1).text();
                if (cur.isBlank()) cur = "\u266A";
            }
            default -> {}
        }

        int tx = x + pad + 2;
        text(g, font, fit(font, prev, inner), tx, y + pad + rowH, DIM);
        text(g, font, fit(font, cur, inner), tx, y + pad + rowH * 2, CUR);
        text(g, font, fit(font, next, inner), tx, y + pad + rowH * 3, DIM);
    }

    /** All text drawing goes through here so a rename in the GUI API only needs one fix. */
    private static void text(GuiGraphicsExtractor g, Font font, String str, int x, int y, int color) {
        g.text(font, str, x, y, color, true);
    }

    private static String fit(Font font, String s, int maxW) {
        if (s == null || s.isEmpty() || font.width(s) <= maxW) return s == null ? "" : s;
        String ell = "...";
        int n = s.length();
        while (n > 0 && font.width(s.substring(0, n)) + font.width(ell) > maxW) n--;
        return s.substring(0, n) + ell;
    }

    private LyricsHud() {}
}
