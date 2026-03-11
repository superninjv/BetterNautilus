package com.betternautilus.attribute;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.Holder;

/**
 * Common attribute definitions for Better Nautilus.
 *
 * The actual Attribute *objects* are created here so all loaders share the same
 * instances, but REGISTRATION into the Minecraft registry is handled per-loader:
 *   - Fabric:   direct Registry.registerForHolder() in BetterNautilusFabric
 *   - NeoForge: DeferredRegister in BetterNautilusNeoForge
 *   - Forge:    DeferredRegister in BetterNautilusForge
 *
 * The Holder references start null and are populated by the loader before any
 * game logic runs. Mixins access these at runtime (during ticks), long after
 * registration is complete, so this is safe.
 */
public class ModAttributes {

    // ── Attribute instances (shared, not yet registered) ─────────────────

    public static final Attribute DASH_STRENGTH_ATTR = new RangedAttribute(
            "attribute.betternautilus.dash_strength",
            0.7, 0.01, 100.0
    ).setSyncable(false);

    public static final Attribute DASH_ATTACK_DAMAGE_ATTR = new RangedAttribute(
            "attribute.betternautilus.dash_attack_damage",
            4.0, 0.0, 1024.0
    ).setSyncable(false);

    // ── Holder references (populated by each loader during registration) ─

    public static Holder<Attribute> DASH_STRENGTH;
    public static Holder<Attribute> DASH_ATTACK_DAMAGE;
}
