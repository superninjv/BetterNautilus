package com.betternautilus.mixin;

import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.entity.passive.NautilusEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for NautilusEntity (the concrete tame variant).
 *
 * createChild is declared here (not on AbstractNautilusEntity), so breeding
 * stat inheritance must be injected here.
 */
@Mixin(NautilusEntity.class)
public class NautilusEntityMixin {

    @Inject(method = "createChild", at = @At("RETURN"))
    private void betternautilus$inheritChildStats(
            ServerWorld serverWorld,
            PassiveEntity passiveEntity,
            CallbackInfoReturnable<NautilusEntity> cir
    ) {
        NautilusEntity child = cir.getReturnValue();
        if (child == null) return;

        AbstractNautilusEntity self = (AbstractNautilusEntity) (Object) this;
        AbstractNautilusEntity other = (AbstractNautilusEntity) passiveEntity;
        Random random = serverWorld.getRandom();

        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        betternautilus$setChildAttribute(child, self, other, EntityAttributes.MAX_HEALTH,
                cfg.healthMin, cfg.healthMax, random);
        child.setHealth(child.getMaxHealth());

        betternautilus$setChildAttribute(child, self, other, EntityAttributes.MOVEMENT_SPEED,
                cfg.speedMin, cfg.speedMax, random);
        betternautilus$setChildAttribute(child, self, other, ModAttributes.DASH_STRENGTH,
                cfg.dashStrengthMin, cfg.dashStrengthMax, random);
        betternautilus$setChildAttribute(child, self, other, ModAttributes.DASH_ATTACK_DAMAGE,
                cfg.dashDamageMin, cfg.dashDamageMax, random);
    }

    /**
     * Mutation-based breeding formula:
     *   child = avg(parentA, parentB) + uniform(-spread, +spread)
     *
     * Where spread = (max - min) * breedingMutationRange (from config).
     *
     * This formula centres offspring on the parent average with configurable
     * variance. Unlike the old (A + B + random) / 3 formula, it does not
     * regress toward the range midpoint — two high-stat parents reliably
     * produce high-stat offspring, with the mutation range controlling how
     * much lucky/unlucky variance each generation can add.
     *
     * Default breedingMutationRange = 0.25 means:
     *   - All 5-star reachable in ~10-15 generations of selective breeding
     *   - Two bad parents can't randomly produce a god-tier child
     *   - Some regression is possible (keeps breeding meaningful)
     *   - Server ops can tune: lower = slower/harder, higher = faster/easier
     */
    private static void betternautilus$setChildAttribute(
            AbstractNautilusEntity child,
            AbstractNautilusEntity parentA,
            AbstractNautilusEntity parentB,
            RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
            double min, double max,
            Random random
    ) {
        double a = MathHelper.clamp(parentA.getAttributeBaseValue(attribute), min, max);
        double b = MathHelper.clamp(parentB.getAttributeBaseValue(attribute), min, max);
        double avg = (a + b) / 2.0;

        double mutationRange = BetterNautilusConfig.get().breedingMutationRange;
        double spread = (max - min) * mutationRange;
        // uniform random in [-spread, +spread]
        double mutation = (random.nextDouble() - 0.5) * 2.0 * spread;
        double result = MathHelper.clamp(avg + mutation, min, max);

        EntityAttributeInstance instance = child.getAttributeInstance(attribute);
        if (instance != null) instance.setBaseValue(result);
    }
}
