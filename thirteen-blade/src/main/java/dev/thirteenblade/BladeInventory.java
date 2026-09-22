package dev.thirteenblade;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** One active sword for combat; the strongest carried sword supplies vitality. */
public final class BladeInventory {
    private BladeInventory() {}

    public static ItemStack activeSword(PlayerEntity player) {
        if (ThirteenBlade.isSword(player.getMainHandStack())) return player.getMainHandStack();
        if (ThirteenBlade.isSword(player.getOffHandStack())) return player.getOffHandStack();
        return ItemStack.EMPTY;
    }

    public static double healthBonus(PlayerEntity player) {
        double best = 0;
        for (ItemStack stack : player.getInventory().main)
            if (ThirteenBlade.isSword(stack)) best = Math.max(best, BladeData.healthBonus(stack));
        for (ItemStack stack : player.getInventory().offHand)
            if (ThirteenBlade.isSword(stack)) best = Math.max(best, BladeData.healthBonus(stack));
        return best;
    }
}
