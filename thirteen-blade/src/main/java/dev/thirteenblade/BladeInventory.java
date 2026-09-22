package dev.thirteenblade;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** One active sword for combat; the strongest carried sword supplies vitality. */
public final class BladeInventory {
    private BladeInventory() {}

    public static ItemStack activeSword(PlayerEntity player) {
        if (player.getMainHandStack().isOf(ThirteenBlade.SWORD)) return player.getMainHandStack();
        if (player.getOffHandStack().isOf(ThirteenBlade.SWORD)) return player.getOffHandStack();
        return ItemStack.EMPTY;
    }

    public static double healthBonus(PlayerEntity player) {
        double best = 0;
        for (ItemStack stack : player.getInventory().main)
            if (stack.isOf(ThirteenBlade.SWORD)) best = Math.max(best, BladeData.healthBonus(stack));
        for (ItemStack stack : player.getInventory().offHand)
            if (stack.isOf(ThirteenBlade.SWORD)) best = Math.max(best, BladeData.healthBonus(stack));
        return best;
    }
}
