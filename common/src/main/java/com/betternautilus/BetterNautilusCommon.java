package com.betternautilus;

import com.betternautilus.config.BetterNautilusConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared mod initialization called by all loader entrypoints.
 *
 * Does NOT register attributes or enchantment effect types — those are
 * loader-specific (Fabric uses direct Registry calls, NeoForge/Forge use
 * DeferredRegister). Each loader entrypoint handles its own registration
 * before or after calling init().
 */
public class BetterNautilusCommon {

    public static final String MOD_ID = "betternautilus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Better Nautilus initializing...");

        // Load config before anything else reads from it
        BetterNautilusConfig.load();

        LOGGER.info("Better Nautilus initialized.");
    }
}
