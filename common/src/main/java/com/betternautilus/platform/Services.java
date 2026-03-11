package com.betternautilus.platform;

import java.util.ServiceLoader;

/**
 * Loads platform-specific service implementations via Java ServiceLoader.
 */
public class Services {

    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "Failed to load service for " + clazz.getName() +
                        ". This usually means the mod was not installed correctly for this loader."));
    }
}
