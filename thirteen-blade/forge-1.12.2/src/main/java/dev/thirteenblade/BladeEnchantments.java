package dev.thirteenblade;

import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;

public final class BladeEnchantments {
    private BladeEnchantments() {}
    public static void ensure(ItemStack stack) {
        if (!ThirteenBlade.isSword(stack)) return;
        BladeData.ensureIdentity(stack);
        stack.getTagCompound().setBoolean("Unbreakable", true);
        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, stack) >= 3 && EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack) >= 1) return;
        Map<Enchantment,Integer> values = EnchantmentHelper.getEnchantments(stack);
        values.merge(Enchantments.LOOTING, 3, Math::max); values.merge(Enchantments.BINDING_CURSE, 1, Math::max);
        EnchantmentHelper.setEnchantments(values, stack);
    }
}
