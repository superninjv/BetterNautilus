package com.betternautilus.mixin;

import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks Better Nautilus crafting recipes when {@code recipesEnabled} is false.
 *
 * Targets CraftingMenu.slotsChanged which is called every time the crafting
 * grid changes. After vanilla updates the result slot, we check if the output
 * is one of our nautilus armor items. If recipes are disabled, we clear the
 * result slot.
 */
@Mixin(CraftingMenu.class)
public abstract class NautilusRecipeToggleMixin extends AbstractContainerMenu {

    protected NautilusRecipeToggleMixin() {
        super(null, 0);
    }

    @Inject(
            method = "slotsChanged",
            at = @At("TAIL")
    )
    private void betternautilus$blockDisabledRecipeOutput(
            net.minecraft.world.Container inventory,
            CallbackInfo ci
    ) {
        if (BetterNautilusConfig.get().recipesEnabled) return;

        // Slot 0 in a CraftingMenu is always the result slot
        ItemStack result = this.slots.get(0).getItem();
        if (result.isEmpty()) return;

        if (NautilusRecipeToggleHelper.isBlockedItem(result)) {
            this.slots.get(0).set(ItemStack.EMPTY);
        }
    }
}
