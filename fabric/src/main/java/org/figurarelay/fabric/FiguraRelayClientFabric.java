package org.figurarelay.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.figurarelay.FiguraRelay;

/**
 * Client entry — safe without Figura (no Figura classes referenced directly).
 * The real init lives in FiguraRelayClientInit and is loaded reflectively.
 */
public class FiguraRelayClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isModLoaded("figura")) {
            FiguraRelay.LOGGER.error("Figura Relay: Figura is not installed — client patching disabled (server relay still works).");
            return;
        }
        try {
            Class<?> c = Class.forName("org.figurarelay.fabric.FiguraRelayClientInit");
            Runnable r = (Runnable) c.getDeclaredConstructor().newInstance();
            r.run();
        } catch (Throwable t) {
            FiguraRelay.LOGGER.error("Figura Relay: failed to init client", t);
        }
    }
}
