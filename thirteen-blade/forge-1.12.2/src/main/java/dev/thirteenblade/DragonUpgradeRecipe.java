package dev.thirteenblade;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/** Copy all sword NBT, including its identity, cooldown, custom name and enchantments. */
public final class DragonUpgradeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @Override public boolean matches(InventoryCrafting inventory, World world) {
        int swords = 0, eggs = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == ThirteenBlade.SWORD) swords++;
            else if (stack.getItem() == Item.getItemFromBlock(Blocks.DRAGON_EGG)) eggs++;
            else return false;
        }
        return swords == 1 && eggs == 1;
    }
    @Override public ItemStack getCraftingResult(InventoryCrafting inventory) {
        if (!matches(inventory, null)) return ItemStack.EMPTY;
        for (int i = 0; i < inventory.getSizeInventory(); i++) if (inventory.getStackInSlot(i).getItem() == ThirteenBlade.SWORD) {
            ItemStack result = new ItemStack(ThirteenBlade.DRAGON_SWORD);
            ItemStack source = inventory.getStackInSlot(i);
            if (source.hasTagCompound()) result.setTagCompound(source.getTagCompound().copy());
            BladeEnchantments.ensure(result); return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean canFit(int width, int height) { return width * height >= 2; }
    @Override public ItemStack getRecipeOutput() { return ThirteenBlade.sword(true); }
    @Override public boolean isDynamic() { return true; }
}
