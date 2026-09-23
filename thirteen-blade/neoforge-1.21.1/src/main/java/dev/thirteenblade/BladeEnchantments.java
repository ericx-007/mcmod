package dev.thirteenblade;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Resolve data-driven enchantments from the current world, never from a cached registry. */
public final class BladeEnchantments {
    private BladeEnchantments() {}

    public static void ensure(ItemStack stack, HolderLookup.Provider registries) {
        if (!ThirteenBlade.isSword(stack)) return;
        var registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        var looting = registry.getOrThrow(Enchantments.LOOTING);
        var binding = registry.getOrThrow(Enchantments.BINDING_CURSE);
        if (EnchantmentHelper.getTagEnchantmentLevel(looting, stack) >= 3
                && EnchantmentHelper.getTagEnchantmentLevel(binding, stack) >= 1) return;
        EnchantmentHelper.updateEnchantments(stack, enchantments -> {
            enchantments.upgrade(looting, 3);
            enchantments.upgrade(binding, 1);
        });
    }
}
