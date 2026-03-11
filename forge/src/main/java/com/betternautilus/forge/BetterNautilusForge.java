package com.betternautilus.forge;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.command.ReloadCommand;
import com.betternautilus.enchantment.ModEnchantmentEffects;
import com.betternautilus.enchantment.NautilusPowerEffect;
import com.betternautilus.enchantment.NautilusSwiftnessEffect;
import com.betternautilus.loot.DashKillDropHandler;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

/**
 * Forge entrypoint for Better Nautilus.
 *
 * Uses Forge 61.x / EventBus 7 API:
 *   - DeferredRegister.register(BusGroup) for registry
 *   - EventName.BUS.addListener() for event subscription
 *   - RegistryObject.getHolder().orElseThrow() to get Holder&lt;Attribute&gt;
 */
@Mod(BetterNautilusCommon.MOD_ID)
public class BetterNautilusForge {

    // ── Attribute registration ───────────────────────────────────────────

    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, BetterNautilusCommon.MOD_ID);

    private static final RegistryObject<Attribute> DASH_STRENGTH_DEFERRED =
            ATTRIBUTES.register("dash_strength", () -> ModAttributes.DASH_STRENGTH_ATTR);

    private static final RegistryObject<Attribute> DASH_ATTACK_DAMAGE_DEFERRED =
            ATTRIBUTES.register("dash_attack_damage", () -> ModAttributes.DASH_ATTACK_DAMAGE_ATTR);

    // ── Enchantment effect component registration ────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final DeferredRegister<DataComponentType<?>> ENCHANTMENT_EFFECTS =
            DeferredRegister.create(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, BetterNautilusCommon.MOD_ID);

    private static final Supplier<DataComponentType<List<ConditionalEffect<NautilusPowerEffect>>>> NAUTILUS_POWER_DEFERRED =
            ENCHANTMENT_EFFECTS.register("nautilus_power", ModEnchantmentEffects::createNautilusPowerType);

    private static final Supplier<DataComponentType<List<ConditionalEffect<NautilusSwiftnessEffect>>>> NAUTILUS_SWIFTNESS_DEFERRED =
            ENCHANTMENT_EFFECTS.register("nautilus_swiftness", ModEnchantmentEffects::createNautilusSwiftnessType);

    // ── Constructor ──────────────────────────────────────────────────────

    public BetterNautilusForge(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register deferred registries with the mod bus group
        ATTRIBUTES.register(modBusGroup);
        ENCHANTMENT_EFFECTS.register(modBusGroup);

        // Mod lifecycle events (via their static BUS fields)
        EntityAttributeModificationEvent.BUS.addListener(BetterNautilusForge::onAttributeModify);

        // Game events
        RegisterCommandsEvent.BUS.addListener(BetterNautilusForge::onRegisterCommands);
        LivingDeathEvent.BUS.addListener(BetterNautilusForge::onLivingDeath);

        // Client HUD overlay (event only fires on logical client, safe to register always)
        AddGuiOverlayLayersEvent.BUS.addListener(BetterNautilusForgeClient::onAddGuiLayers);

        BetterNautilusCommon.init();

        BetterNautilusCommon.LOGGER.info("Better Nautilus Forge initialized.");
    }

    // ── Entity attribute modification ────────────────────────────────────

    private static void onAttributeModify(EntityAttributeModificationEvent event) {
        // Populate common Holder references from RegistryObject
        ModAttributes.DASH_STRENGTH = DASH_STRENGTH_DEFERRED.getHolder().orElseThrow(
                () -> new IllegalStateException("DASH_STRENGTH holder not available"));
        ModAttributes.DASH_ATTACK_DAMAGE = DASH_ATTACK_DAMAGE_DEFERRED.getHolder().orElseThrow(
                () -> new IllegalStateException("DASH_ATTACK_DAMAGE holder not available"));

        // Populate enchantment effect references
        ModEnchantmentEffects.NAUTILUS_POWER = NAUTILUS_POWER_DEFERRED.get();
        ModEnchantmentEffects.NAUTILUS_SWIFTNESS = NAUTILUS_SWIFTNESS_DEFERRED.get();

        // Add our custom attributes to vanilla nautilus and zombie nautilus
        event.add(EntityType.NAUTILUS, ModAttributes.DASH_STRENGTH);
        event.add(EntityType.NAUTILUS, ModAttributes.DASH_ATTACK_DAMAGE);
        event.add(EntityType.ZOMBIE_NAUTILUS, ModAttributes.DASH_STRENGTH);
        event.add(EntityType.ZOMBIE_NAUTILUS, ModAttributes.DASH_ATTACK_DAMAGE);

        BetterNautilusCommon.LOGGER.info("Added custom attributes to nautilus entities (Forge)");
    }

    // ── Game events ──────────────────────────────────────────────────────

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        ReloadCommand.register(event.getDispatcher());
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        DashKillDropHandler.onMobDeath(event.getEntity(), event.getSource());
    }
}
