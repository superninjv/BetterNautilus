package com.betternautilus.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Helper for {@link NautilusRecipeToggleMixin}.
 *
 * <p>Checks whether a crafting output ItemStack is one of the nautilus armor items
 * that our mod adds recipes for. Used to block crafting output when the recipes
 * feature is disabled in the config.
 */
public final class NautilusRecipeToggleHelper {

    private static final Set<Identifier> NAUTILUS_ARMOR_IDS = Set.of(
            Identifier.ofVanilla("copper_nautilus_armor"),
            Identifier.ofVanilla("iron_nautilus_armor"),
            Identifier.ofVanilla("golden_nautilus_armor"),
            Identifier.ofVanilla("diamond_nautilus_armor")
    );

    private NautilusRecipeToggleHelper() { }

    /**
     * Returns true if the given ItemStack is a nautilus armor item that our
     * mod adds a crafting recipe for.
     */
    public static boolean isBlockedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return NAUTILUS_ARMOR_IDS.contains(id);
    }
}
