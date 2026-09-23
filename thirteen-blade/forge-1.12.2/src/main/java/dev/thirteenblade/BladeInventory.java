package dev.thirteenblade;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class BladeInventory {
    private BladeInventory() {}
    public static ItemStack activeSword(EntityPlayer player) {
        if (ThirteenBlade.isSword(player.getHeldItemMainhand())) return player.getHeldItemMainhand();
        return ThirteenBlade.isSword(player.getHeldItemOffhand()) ? player.getHeldItemOffhand() : ItemStack.EMPTY;
    }
    public static double healthBonus(EntityPlayer player) {
        double best = 0;
        for (ItemStack stack : player.inventory.mainInventory) if (ThirteenBlade.isSword(stack)) best = Math.max(best, BladeData.healthBonus(stack));
        for (ItemStack stack : player.inventory.offHandInventory) if (ThirteenBlade.isSword(stack)) best = Math.max(best, BladeData.healthBonus(stack));
        return best;
    }
}
