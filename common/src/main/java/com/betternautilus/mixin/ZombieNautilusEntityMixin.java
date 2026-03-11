package com.betternautilus.mixin;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives ZombieNautilus randomised trait stats on spawn.
 *
 * NOTE: Class name MUST match filename (ZombieNautilusEntityMixin).
 */
@Mixin(ZombieNautilus.class)
public class ZombieNautilusEntityMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void betternautilus$initZombieStats(
            ServerLevelAccessor world,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        ZombieNautilus self = (ZombieNautilus) (Object) this;

        BetterNautilusCommon.LOGGER.info("[BetterNautilus] ZombieNautilusMixin fired for {}", self.getUUID());

        BetterNautilusConfig cfg = BetterNautilusConfig.get();
        var random = self.getRandom();

        double health = cfg.healthMin + random.nextDouble() * (cfg.spawnHealthMax - cfg.healthMin);
        AttributeInstance healthAttr = self.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) healthAttr.setBaseValue(health);
        self.setHealth(self.getMaxHealth());

        double speedValue = cfg.speedMin + random.nextDouble() * (cfg.spawnSpeedMax - cfg.speedMin);
        AttributeInstance speedAttr = self.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(speedValue);

        double dashStrength = cfg.dashStrengthMin + random.nextDouble() * (cfg.spawnDashStrengthMax - cfg.dashStrengthMin);
        AttributeInstance dashAttr = self.getAttribute(ModAttributes.DASH_STRENGTH);
        if (dashAttr != null) dashAttr.setBaseValue(dashStrength);

        double dashDamage = cfg.dashDamageMin + random.nextDouble() * (cfg.spawnDashDamageMax - cfg.dashDamageMin);
        AttributeInstance damageAttr = self.getAttribute(ModAttributes.DASH_ATTACK_DAMAGE);
        if (damageAttr != null) damageAttr.setBaseValue(dashDamage);
    }
}
