package com.betternautilus.mixin;

import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Allows nautilus armor to be enchanted via the enchanting table and anvil in Survival.
 *
 * <p>EnchantmentScreenHandler#onContentChanged gates enchanting on itemStack.isEnchantable().
 * Nautilus armor has no EnchantmentValue component so isEnchantable() returns false.
 * We inject into ItemStack#isEnchantable and return true for nautilus armor items,
 * which makes the enchanting table show options and the anvil accept enchanted books.
 *
 * <p>When {@code enchantmentsEnabled} is false in the config, this mixin does nothing,
 * restoring vanilla behavior where nautilus armor cannot be enchanted in Survival.
 * Any existing enchantments on armor items remain but their effects are inert
 * (gated in AbstractNautilusEntityMixin).
 *
 * <p>Items are identified by registry ID rather than Items.FIELD references because
 * the nautilus armor items are dynamically registered and have no static fields on Items.
 */
@Mixin(ItemStack.class)
public class NautilusArmorEnchantMixin {

    private static final Set<Identifier> NAUTILUS_ARMOR_IDS = Set.of(
            Identifier.ofVanilla("copper_nautilus_armor"),
            Identifier.ofVanilla("iron_nautilus_armor"),
            Identifier.ofVanilla("golden_nautilus_armor"),
            Identifier.ofVanilla("diamond_nautilus_armor"),
            Identifier.ofVanilla("netherite_nautilus_armor")
    );

    @Inject(
            method = "isEnchantable",
            at = @At("RETURN"),
            cancellable = true
    )
    private void betternautilus$allowNautilusArmorEnchanting(CallbackInfoReturnable<Boolean> cir) {
        if (!BetterNautilusConfig.get().enchantmentsEnabled) return;

        if (!cir.getReturnValue()) {
            Identifier id = Registries.ITEM.getId(((ItemStack) (Object) this).getItem());
            if (NAUTILUS_ARMOR_IDS.contains(id)) {
                cir.setReturnValue(true);
            }
        }
    }
}
