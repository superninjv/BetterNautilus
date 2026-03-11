package com.betternautilus.mixin;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into Mob.finalizeSpawn() to assign random stats to nautiluses on spawn.
 *
 * AbstractNautilus does NOT override finalizeSpawn(), so we must target Mob
 * and guard with instanceof.
 */
@Mixin(Mob.class)
public class NautilusSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void betternautilus$initNautilusStats(
            ServerLevelAccessor world,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!((Object) this instanceof AbstractNautilus nautilus)) return;
        if ((Object) this instanceof ZombieNautilus) return;

        if (spawnReason == EntitySpawnReason.BREEDING) {
            BetterNautilusCommon.LOGGER.info("[BetterNautilus] Skipping spawn stat assignment for bred nautilus {}", nautilus.getUUID());
            return;
        }

        BetterNautilusCommon.LOGGER.info("[BetterNautilus] NautilusSpawnMixin fired for {}", nautilus.getUUID());

        RandomSource random = nautilus.getRandom();
        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        // --- Health ---
        double health = cfg.healthMin + random.nextDouble() * (cfg.spawnHealthMax - cfg.healthMin);
        AttributeInstance healthAttr = nautilus.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(health);
            BetterNautilusCommon.LOGGER.info("[BetterNautilus] {} health -> {} (readback {})",
                    nautilus.getUUID(), health, healthAttr.getBaseValue());
        } else {
            BetterNautilusCommon.LOGGER.error("[BetterNautilus] {} MAX_HEALTH attr is NULL", nautilus.getUUID());
        }
        nautilus.setHealth(nautilus.getMaxHealth());

        // --- Swim Speed ---
        double speedValue = cfg.speedMin + random.nextDouble() * (cfg.spawnSpeedMax - cfg.speedMin);
        AttributeInstance speedAttr = nautilus.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedValue);
            BetterNautilusCommon.LOGGER.info("[BetterNautilus] {} speed -> {} (readback {})",
                    nautilus.getUUID(), speedValue, speedAttr.getBaseValue());
        } else {
            BetterNautilusCommon.LOGGER.error("[BetterNautilus] {} MOVEMENT_SPEED attr is NULL", nautilus.getUUID());
        }

        // --- Dash Strength ---
        double dashStrength = cfg.dashStrengthMin + random.nextDouble() * (cfg.spawnDashStrengthMax - cfg.dashStrengthMin);
        AttributeInstance dashAttr = nautilus.getAttribute(ModAttributes.DASH_STRENGTH);
        if (dashAttr != null) {
            dashAttr.setBaseValue(dashStrength);
            BetterNautilusCommon.LOGGER.info("[BetterNautilus] {} dash_strength -> {} (readback {})",
                    nautilus.getUUID(), dashStrength, dashAttr.getBaseValue());
        } else {
            BetterNautilusCommon.LOGGER.error("[BetterNautilus] {} DASH_STRENGTH attr is NULL", nautilus.getUUID());
        }

        // --- Dash Attack Damage ---
        double dashDamage = cfg.dashDamageMin + random.nextDouble() * (cfg.spawnDashDamageMax - cfg.dashDamageMin);
        AttributeInstance damageAttr = nautilus.getAttribute(ModAttributes.DASH_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(dashDamage);
            BetterNautilusCommon.LOGGER.info("[BetterNautilus] {} dash_attack_damage -> {} (readback {})",
                    nautilus.getUUID(), dashDamage, damageAttr.getBaseValue());
        } else {
            BetterNautilusCommon.LOGGER.error("[BetterNautilus] {} DASH_ATTACK_DAMAGE attr is NULL", nautilus.getUUID());
        }
    }
}
