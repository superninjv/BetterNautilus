package com.betternautilus.mixin;

import com.betternautilus.BetterNautilus;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieNautilusEntity;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into MobEntity.initialize() to assign random stats to nautiluses on spawn.
 *
 * AbstractNautilusEntity does NOT override initialize(), so we must target MobEntity
 * and guard with instanceof. Stat assignment is inlined here rather than delegating
 * to AbstractNautilusEntityMixin, because cross-mixin casts between different target
 * classes are not supported by the mixin transformer.
 */
@Mixin(MobEntity.class)
public class NautilusSpawnMixin {

    @Inject(method = "initialize", at = @At("RETURN"))
    private void betternautilus$initNautilusStats(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            EntityData entityData,
            CallbackInfoReturnable<EntityData> cir
    ) {
        if (!((Object) this instanceof AbstractNautilusEntity nautilus)) return;
        // ZombieNautilusEntity extends AbstractNautilusEntity, so exclude it here —
        // it has its own mixin (ZombieNautilusEntityMixin) for stat assignment.
        if ((Object) this instanceof ZombieNautilusEntity) return;

        // CRITICAL: Do NOT overwrite stats on bred babies. Breeding stat inheritance
        // is handled by NautilusEntityMixin (createChild), which runs BEFORE initialize().
        // Without this guard, every bred baby's carefully-calculated inherited stats get
        // replaced with spawn-capped random values, making breeding progress impossible.
        if (spawnReason == SpawnReason.BREEDING) {
            BetterNautilus.LOGGER.info("[BetterNautilus] Skipping spawn stat assignment for bred nautilus {}", nautilus.getUuid());
            return;
        }

        BetterNautilus.LOGGER.info("[BetterNautilus] NautilusSpawnMixin fired for {}", nautilus.getUuid());

        Random random = nautilus.getRandom();
        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        // --- Health ---
        double health = cfg.healthMin + random.nextDouble() * (cfg.spawnHealthMax - cfg.healthMin);
        EntityAttributeInstance healthAttr = nautilus.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(health);
            BetterNautilus.LOGGER.info("[BetterNautilus] {} health -> {} (readback {})",
                    nautilus.getUuid(), health, healthAttr.getBaseValue());
        } else {
            BetterNautilus.LOGGER.error("[BetterNautilus] {} MAX_HEALTH attr is NULL", nautilus.getUuid());
        }
        nautilus.setHealth(nautilus.getMaxHealth());

        // --- Swim Speed ---
        double speedValue = cfg.speedMin + random.nextDouble() * (cfg.spawnSpeedMax - cfg.speedMin);
        EntityAttributeInstance speedAttr = nautilus.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedValue);
            BetterNautilus.LOGGER.info("[BetterNautilus] {} speed -> {} (readback {})",
                    nautilus.getUuid(), speedValue, speedAttr.getBaseValue());
        } else {
            BetterNautilus.LOGGER.error("[BetterNautilus] {} MOVEMENT_SPEED attr is NULL", nautilus.getUuid());
        }

        // --- Dash Strength ---
        double dashStrength = cfg.dashStrengthMin + random.nextDouble() * (cfg.spawnDashStrengthMax - cfg.dashStrengthMin);
        EntityAttributeInstance dashAttr = nautilus.getAttributeInstance(ModAttributes.DASH_STRENGTH);
        if (dashAttr != null) {
            dashAttr.setBaseValue(dashStrength);
            BetterNautilus.LOGGER.info("[BetterNautilus] {} dash_strength -> {} (readback {})",
                    nautilus.getUuid(), dashStrength, dashAttr.getBaseValue());
        } else {
            BetterNautilus.LOGGER.error("[BetterNautilus] {} DASH_STRENGTH attr is NULL", nautilus.getUuid());
        }

        // --- Dash Attack Damage ---
        double dashDamage = cfg.dashDamageMin + random.nextDouble() * (cfg.spawnDashDamageMax - cfg.dashDamageMin);
        EntityAttributeInstance damageAttr = nautilus.getAttributeInstance(ModAttributes.DASH_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(dashDamage);
            BetterNautilus.LOGGER.info("[BetterNautilus] {} dash_attack_damage -> {} (readback {})",
                    nautilus.getUuid(), dashDamage, damageAttr.getBaseValue());
        } else {
            BetterNautilus.LOGGER.error("[BetterNautilus] {} DASH_ATTACK_DAMAGE attr is NULL", nautilus.getUuid());
        }
    }
}
