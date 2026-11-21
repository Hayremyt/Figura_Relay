package org.figurarelay;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Figura Relay — companion mod for Figura 0.1.6 (MC 1.21.1).
 *
 * On the CLIENT (with Figura installed) it patches Figura's WebSocket
 * backend out via mixins and replaces it with avatar exchange
 * through the game server.
 *
 * On the SERVER (Figura not required) it acts as the relay: stores
 * avatars, broadcasts hashes and forwards pings.
 *
 * Wire protocol is identical to the previous fork setup, so old
 * fork clients/servers stay compatible (figura:hello, figura:relay_*).
 */
public class FiguraRelay {
    public static final String MOD_ID = "figura_relay";
    public static final String MOD_NAME = "Figura Relay";
    public static final Logger LOGGER = LogUtils.getLogger();
}
