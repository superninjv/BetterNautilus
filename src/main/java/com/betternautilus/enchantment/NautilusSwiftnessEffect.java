package com.betternautilus.enchantment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Enchantment effect component for Nautilus Swiftness.
 *
 * <p>Each level of Nautilus Swiftness multiplies the nautilus's movement speed
 * attribute by {@code (1 + speedBonusPerLevel * level)} while the nautilus is
 * ridden by a player. The mixin in
 * {@link com.betternautilus.mixin.AbstractNautilusEntityMixin} applies this as a
 * temporary attribute modifier each tick the nautilus has a rider.
 *
 * <p>JSON format:
 * <pre>
 * {
 *   "type": "betternautilus:nautilus_swiftness",
 *   "speed_bonus_per_level": 0.15
 * }
 * </pre>
 */
public record NautilusSwiftnessEffect(float speedBonusPerLevel) {

    public static final MapCodec<NautilusSwiftnessEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.FLOAT
                            .fieldOf("speed_bonus_per_level")
                            .forGetter(NautilusSwiftnessEffect::speedBonusPerLevel)
            ).apply(instance, NautilusSwiftnessEffect::new)
    );

    /**
     * Returns the multiplicative speed bonus for the given enchantment level.
     * The caller should multiply the base speed by {@code (1 + getSpeedMultiplier(level))}.
     *
     * @param level enchantment level (1-based)
     * @return fractional bonus to add to 1.0 before multiplying speed
     */
    public float getSpeedMultiplier(int level) {
        return speedBonusPerLevel * level;
    }
}
