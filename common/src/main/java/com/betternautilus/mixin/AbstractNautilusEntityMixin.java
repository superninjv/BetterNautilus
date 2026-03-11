package com.betternautilus.mixin;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import com.betternautilus.enchantment.ModEnchantmentEffects;
import com.betternautilus.enchantment.NautilusPowerEffect;
import com.betternautilus.enchantment.NautilusSwiftnessEffect;
import com.betternautilus.loot.DashTracker;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin for AbstractNautilus.
 *
 * Handles: dash scaling, Nautilus Power, Nautilus Swiftness, dash collision damage,
 * and DashTracker ticking.
 *
 * Stat assignment on spawn is in NautilusSpawnMixin (targets Mob).
 * Breeding stat inheritance is in NautilusEntityMixin (targets Nautilus).
 */
@Mixin(AbstractNautilus.class)
public abstract class AbstractNautilusEntityMixin extends AgeableMob {

    private static final Identifier SWIFTNESS_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("betternautilus", "swiftness_enchant_boost");

    protected AbstractNautilusEntityMixin() {
        super(null, null);
    }

    // =========================================================================
    // FEATURE 3: DASH — scale by DASH_STRENGTH attribute
    // =========================================================================

    @ModifyVariable(
            method = "executeRidersJump",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private float betternautilus$scaleDashByAttribute(float strength) {
        AttributeInstance dashAttr = this.getAttribute(ModAttributes.DASH_STRENGTH);
        if (dashAttr != null) return strength * (float) dashAttr.getValue();
        return strength;
    }

    // =========================================================================
    // FEATURE 2: NAUTILUS POWER (gated by enchantmentsEnabled)
    // =========================================================================

    @Inject(method = "executeRidersJump", at = @At("HEAD"))
    private void betternautilus$markDashStart(float strength, Player controller, CallbackInfo ci) {
        DashTracker.markDashing((AbstractNautilus) (Object) this);
    }

    @Inject(method = "executeRidersJump", at = @At("TAIL"))
    private void betternautilus$applyNautilusPowerOnDash(float strength, Player controller, CallbackInfo ci) {
        if (!BetterNautilusConfig.get().enchantmentsEnabled) return;

        float bonusDamage = betternautilus$getNautilusPowerBonus();
        if (bonusDamage <= 0) return;
        AttributeInstance attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr == null) return;
        Identifier modId = Identifier.fromNamespaceAndPath("betternautilus", "power_dash_bonus");
        attackAttr.removeModifier(modId);
        attackAttr.addTransientModifier(new AttributeModifier(
                modId, bonusDamage, AttributeModifier.Operation.ADD_VALUE));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void betternautilus$removePowerModifierWhenDashEnds(CallbackInfo ci) {
        AbstractNautilus self = (AbstractNautilus) (Object) this;
        if (self.getJumpCooldown() == 0) {
            AttributeInstance attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null)
                attackAttr.removeModifier(Identifier.fromNamespaceAndPath("betternautilus", "power_dash_bonus"));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void betternautilus$tickDashTracker(CallbackInfo ci) {
        DashTracker.tickEntity((AbstractNautilus) (Object) this);
    }

    private float betternautilus$getNautilusPowerBonus() {
        ItemStack armor = this.getItemBySlot(EquipmentSlot.BODY);
        if (armor.isEmpty()) return 0;
        float total = 0;
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(armor);
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            List<ConditionalEffect<NautilusPowerEffect>> effects =
                    holder.value().getEffects(ModEnchantmentEffects.NAUTILUS_POWER);
            for (var effectEntry : effects) total += effectEntry.effect().getBonusDamage(level);
        }
        return total;
    }

    // =========================================================================
    // DASH COLLISION DAMAGE
    // =========================================================================

    @Inject(method = "tickRidden", at = @At("TAIL"))
    private void betternautilus$dashCollisionDamage(Player controllingPlayer, Vec3 movementInput, CallbackInfo ci) {
        AbstractNautilus self = (AbstractNautilus) (Object) this;
        if (self.level().isClientSide()) return;
        if (!self.isDashing()) return;
        if (!(self.level() instanceof ServerLevel serverWorld)) return;

        net.minecraft.world.phys.AABB hitBox = self.getBoundingBox().inflate(0.3);
        self.level().getEntitiesOfClass(LivingEntity.class, hitBox, candidate -> {
            if (candidate == self) return false;
            if (candidate == controllingPlayer) return false;
            if (candidate.isAlliedTo(self)) return false;
            return true;
        }).forEach(target -> self.doHurtTarget(serverWorld, target));
    }

    // =========================================================================
    // FEATURE 2: NAUTILUS SWIFTNESS (gated by enchantmentsEnabled)
    // =========================================================================

    @Inject(method = "tick", at = @At("TAIL"))
    private void betternautilus$applySwiftnessEnchant(CallbackInfo ci) {
        if (this.level().isClientSide()) return;
        AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;
        speedAttr.removeModifier(SWIFTNESS_MODIFIER_ID);

        if (!BetterNautilusConfig.get().enchantmentsEnabled) return;

        if (!(this.getFirstPassenger() instanceof Player)) return;
        float swiftnessBonus = betternautilus$getSwiftnessBonus();
        if (swiftnessBonus <= 0) return;
        speedAttr.addTransientModifier(new AttributeModifier(
                SWIFTNESS_MODIFIER_ID, swiftnessBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private float betternautilus$getSwiftnessBonus() {
        ItemStack armor = this.getItemBySlot(EquipmentSlot.BODY);
        if (armor.isEmpty()) return 0;
        float total = 0;
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(armor);
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            List<ConditionalEffect<NautilusSwiftnessEffect>> effects =
                    holder.value().getEffects(ModEnchantmentEffects.NAUTILUS_SWIFTNESS);
            for (var effectEntry : effects) total += effectEntry.effect().getSpeedMultiplier(level);
        }
        return total;
    }
}
