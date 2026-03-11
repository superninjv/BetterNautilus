package com.betternautilus.enchantment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Enchantment effect component for Nautilus Power.
 *
 * <p>Each level of Nautilus Power adds {@code damagePerLevel} to the nautilus's
 * dash attack. The mixin in {@link com.betternautilus.mixin.AbstractNautilusEntityMixin}
 * iterates enchantments on the equipped armor, sums all NAUTILUS_POWER effects,
 * and adds the total to the outgoing dash damage.
 *
 * <p>JSON format:
 * <pre>
 * {
 *   "type": "betternautilus:nautilus_power",
 *   "damage_per_level": 1.5
 * }
 * </pre>
 */
public record NautilusPowerEffect(float damagePerLevel) {

    public static final MapCodec<NautilusPowerEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.FLOAT
                            .fieldOf("damage_per_level")
                            .forGetter(NautilusPowerEffect::damagePerLevel)
            ).apply(instance, NautilusPowerEffect::new)
    );

    /**
     * Returns the total bonus damage for the given enchantment level.
     *
     * @param level the enchantment level (1-based)
     * @return bonus damage to add to the dash attack
     */
    public float getBonusDamage(int level) {
        return damagePerLevel * level;
    }
}
