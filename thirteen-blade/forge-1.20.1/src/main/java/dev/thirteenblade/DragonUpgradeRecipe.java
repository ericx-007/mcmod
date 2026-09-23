package dev.thirteenblade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.Level;

public final class DragonUpgradeRecipe extends CustomRecipe {
    public DragonUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    @Override public boolean matches(CraftingContainer input, Level level) {
        int swords = 0, eggs = 0;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ThirteenBlade.SWORD.get())) swords++;
            else if (stack.is(Items.DRAGON_EGG)) eggs++;
            else if (!stack.isEmpty()) return false;
        }
        return swords == 1 && eggs == 1;
    }
    @Override public ItemStack assemble(CraftingContainer input, net.minecraft.core.RegistryAccess registries) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack source = input.getItem(i);
            if (!source.is(ThirteenBlade.SWORD.get())) continue;
            ItemStack result = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
            // Preserve custom NBT; the new item supplies its stronger default attack attributes.
            if (source.hasTag()) result.setTag(source.getTag().copy());
            BladeEnchantments.ensure(result);
            return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ThirteenBlade.DRAGON_UPGRADE.get(); }
}
