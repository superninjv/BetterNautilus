package com.betternautilus.mixin;

import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for Nautilus (the concrete tame variant).
 *
 * createChild/getBreedOffspring is declared here (not on AbstractNautilus),
 * so breeding stat inheritance must be injected here.
 */
@Mixin(Nautilus.class)
public class NautilusEntityMixin {

    @Inject(method = "getBreedOffspring", at = @At("RETURN"))
    private void betternautilus$inheritChildStats(
            ServerLevel serverWorld,
            AgeableMob passiveEntity,
            CallbackInfoReturnable<Nautilus> cir
    ) {
        Nautilus child = cir.getReturnValue();
        if (child == null) return;

        AbstractNautilus self = (AbstractNautilus) (Object) this;
        AbstractNautilus other = (AbstractNautilus) passiveEntity;
        RandomSource random = serverWorld.getRandom();

        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        betternautilus$setChildAttribute(child, self, other, Attributes.MAX_HEALTH,
                cfg.healthMin, cfg.healthMax, random);
        child.setHealth(child.getMaxHealth());

        betternautilus$setChildAttribute(child, self, other, Attributes.MOVEMENT_SPEED,
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
     */
    private static void betternautilus$setChildAttribute(
            AbstractNautilus child,
            AbstractNautilus parentA,
            AbstractNautilus parentB,
            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double min, double max,
            RandomSource random
    ) {
        double a = Mth.clamp(parentA.getAttributeBaseValue(attribute), min, max);
        double b = Mth.clamp(parentB.getAttributeBaseValue(attribute), min, max);
        double avg = (a + b) / 2.0;

        double mutationRange = BetterNautilusConfig.get().breedingMutationRange;
        double spread = (max - min) * mutationRange;
        double mutation = (random.nextDouble() - 0.5) * 2.0 * spread;
        double result = Mth.clamp(avg + mutation, min, max);

        AttributeInstance instance = child.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(result);
    }
}
