package com.betternautilus.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Helper for {@link NautilusRecipeToggleMixin}.
 *
 * Checks whether a crafting output ItemStack is one of the nautilus armor items
 * that our mod adds recipes for.
 */
public final class NautilusRecipeToggleHelper {

    private static final Set<Identifier> NAUTILUS_ARMOR_IDS = Set.of(
            Identifier.withDefaultNamespace("copper_nautilus_armor"),
            Identifier.withDefaultNamespace("iron_nautilus_armor"),
            Identifier.withDefaultNamespace("golden_nautilus_armor"),
            Identifier.withDefaultNamespace("diamond_nautilus_armor")
    );

    private NautilusRecipeToggleHelper() { }

    public static boolean isBlockedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return NAUTILUS_ARMOR_IDS.contains(id);
    }
}
