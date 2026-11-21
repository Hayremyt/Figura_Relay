package org.figurarelay.server;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Server-side entry: storage init + config load. Figura is NOT required here. */
public class RelayMain {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        RelayConfig.load();
        BanStore.load();
        AvatarStore.init();
        LOGGER.info("Figura Relay relay initialized.");
    }
}
