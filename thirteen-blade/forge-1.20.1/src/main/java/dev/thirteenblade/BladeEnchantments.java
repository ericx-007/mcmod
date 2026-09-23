package dev.thirteenblade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class BladeEnchantments {
    private BladeEnchantments() {}
    public static void ensure(ItemStack stack) {
        if (!ThirteenBlade.isSword(stack)) return;
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, stack) >= 3
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BINDING_CURSE, stack) >= 1) return;
        var values = EnchantmentHelper.getEnchantments(stack);
        values.merge(Enchantments.MOB_LOOTING, 3, Math::max);
        values.merge(Enchantments.BINDING_CURSE, 1, Math::max);
        EnchantmentHelper.setEnchantments(values, stack);
    }
}
