package com.betternautilus.config;

import com.betternautilus.BetterNautilusCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.betternautilus.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON config for Better Nautilus.
 *
 * Written to: config/betternautilus.json
 * Created with defaults on first launch, never crashes if malformed
 * (falls back to defaults and logs a warning).
 *
 * All values are read via BetterNautilusConfig.get() which is loaded once
 * at mod init and accessible statically everywhere.
 */
public class BetterNautilusConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "betternautilus.json";

    private static BetterNautilusConfig INSTANCE = null;

    // =========================================================================
    // CONFIG FIELDS — these map directly to/from JSON
    // =========================================================================

    // --- Feature toggles ---
    /** When false, nautilus armor crafting recipes produce no output and are hidden from the recipe book. */
    public boolean recipesEnabled = true;
    /** When false, nautilus armor cannot be enchanted in Survival, enchantment effects do nothing,
     *  and enchanted book dash-kill drops are disabled. Existing enchantments stay on items but are inert. */
    public boolean enchantmentsEnabled = true;

    // --- Stat ranges for spawning and breeding ---
    // The full range is used by the breeding formula (third random roll can reach healthMax etc.)
    // Wild spawns are capped at spawnXxxMax, which defaults to the 2-star point of the full range.
    public double healthMin        = 12.0;
    public double healthMax        = 50.0;
    /** Wild-spawn cap. Defaults to 2-star point (25% up the full range). Breeding can exceed this. */
    public double spawnHealthMax   = 20.0;

    public double speedMin         = 0.5;
    public double speedMax         = 2.45;
    /** Wild-spawn cap. Defaults to 2-star point. */
    public double spawnSpeedMax    = 1.0;

    public double dashStrengthMin  = 0.2;
    public double dashStrengthMax  = 2.0;
    /** Wild-spawn cap. Defaults to 2-star point. */
    public double spawnDashStrengthMax = 1.0;

    public double dashDamageMin    = 2.0;
    public double dashDamageMax    = 12.0;
    /** Wild-spawn cap. Defaults to 2-star point. */
    public double spawnDashDamageMax = 6.0;

    // --- Enchanted book drop from dash kills ---
    public boolean enchantedBookDropEnabled = true;
    public double  enchantedBookDropChance  = 0.01; // 0.0 to 1.0

    // --- Breeding tuning ---
    /**
     * Controls offspring stat variance. Each child stat = avg(parents) +/- random mutation.
     * The mutation range is this fraction of the full stat range.
     *   0.15 = slow grind (~18 gen to all 5-star)
     *   0.25 = moderate   (~12 gen to all 5-star)  [default]
     *   0.35 = fast        (~9 gen to all 5-star)
     * Higher = more variance per generation = faster breeding progress.
     */
    public double breedingMutationRange = 0.25;

    // =========================================================================
    // LOAD / SAVE
    // =========================================================================

    public static BetterNautilusConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path configDir  = Services.PLATFORM.getConfigDir();
        Path configFile = configDir.resolve(FILE_NAME);

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                INSTANCE = GSON.fromJson(json, BetterNautilusConfig.class);
                if (INSTANCE == null) {
                    BetterNautilusCommon.LOGGER.warn(
                            "[BetterNautilus] Config file was empty or null — using defaults.");
                    INSTANCE = new BetterNautilusConfig();
                }
                INSTANCE.clamp();
                BetterNautilusCommon.LOGGER.info("[BetterNautilus] Config loaded from {}", configFile);
            } catch (Exception e) {
                BetterNautilusCommon.LOGGER.error(
                        "[BetterNautilus] Failed to read config, using defaults: {}", e.getMessage());
                INSTANCE = new BetterNautilusConfig();
            }
            // Always re-save so new fields added in updates appear in the file
            save(configFile);
        } else {
            INSTANCE = new BetterNautilusConfig();
            save(configFile);
            BetterNautilusCommon.LOGGER.info(
                    "[BetterNautilus] Config file not found — created defaults at {}", configFile);
        }
    }

    private static void save(Path configFile) {
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            BetterNautilusCommon.LOGGER.error(
                    "[BetterNautilus] Failed to save config: {}", e.getMessage());
        }
    }

    /**
     * Sanity-clamps all values after loading so bad inputs don't crash entity
     * attribute assignment (RangedAttribute will throw if base > max).
     * Logs a warning for any value that needed clamping.
     */
    private void clamp() {
        healthMin          = clampField("healthMin",          healthMin,          1.0,  1024.0);
        healthMax          = clampField("healthMax",          healthMax,          healthMin, 1024.0);
        spawnHealthMax     = clampField("spawnHealthMax",     spawnHealthMax,     healthMin, healthMax);

        speedMin           = clampField("speedMin",           speedMin,           0.01, 10.0);
        speedMax           = clampField("speedMax",           speedMax,           speedMin,  10.0);
        spawnSpeedMax      = clampField("spawnSpeedMax",      spawnSpeedMax,      speedMin, speedMax);

        dashStrengthMin    = clampField("dashStrengthMin",    dashStrengthMin,    0.01, 10.0);
        dashStrengthMax    = clampField("dashStrengthMax",    dashStrengthMax,    dashStrengthMin, 10.0);
        spawnDashStrengthMax = clampField("spawnDashStrengthMax", spawnDashStrengthMax, dashStrengthMin, dashStrengthMax);

        dashDamageMin      = clampField("dashDamageMin",      dashDamageMin,      0.0,  256.0);
        dashDamageMax      = clampField("dashDamageMax",      dashDamageMax,      dashDamageMin, 256.0);
        spawnDashDamageMax = clampField("spawnDashDamageMax", spawnDashDamageMax, dashDamageMin, dashDamageMax);

        enchantedBookDropChance = clampField("enchantedBookDropChance",
                enchantedBookDropChance, 0.0, 1.0);

        breedingMutationRange = clampField("breedingMutationRange",
                breedingMutationRange, 0.01, 0.5);
    }

    private double clampField(String name, double value, double min, double max) {
        if (value < min) {
            BetterNautilusCommon.LOGGER.warn(
                    "[BetterNautilus] Config value '{}' ({}) is below minimum ({}), clamping.",
                    name, value, min);
            return min;
        }
        if (value > max) {
            BetterNautilusCommon.LOGGER.warn(
                    "[BetterNautilus] Config value '{}' ({}) is above maximum ({}), clamping.",
                    name, value, max);
            return max;
        }
        return value;
    }
}
