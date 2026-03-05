package com.betternautilus.attribute;

import com.betternautilus.BetterNautilus;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModAttributes {

    /**
     * Dash strength determines how far the nautilus launches when dashing.
     * Clamp min/max are set wide (0.01 to 100.0) so the config's dashStrengthMax
     * (default 2.0, user-configurable up to 10.0) never hits the ceiling.
     * The actual gameplay range is controlled entirely by BetterNautilusConfig.
     *
     * MUST be tracked (setTracked(true)) so the value syncs to the client for
     * the stat HUD to display correct per-entity values.
     */
    public static final RegistryEntry<EntityAttribute> DASH_STRENGTH = register(
            "dash_strength",
            new ClampedEntityAttribute(
                    "attribute.betternautilus.dash_strength",
                    0.7,    // default (midpoint of vanilla-ish range)
                    0.01,   // min — wide enough to never interfere with config
                    100.0   // max — wide enough to never interfere with config
            ).setTracked(true)
    );

    /**
     * Attack damage for the nautilus's dash/charge attack.
     * Clamp min/max are set wide so the config's dashDamageMax (default 12.0,
     * user-configurable up to 256.0) never hits the ceiling.
     * The actual gameplay range is controlled entirely by BetterNautilusConfig.
     *
     * MUST be tracked (setTracked(true)) so the value syncs to the client for
     * the stat HUD to display correct per-entity values.
     */
    public static final RegistryEntry<EntityAttribute> DASH_ATTACK_DAMAGE = register(
            "dash_attack_damage",
            new ClampedEntityAttribute(
                    "attribute.betternautilus.dash_attack_damage",
                    4.0,    // default midpoint
                    0.0,    // min
                    1024.0  // max — wide enough to never interfere with config
            ).setTracked(true)
    );

    private static RegistryEntry<EntityAttribute> register(String id, EntityAttribute attribute) {
        return Registry.registerReference(
                Registries.ATTRIBUTE,
                Identifier.of(BetterNautilus.MOD_ID, id),
                attribute
        );
    }

    /** Called from BetterNautilus#onInitialize to trigger static initialization. */
    public static void register() {
        BetterNautilus.LOGGER.info("Registering Better Nautilus attributes");
    }
}
