package dev.thirteenblade;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/** A shapeless upgrade that copies the complete source stack data instead of resetting the sword. */
public final class DragonUpgradeRecipe extends SpecialCraftingRecipe {
    public DragonUpgradeRecipe(Identifier id, CraftingRecipeCategory category) { super(id, category); }

    @Override public boolean matches(RecipeInputInventory input, World world) {
        int swords = 0, eggs = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);
            if (stack.isOf(ThirteenBlade.SWORD)) swords++;
            else if (stack.isOf(Items.DRAGON_EGG)) eggs++;
            else if (!stack.isEmpty()) return false;
        }
        return swords == 1 && eggs == 1;
    }

    @Override public ItemStack craft(RecipeInputInventory input, DynamicRegistryManager registries) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack source = input.getStack(i);
            if (!source.isOf(ThirteenBlade.SWORD)) continue;
            ItemStack result = new ItemStack(ThirteenBlade.DRAGON_SWORD);
            if (source.hasNbt()) result.setNbt(source.getNbt().copy());
            BladeEnchantments.ensure(result);
            return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean fits(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ThirteenBlade.DRAGON_UPGRADE; }
}
