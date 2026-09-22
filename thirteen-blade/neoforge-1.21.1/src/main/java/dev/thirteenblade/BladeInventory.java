package dev.thirteenblade;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One active sword for combat; the strongest carried sword supplies vitality. */
public final class BladeInventory {
    private BladeInventory() {}

    public static ItemStack activeSword(Player player) {
        if (ThirteenBlade.isSword(player.getMainHandItem())) return player.getMainHandItem();
        if (ThirteenBlade.isSword(player.getOffhandItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static double healthBonus(Player player) {
        double best = 0;
        for (ItemStack stack : player.getInventory().items)
            if (ThirteenBlade.isSword(stack)) best = Math.max(best, BladeData.healthBonus(stack));
        for (ItemStack stack : player.getInventory().offhand)
            if (ThirteenBlade.isSword(stack)) best = Math.max(best, BladeData.healthBonus(stack));
        return best;
    }
}
