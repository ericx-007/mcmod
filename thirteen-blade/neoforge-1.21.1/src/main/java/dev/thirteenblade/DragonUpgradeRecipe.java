package dev.thirteenblade;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.Level;

public final class DragonUpgradeRecipe extends CustomRecipe {
    public DragonUpgradeRecipe(CraftingBookCategory category) { super(category); }
    @Override public boolean matches(CraftingInput input, Level level) {
        int swords = 0, eggs = 0;
        for (ItemStack stack : input.items()) {
            if (stack.is(ThirteenBlade.SWORD.get())) swords++;
            else if (stack.is(Items.DRAGON_EGG)) eggs++;
            else if (!stack.isEmpty()) return false;
        }
        return swords == 1 && eggs == 1;
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        for (ItemStack source : input.items()) {
            if (!source.is(ThirteenBlade.SWORD.get())) continue;
            ItemStack result = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
            // Transfer only component changes: copying the base sword's defaults would replace
            // the advanced sword's stronger default attribute modifiers.
            result.applyComponents(source.getComponentsPatch());
            BladeEnchantments.ensure(result, registries);
            return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ThirteenBlade.DRAGON_UPGRADE.get(); }
}
