package com.betternautilus.mixin;

import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks Better Nautilus crafting recipes when {@code recipesEnabled} is false.
 *
 * <p>Targets {@link CraftingScreenHandler#onContentChanged} which is called every
 * time the crafting grid changes. After vanilla updates the result slot with the
 * matched recipe output, we check if the output is one of our nautilus armor items.
 * If recipes are disabled, we clear the result slot — the player sees no output
 * and cannot craft the item.
 *
 * <p>This also effectively hides the recipe from the recipe book because the
 * recipe book UI checks whether a recipe can produce output. With an empty result,
 * the recipe book will not suggest or auto-fill these recipes.
 *
 * <p>The JSON recipe and advancement files remain in the jar; they are simply
 * invisible at runtime when disabled. Re-enabling requires only a config change
 * and /betternautilus reload (or server restart).
 */
@Mixin(CraftingScreenHandler.class)
public abstract class NautilusRecipeToggleMixin extends ScreenHandler {

    protected NautilusRecipeToggleMixin() {
        super(null, 0);
    }

    @Inject(
            method = "onContentChanged",
            at = @At("TAIL")
    )
    private void betternautilus$blockDisabledRecipeOutput(
            net.minecraft.inventory.Inventory inventory,
            CallbackInfo ci
    ) {
        if (BetterNautilusConfig.get().recipesEnabled) return;

        // Slot 0 in a CraftingScreenHandler is always the result slot
        ItemStack result = this.slots.get(0).getStack();
        if (result.isEmpty()) return;

        if (NautilusRecipeToggleHelper.isBlockedItem(result)) {
            this.slots.get(0).setStack(ItemStack.EMPTY);
        }
    }
}
