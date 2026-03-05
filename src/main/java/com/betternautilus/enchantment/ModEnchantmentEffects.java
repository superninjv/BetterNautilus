package com.betternautilus.enchantment;

import com.betternautilus.BetterNautilus;
import com.mojang.serialization.MapCodec;
import net.minecraft.component.ComponentType;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModEnchantmentEffects {

    /**
     * Custom effect component type for Nautilus Power.
     * Uses ENCHANTED_ENTITY as the loot context type, appropriate for
     * entity-targeting effects applied during a dash attack.
     */
    public static final ComponentType<List<EnchantmentEffectEntry<NautilusPowerEffect>>> NAUTILUS_POWER =
            register("nautilus_power", NautilusPowerEffect.CODEC);

    /**
     * Custom effect component type for Nautilus Swiftness.
     */
    public static final ComponentType<List<EnchantmentEffectEntry<NautilusSwiftnessEffect>>> NAUTILUS_SWIFTNESS =
            register("nautilus_swiftness", NautilusSwiftnessEffect.CODEC);

    private static <T> ComponentType<List<EnchantmentEffectEntry<T>>> register(String id, MapCodec<T> codec) {
        return Registry.register(
                Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
                Identifier.of(BetterNautilus.MOD_ID, id),
                ComponentType.<List<EnchantmentEffectEntry<T>>>builder()
                        // createCodec requires Codec<T> (not MapCodec) + a ContextType.
                        // .codec() on a MapCodec wraps it into a full Codec.
                        .codec(EnchantmentEffectEntry.createCodec(codec.codec(), LootContextTypes.ENCHANTED_ENTITY).listOf())
                        .build()
        );
    }

    /** Called from BetterNautilus#onInitialize to trigger static initialization. */
    public static void register() {
        BetterNautilus.LOGGER.info("Registering Better Nautilus enchantment effect types");
    }
}
