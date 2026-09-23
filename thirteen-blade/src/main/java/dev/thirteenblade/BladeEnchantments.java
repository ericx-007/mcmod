package dev.thirteenblade;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;

/** Intrinsic minimum enchantments; preserve every other enchantment and stronger existing levels. */
public final class BladeEnchantments {
    private BladeEnchantments() {}

    public static void ensure(ItemStack stack) {
        if (!ThirteenBlade.isSword(stack)) return;
        if (EnchantmentHelper.getLevel(Enchantments.LOOTING, stack) >= 3
                && EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, stack) >= 1) return;
        var enchantments = EnchantmentHelper.get(stack);
        enchantments.merge(Enchantments.LOOTING, 3, Math::max);
        enchantments.merge(Enchantments.BINDING_CURSE, 1, Math::max);
        EnchantmentHelper.set(enchantments, stack);
    }
}
