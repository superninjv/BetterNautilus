package com.betternautilus;

import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.command.ReloadCommand;
import com.betternautilus.config.BetterNautilusConfig;
import com.betternautilus.enchantment.ModEnchantmentEffects;
import com.betternautilus.loot.DashKillDropHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterNautilus implements ModInitializer {

    public static final String MOD_ID = "betternautilus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Better Nautilus initializing...");

        // Load config before anything else reads from it
        BetterNautilusConfig.load();

        // Register custom attributes first so they exist before entity registration
        ModAttributes.register();
        ModEnchantmentEffects.register();
        DashKillDropHandler.register();

        // Add custom attributes to the nautilus entity attribute containers.
        // Without this, getAttributeInstance() returns null for our custom attributes
        // and the stat system silently does nothing — or worse, crashes on breeding.
        registerNautilusAttributes();

        // Register /betternautilus reload command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ReloadCommand.register(dispatcher));

        LOGGER.info("Better Nautilus initialized.");
    }

    private void registerNautilusAttributes() {
        // Override the attribute container for NautilusEntity to include our custom attributes.
        // We call createNautilusAttributes() to get the vanilla base set and add ours on top.
        try {
            FabricDefaultAttributeRegistry.register(
                    EntityType.NAUTILUS,
                    AbstractNautilusEntity.createNautilusAttributes()
                            .add(ModAttributes.DASH_STRENGTH)
                            .add(ModAttributes.DASH_ATTACK_DAMAGE)
            );
            LOGGER.info("Registered custom attributes on NautilusEntity");
        } catch (IllegalArgumentException e) {
            // Already registered (e.g. in a dev environment where this runs twice)
            LOGGER.warn("Nautilus attributes already registered: {}", e.getMessage());
        }

        try {
            FabricDefaultAttributeRegistry.register(
                    EntityType.ZOMBIE_NAUTILUS,
                    AbstractNautilusEntity.createNautilusAttributes()
                            .add(ModAttributes.DASH_STRENGTH)
                            .add(ModAttributes.DASH_ATTACK_DAMAGE)
            );
            LOGGER.info("Registered custom attributes on ZombieNautilusEntity");
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Zombie nautilus attributes already registered: {}", e.getMessage());
        }
    }
}
