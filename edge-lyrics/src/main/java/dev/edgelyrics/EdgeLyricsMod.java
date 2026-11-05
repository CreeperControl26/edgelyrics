package dev.edgelyrics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdgeLyricsMod implements ClientModInitializer {
    public static final String MOD_ID = "edgelyrics";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        Config.load();

        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            LOG.warn("Edge Lyrics needs Windows (it reads Windows media controls). Disabled.");
            return;
        }

        new SmtcReader().start();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"),
                LyricsHud::render);
    }
}
