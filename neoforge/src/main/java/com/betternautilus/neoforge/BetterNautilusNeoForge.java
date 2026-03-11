package com.betternautilus.neoforge;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.client.NautilusStatHudRenderer;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

@Mod(BetterNautilusCommon.MOD_ID)
public class BetterNautilusNeoForge {

    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, BetterNautilusCommon.MOD_ID);

    private static final Supplier<Attribute> DASH_STRENGTH_DEFERRED =
            ATTRIBUTES.register("dash_strength", () -> ModAttributes.DASH_STRENGTH_ATTR);

    private static final Supplier<Attribute> DASH_ATTACK_DAMAGE_DEFERRED =
            ATTRIBUTES.register("dash_attack_damage", () -> ModAttributes.DASH_ATTACK_DAMAGE_ATTR);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final DeferredRegister<DataComponentType<?>> ENCHANTMENT_EFFECTS =
            DeferredRegister.create(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, BetterNautilusCommon.MOD_ID);

    private static final Supplier<DataComponentType<List<ConditionalEffect<NautilusPowerEffect>>>> NAUTILUS_POWER_DEFERRED =
            ENCHANTMENT_EFFECTS.register("nautilus_power", ModEnchantmentEffects::createNautilusPowerType);

    private static final Supplier<DataComponentType<List<ConditionalEffect<NautilusSwiftnessEffect>>>> NAUTILUS_SWIFTNESS_DEFERRED =
            ENCHANTMENT_EFFECTS.register("nautilus_swiftness", ModEnchantmentEffects::createNautilusSwiftnessType);

    public BetterNautilusNeoForge(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        ENCHANTMENT_EFFECTS.register(modEventBus);

        // Use EntityAttributeModificationEvent to ADD custom attributes
        // to the already-registered vanilla nautilus entity types
        modEventBus.addListener(this::onAttributeModify);

        NeoForge.EVENT_BUS.addListener(BetterNautilusNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(BetterNautilusNeoForge::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(BetterNautilusNeoForge::onRenderGui);

        BetterNautilusCommon.init();

        BetterNautilusCommon.LOGGER.info("Better Nautilus NeoForge initialized.");
    }

    @SuppressWarnings("unchecked")
    private void onAttributeModify(EntityAttributeModificationEvent event) {
        // Populate common Holder references from DeferredHolder
        ModAttributes.DASH_STRENGTH =
                (net.minecraft.core.Holder<Attribute>) DASH_STRENGTH_DEFERRED;
        ModAttributes.DASH_ATTACK_DAMAGE =
                (net.minecraft.core.Holder<Attribute>) DASH_ATTACK_DAMAGE_DEFERRED;

        // Populate enchantment effect references
        ModEnchantmentEffects.NAUTILUS_POWER = NAUTILUS_POWER_DEFERRED.get();
        ModEnchantmentEffects.NAUTILUS_SWIFTNESS = NAUTILUS_SWIFTNESS_DEFERRED.get();

        // Add our custom attributes to the vanilla nautilus and zombie nautilus
        event.add(EntityType.NAUTILUS, ModAttributes.DASH_STRENGTH);
        event.add(EntityType.NAUTILUS, ModAttributes.DASH_ATTACK_DAMAGE);
        event.add(EntityType.ZOMBIE_NAUTILUS, ModAttributes.DASH_STRENGTH);
        event.add(EntityType.ZOMBIE_NAUTILUS, ModAttributes.DASH_ATTACK_DAMAGE);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        ReloadCommand.register(event.getDispatcher());
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        DashKillDropHandler.onMobDeath(event.getEntity(), event.getSource());
    }

    private static void onRenderGui(RenderGuiLayerEvent.Post event) {
        NautilusStatHudRenderer.onHudRender(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
    }
}
