package com.betternautilus.mixin;

import com.betternautilus.BetterNautilus;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieNautilusEntity;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives ZombieNautilusEntity randomised trait stats on spawn.
 *
 * Targets ZombieNautilusEntity directly. If ZombieNautilusEntity does not
 * override initialize(), Mixin will still find it via the superclass hierarchy
 * since we target the concrete class (not MobEntity). This avoids the problem
 * of having TWO mixins on MobEntity both injecting into initialize().
 *
 * If the game complains that initialize() is not found on ZombieNautilusEntity,
 * change the @Mixin target back to MobEntity and use the instanceof guard.
 */
@Mixin(ZombieNautilusEntity.class)
public class ZombieNautilusEntityMixin {

    @Inject(method = "initialize", at = @At("RETURN"))
    private void betternautilus$initZombieStats(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            EntityData entityData,
            CallbackInfoReturnable<EntityData> cir
    ) {
        ZombieNautilusEntity self = (ZombieNautilusEntity) (Object) this;

        BetterNautilus.LOGGER.info("[BetterNautilus] ZombieNautilusEntityMixin fired for {}", self.getUuid());

        BetterNautilusConfig cfg = BetterNautilusConfig.get();
        var random = self.getRandom();

        double health = cfg.healthMin + random.nextDouble() * (cfg.spawnHealthMax - cfg.healthMin);
        EntityAttributeInstance healthAttr = self.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttr != null) healthAttr.setBaseValue(health);
        self.setHealth(self.getMaxHealth());

        double speedValue = cfg.speedMin + random.nextDouble() * (cfg.spawnSpeedMax - cfg.speedMin);
        EntityAttributeInstance speedAttr = self.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(speedValue);

        double dashStrength = cfg.dashStrengthMin + random.nextDouble() * (cfg.spawnDashStrengthMax - cfg.dashStrengthMin);
        EntityAttributeInstance dashAttr = self.getAttributeInstance(ModAttributes.DASH_STRENGTH);
        if (dashAttr != null) dashAttr.setBaseValue(dashStrength);

        double dashDamage = cfg.dashDamageMin + random.nextDouble() * (cfg.spawnDashDamageMax - cfg.dashDamageMin);
        EntityAttributeInstance damageAttr = self.getAttributeInstance(ModAttributes.DASH_ATTACK_DAMAGE);
        if (damageAttr != null) damageAttr.setBaseValue(dashDamage);
    }
}
