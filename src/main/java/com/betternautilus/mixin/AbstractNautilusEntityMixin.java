package com.betternautilus.mixin;

import com.betternautilus.BetterNautilus;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import com.betternautilus.enchantment.ModEnchantmentEffects;
import com.betternautilus.enchantment.NautilusPowerEffect;
import com.betternautilus.enchantment.NautilusSwiftnessEffect;
import com.betternautilus.loot.DashTracker;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin for AbstractNautilusEntity.
 *
 * NOTE: initialize() is NOT declared on AbstractNautilusEntity — it lives on MobEntity.
 * Stat assignment on spawn is handled by NautilusSpawnMixin (targets MobEntity) which
 * checks instanceof AbstractNautilusEntity before acting. This mixin handles everything
 * else: dash scaling, Nautilus Power, Nautilus Swiftness, and dash collision damage.
 * Breeding stat inheritance lives in NautilusEntityMixin (createChild is on NautilusEntity).
 */
@Mixin(AbstractNautilusEntity.class)
public abstract class AbstractNautilusEntityMixin extends PassiveEntity {

    private static final Identifier SWIFTNESS_MODIFIER_ID =
            Identifier.of("betternautilus", "swiftness_enchant_boost");

    protected AbstractNautilusEntityMixin() {
        super(null, null);
    }

    // NOTE: Random stat assignment on spawn is handled by NautilusSpawnMixin
    // (which targets MobEntity and guards with instanceof AbstractNautilusEntity).
    // It is inlined there rather than delegated here, because cross-mixin casts
    // between different target classes are not supported by the mixin transformer.

    // =========================================================================
    // FEATURE 3: DASH — scale by DASH_STRENGTH attribute
    // =========================================================================

    @ModifyVariable(
            method = "dash(FLnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private float betternautilus$scaleDashByAttribute(float strength) {
        EntityAttributeInstance dashAttr = this.getAttributeInstance(ModAttributes.DASH_STRENGTH);
        if (dashAttr != null) return strength * (float) dashAttr.getValue();
        return strength;
    }

    // =========================================================================
    // FEATURE 2: NAUTILUS POWER (gated by enchantmentsEnabled)
    // =========================================================================

    @Inject(method = "dash(FLnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
    private void betternautilus$markDashStart(float strength, PlayerEntity controller, CallbackInfo ci) {
        DashTracker.markDashing((AbstractNautilusEntity) (Object) this);
    }

    @Inject(method = "dash(FLnet/minecraft/entity/player/PlayerEntity;)V", at = @At("TAIL"))
    private void betternautilus$applyNautilusPowerOnDash(float strength, PlayerEntity controller, CallbackInfo ci) {
        if (!BetterNautilusConfig.get().enchantmentsEnabled) return;

        float bonusDamage = betternautilus$getNautilusPowerBonus();
        if (bonusDamage <= 0) return;
        EntityAttributeInstance attackAttr = this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (attackAttr == null) return;
        Identifier modId = Identifier.of("betternautilus", "power_dash_bonus");
        attackAttr.removeModifier(modId);
        attackAttr.addTemporaryModifier(new EntityAttributeModifier(
                modId, bonusDamage, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void betternautilus$removePowerModifierWhenDashEnds(CallbackInfo ci) {
        AbstractNautilusEntity self = (AbstractNautilusEntity) (Object) this;
        if (self.getJumpCooldown() == 0) {
            EntityAttributeInstance attackAttr = this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            if (attackAttr != null)
                attackAttr.removeModifier(Identifier.of("betternautilus", "power_dash_bonus"));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void betternautilus$tickDashTracker(CallbackInfo ci) {
        DashTracker.tickEntity((AbstractNautilusEntity) (Object) this);
    }

    private float betternautilus$getNautilusPowerBonus() {
        ItemStack armor = this.getEquippedStack(EquipmentSlot.BODY);
        if (armor.isEmpty()) return 0;
        float total = 0;
        var enchantments = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(armor);
        for (var entry : enchantments.getEnchantments()) {
            int level = enchantments.getLevel(entry);
            List<EnchantmentEffectEntry<NautilusPowerEffect>> effects =
                    entry.value().getEffect(ModEnchantmentEffects.NAUTILUS_POWER);
            for (var effectEntry : effects) total += effectEntry.effect().getBonusDamage(level);
        }
        return total;
    }

    // =========================================================================
    // DASH COLLISION DAMAGE
    // =========================================================================

    @Inject(method = "tickControlled", at = @At("TAIL"))
    private void betternautilus$dashCollisionDamage(PlayerEntity controllingPlayer, Vec3d movementInput, CallbackInfo ci) {
        AbstractNautilusEntity self = (AbstractNautilusEntity) (Object) this;
        if (self.getEntityWorld().isClient()) return;
        if (!self.isDashing()) return;
        if (!(self.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        net.minecraft.util.math.Box hitBox = self.getBoundingBox().expand(0.3);
        self.getEntityWorld().getEntitiesByClass(LivingEntity.class, hitBox, candidate -> {
            if (candidate == self) return false;
            if (candidate == controllingPlayer) return false;
            if (candidate.isTeammate(self)) return false;
            return true;
        }).forEach(target -> self.tryAttack(serverWorld, target));
    }

    // =========================================================================
    // FEATURE 2: NAUTILUS SWIFTNESS (gated by enchantmentsEnabled)
    // =========================================================================

    @Inject(method = "tick", at = @At("TAIL"))
    private void betternautilus$applySwiftnessEnchant(CallbackInfo ci) {
        if (this.getEntityWorld().isClient()) return;
        EntityAttributeInstance speedAttr = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;
        speedAttr.removeModifier(SWIFTNESS_MODIFIER_ID);

        // Gate: if enchantments are disabled, just remove any existing modifier and stop
        if (!BetterNautilusConfig.get().enchantmentsEnabled) return;

        if (!(this.getFirstPassenger() instanceof PlayerEntity)) return;
        float swiftnessBonus = betternautilus$getSwiftnessBonus();
        if (swiftnessBonus <= 0) return;
        speedAttr.addTemporaryModifier(new EntityAttributeModifier(
                SWIFTNESS_MODIFIER_ID, swiftnessBonus, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private float betternautilus$getSwiftnessBonus() {
        ItemStack armor = this.getEquippedStack(EquipmentSlot.BODY);
        if (armor.isEmpty()) return 0;
        float total = 0;
        var enchantments = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(armor);
        for (var entry : enchantments.getEnchantments()) {
            int level = enchantments.getLevel(entry);
            List<EnchantmentEffectEntry<NautilusSwiftnessEffect>> effects =
                    entry.value().getEffect(ModEnchantmentEffects.NAUTILUS_SWIFTNESS);
            for (var effectEntry : effects) total += effectEntry.effect().getSpeedMultiplier(level);
        }
        return total;
    }
}
