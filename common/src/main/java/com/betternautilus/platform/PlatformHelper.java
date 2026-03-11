package com.betternautilus.platform;

import java.nio.file.Path;

/**
 * Abstraction layer for loader-specific functionality.
 * Each loader (Fabric, Forge, NeoForge) provides an implementation
 * via Java ServiceLoader.
 */
public interface PlatformHelper {

    /**
     * Returns the platform-specific config directory path.
     * Fabric: FabricLoader.getInstance().getConfigDir()
     * Forge/NeoForge: FMLPaths.CONFIGDIR.get()
     */
    Path getConfigDir();
}
