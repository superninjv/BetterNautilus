package com.betternautilus.mixin;

import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Allows nautilus armor to be enchanted via the enchanting table and anvil in Survival.
 */
@Mixin(ItemStack.class)
public class NautilusArmorEnchantMixin {

    private static final Set<Identifier> NAUTILUS_ARMOR_IDS = Set.of(
            Identifier.withDefaultNamespace("copper_nautilus_armor"),
            Identifier.withDefaultNamespace("iron_nautilus_armor"),
            Identifier.withDefaultNamespace("golden_nautilus_armor"),
            Identifier.withDefaultNamespace("diamond_nautilus_armor"),
            Identifier.withDefaultNamespace("netherite_nautilus_armor")
    );

    @Inject(
            method = "isEnchantable",
            at = @At("RETURN"),
            cancellable = true
    )
    private void betternautilus$allowNautilusArmorEnchanting(CallbackInfoReturnable<Boolean> cir) {
        if (!BetterNautilusConfig.get().enchantmentsEnabled) return;

        if (!cir.getReturnValue()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem());
            if (NAUTILUS_ARMOR_IDS.contains(id)) {
                cir.setReturnValue(true);
            }
        }
    }
}
