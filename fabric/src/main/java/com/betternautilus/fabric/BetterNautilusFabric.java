package com.betternautilus.fabric;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.command.ReloadCommand;
import com.betternautilus.enchantment.ModEnchantmentEffects;
import com.betternautilus.loot.DashKillDropHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;

public class BetterNautilusFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register attributes and enchantment effects BEFORE common init
        // (Fabric allows direct registry calls during mod init)
        registerAttributes();
        registerEnchantmentEffects();

        BetterNautilusCommon.init();

        // Register custom attributes on nautilus entities
        registerNautilusAttributes();

        // Register dash-kill drop handler via Fabric event
        ServerLivingEntityEvents.AFTER_DEATH.register(DashKillDropHandler::onMobDeath);

        // Register /betternautilus reload command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ReloadCommand.register(dispatcher));

        BetterNautilusCommon.LOGGER.info("Better Nautilus Fabric initialized.");
    }

    /**
     * Register custom attributes directly into BuiltInRegistries.ATTRIBUTE.
     * This works on Fabric because registries are still open during mod init.
     */
    private void registerAttributes() {
        ModAttributes.DASH_STRENGTH = Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                Identifier.fromNamespaceAndPath(BetterNautilusCommon.MOD_ID, "dash_strength"),
                ModAttributes.DASH_STRENGTH_ATTR
        );
        ModAttributes.DASH_ATTACK_DAMAGE = Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                Identifier.fromNamespaceAndPath(BetterNautilusCommon.MOD_ID, "dash_attack_damage"),
                ModAttributes.DASH_ATTACK_DAMAGE_ATTR
        );
        BetterNautilusCommon.LOGGER.info("Registered Better Nautilus attributes (Fabric)");
    }

    /**
     * Register custom enchantment effect component types directly.
     */
    private void registerEnchantmentEffects() {
        ModEnchantmentEffects.NAUTILUS_POWER = Registry.register(
                BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(BetterNautilusCommon.MOD_ID, "nautilus_power"),
                ModEnchantmentEffects.createNautilusPowerType()
        );
        ModEnchantmentEffects.NAUTILUS_SWIFTNESS = Registry.register(
                BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(BetterNautilusCommon.MOD_ID, "nautilus_swiftness"),
                ModEnchantmentEffects.createNautilusSwiftnessType()
        );
        BetterNautilusCommon.LOGGER.info("Registered Better Nautilus enchantment effects (Fabric)");
    }

    private void registerNautilusAttributes() {
        try {
            FabricDefaultAttributeRegistry.register(
                    EntityType.NAUTILUS,
                    AbstractNautilus.createAttributes()
                            .add(ModAttributes.DASH_STRENGTH)
                            .add(ModAttributes.DASH_ATTACK_DAMAGE)
            );
        } catch (IllegalArgumentException e) {
            BetterNautilusCommon.LOGGER.warn("Nautilus attributes already registered: {}", e.getMessage());
        }

        try {
            FabricDefaultAttributeRegistry.register(
                    EntityType.ZOMBIE_NAUTILUS,
                    AbstractNautilus.createAttributes()
                            .add(ModAttributes.DASH_STRENGTH)
                            .add(ModAttributes.DASH_ATTACK_DAMAGE)
            );
        } catch (IllegalArgumentException e) {
            BetterNautilusCommon.LOGGER.warn("Zombie nautilus attributes already registered: {}", e.getMessage());
        }
    }
}
