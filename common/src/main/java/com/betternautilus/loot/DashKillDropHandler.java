package com.betternautilus.loot;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Handles enchanted book drops when a mob is killed by a dashing nautilus.
 *
 * The onMobDeath method is a plain static method — each loader registers it
 * with its own death event system (Fabric: ServerLivingEntityEvents,
 * Forge: LivingDeathEvent, NeoForge: LivingDeathEvent).
 */
public class DashKillDropHandler {

    private static final List<EnchantPool> POOL = List.of(
            new EnchantPool("betternautilus", "nautilus_protection", 4, 5),
            new EnchantPool("betternautilus", "nautilus_power",      3, 4),
            new EnchantPool("betternautilus", "nautilus_swiftness",  3, 4)
    );

    /**
     * Called by each loader's death event. Does NOT self-register;
     * registration is handled per-loader in BetterNautilusFabric,
     * BetterNautilusForge, and BetterNautilusNeoForge.
     */
    public static void onMobDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel serverWorld)) return;

        // The attacker must be a dashing nautilus
        if (!(source.getEntity() instanceof AbstractNautilus nautilus)) return;
        if (!DashTracker.isDashing(nautilus)) return;

        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        if (!cfg.enchantmentsEnabled) return;
        if (!cfg.enchantedBookDropEnabled) return;

        RandomSource random = serverWorld.getRandom();
        if (random.nextFloat() > cfg.enchantedBookDropChance) return;

        ItemStack book = createEnchantedBook(serverWorld, random);
        if (book == null) return;

        victim.spawnAtLocation(serverWorld, book);
    }

    private static ItemStack createEnchantedBook(ServerLevel world, RandomSource random) {
        record Option(String namespace, String path, int level, int weight) { }
        List<Option> options = new java.util.ArrayList<>();
        for (EnchantPool pool : POOL) {
            for (int level = 1; level <= pool.maxLevel; level++) {
                options.add(new Option(pool.namespace, pool.path, level, pool.weight));
            }
        }

        int totalWeight = options.stream().mapToInt(o -> o.weight).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        Option chosen = options.get(0);
        for (Option opt : options) {
            cumulative += opt.weight;
            if (roll < cumulative) {
                chosen = opt;
                break;
            }
        }

        var registryOptional = world.registryAccess()
                .lookup(Registries.ENCHANTMENT);
        if (registryOptional.isEmpty()) return null;
        var enchantmentRegistry = registryOptional.get();

        var enchantmentEntry = enchantmentRegistry.get(Identifier.fromNamespaceAndPath(chosen.namespace, chosen.path));
        if (enchantmentEntry.isEmpty()) {
            BetterNautilusCommon.LOGGER.warn("Could not find enchantment {}:{} for book drop", chosen.namespace, chosen.path);
            return null;
        }

        final Option finalChosen = chosen;
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(
                book,
                enchantments -> enchantments.set(enchantmentEntry.get(), finalChosen.level)
        );
        return book;
    }

    private record EnchantPool(String namespace, String path, int maxLevel, int weight) { }
}
