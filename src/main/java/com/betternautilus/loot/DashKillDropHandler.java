package com.betternautilus.loot;

import com.betternautilus.BetterNautilus;
import com.betternautilus.config.BetterNautilusConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Listens for mob death events. When a mob dies from an attack whose source is a
 * dashing nautilus, there is a chance to drop an enchanted book containing one
 * of the three Better Nautilus enchantments.
 *
 * Drop rates (per kill while dashing):
 *   - 20% chance a book drops at all
 *   - If a book drops, the enchantment and level are chosen randomly:
 *       Nautilus Protection I–IV  (weight 5 each level)
 *       Nautilus Power      I–III  (weight 4 each level)
 *       Nautilus Swiftness  I–III  (weight 4 each level)
 *
 * Drops are disabled when either {@code enchantmentsEnabled} or
 * {@code enchantedBookDropEnabled} is false in the config.
 *
 * Registration is called from BetterNautilus#onInitialize (server-side event, safe
 * to register in the main entrypoint).
 */
public class DashKillDropHandler {

    /** Enchantment ID → max level pairs. Order determines weighted selection below. */
    private static final List<EnchantPool> POOL = List.of(
            new EnchantPool("betternautilus:nautilus_protection", 4, 5),
            new EnchantPool("betternautilus:nautilus_power",      3, 4),
            new EnchantPool("betternautilus:nautilus_swiftness",  3, 4)
    );

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(DashKillDropHandler::onMobDeath);
        BetterNautilus.LOGGER.info("Registered dash-kill enchanted book drop handler.");
    }

    private static void onMobDeath(LivingEntity victim, DamageSource source) {
        // We only care about server-side deaths
        if (!(victim.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        // The attacker must be a dashing nautilus
        if (!(source.getAttacker() instanceof AbstractNautilusEntity nautilus)) return;
        if (!DashTracker.isDashing(nautilus)) return;

        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        // Gate: enchantments feature must be enabled for book drops
        if (!cfg.enchantmentsEnabled) return;

        if (!cfg.enchantedBookDropEnabled) return;

        Random random = serverWorld.getRandom();
        if (random.nextFloat() > cfg.enchantedBookDropChance) return;

        ItemStack book = createEnchantedBook(serverWorld, random);
        if (book == null) return;

        // Drop at the victim's position
        victim.dropStack(serverWorld, book);
    }

    private static ItemStack createEnchantedBook(ServerWorld world, Random random) {
        // Build a weighted list of (enchantment ID, level) options
        // Weight per entry = pool weight; each level is an independent entry
        record Option(String id, int level, int weight) { }
        List<Option> options = new java.util.ArrayList<>();
        for (EnchantPool pool : POOL) {
            for (int level = 1; level <= pool.maxLevel; level++) {
                options.add(new Option(pool.id, level, pool.weight));
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

        // Look up the enchantment in the registry
        var registryOptional = world.getRegistryManager()
                .getOptional(RegistryKeys.ENCHANTMENT);
        if (registryOptional.isEmpty()) return null;
        var enchantmentRegistry = registryOptional.get();

        var enchantmentEntry = enchantmentRegistry.getEntry(Identifier.of(chosen.id));
        if (enchantmentEntry.isEmpty()) {
            BetterNautilus.LOGGER.warn("Could not find enchantment {} for book drop", chosen.id);
            return null;
        }

        final Option finalChosen = chosen;
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        net.minecraft.enchantment.EnchantmentHelper.apply(
                book,
                enchantments -> enchantments.add(enchantmentEntry.get(), finalChosen.level)
        );
        return book;
    }

    private record EnchantPool(String id, int maxLevel, int weight) { }
}
