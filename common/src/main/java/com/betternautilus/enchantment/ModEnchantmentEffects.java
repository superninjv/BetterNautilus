package com.betternautilus.enchantment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;

/**
 * Common enchantment effect component type definitions.
 *
 * Like ModAttributes, the actual DataComponentType references start null and
 * are populated by each loader during registration:
 *   - Fabric:   direct Registry.register()
 *   - NeoForge: DeferredRegister
 *   - Forge:    DeferredRegister
 */
public class ModEnchantmentEffects {

    // ── Holder references (populated by each loader) ─────────────────────

    public static DataComponentType<List<ConditionalEffect<NautilusPowerEffect>>> NAUTILUS_POWER;
    public static DataComponentType<List<ConditionalEffect<NautilusSwiftnessEffect>>> NAUTILUS_SWIFTNESS;

    // ── Factory methods for loaders to create the DataComponentType ───────

    public static DataComponentType<List<ConditionalEffect<NautilusPowerEffect>>> createNautilusPowerType() {
        return buildType(NautilusPowerEffect.CODEC);
    }

    public static DataComponentType<List<ConditionalEffect<NautilusSwiftnessEffect>>> createNautilusSwiftnessType() {
        return buildType(NautilusSwiftnessEffect.CODEC);
    }

    private static <T> DataComponentType<List<ConditionalEffect<T>>> buildType(MapCodec<T> codec) {
        return DataComponentType.<List<ConditionalEffect<T>>>builder()
                .persistent(ConditionalEffect.codec(codec.codec(), LootContextParamSets.ENCHANTED_ENTITY).listOf())
                .build();
    }
}
