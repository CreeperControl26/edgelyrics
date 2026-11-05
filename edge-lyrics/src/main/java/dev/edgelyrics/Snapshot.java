package dev.edgelyrics;

/** One reading of the Edge media session. position is in seconds as of receivedNanos. */
public record Snapshot(boolean active, boolean playing, String title, String artist,
                       String album, double position, double duration, long receivedNanos) {
    public static final Snapshot NONE = new Snapshot(false, false, "", "", "", 0, 0, 0);

    public String key() {
        return title + "\u0000" + artist;
    }

    public double positionNow() {
        double p = position;
        if (playing) p += (System.nanoTime() - receivedNanos) / 1_000_000_000.0;
        return p;
    }
}
