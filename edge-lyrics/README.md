# Edge Lyrics (Fabric 26.2, client-only)

Shows synced lyrics for Spotify Web Player playing in Microsoft Edge, in a small box in a screen corner.

- Windows only. Reads Edge's media session via a PowerShell helper (no Spotify API, no keys).
- Lyrics come from lrclib.net (free, keyless).
- Config: `.minecraft/config/edgelyrics.properties` (corner=left|right, width, offsetMs, enabled)

## Build
1. Make sure JDK 25 is installed.
2. Easiest: generate a blank 26.2 project at https://fabricmc.net/develop/template/ , then copy
   `src/` from here over its `src/` (and keep its gradle wrapper). Or use these build files and run
   `gradle wrapper` once (Gradle 9.x).
3. `./gradlew build` -> `build/libs/edge-lyrics-1.0.0.jar`, drop it in `mods/` with Fabric API.

## Build without installing Java
Push this folder to a GitHub repo; the workflow in `.github/workflows/build.yml` builds it in the cloud.
Download the jar from the run's "Artifacts" section (use the one without `-sources`).
