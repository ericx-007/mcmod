package dev.thirteenblade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThirteenBlade implements ModInitializer {
    public static final String ID = "thirteenblade";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final ThirteenBladeItem SWORD = new ThirteenBladeItem(false);
    public static final ThirteenBladeItem DRAGON_SWORD = new ThirteenBladeItem(true);
    public static final net.minecraft.recipe.RecipeSerializer<DragonUpgradeRecipe> DRAGON_UPGRADE = new net.minecraft.recipe.SpecialRecipeSerializer<>(DragonUpgradeRecipe::new);
    public static BalanceConfig balance = new BalanceConfig();
    public static BalanceConfig localBalance = balance;

    public static Identifier id(String path) { return new Identifier(ID, path); }

    public static boolean isSword(net.minecraft.item.ItemStack stack) { return stack.getItem() instanceof ThirteenBladeItem; }

    @Override
    public void onInitialize() {
        localBalance = BalanceConfig.load(FabricLoader.getInstance().getConfigDir().resolve("thirteenblade.json"));
        balance = localBalance;
        Registry.register(Registries.ITEM, id("thirteen_blade"), SWORD);
        Registry.register(Registries.ITEM, id("dragon_thirteen_blade"), DRAGON_SWORD);
        Registry.register(Registries.RECIPE_SERIALIZER, id("dragon_upgrade"), DRAGON_UPGRADE);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> { entries.add(SWORD); entries.add(DRAGON_SWORD); });
        BladeGameplay.register();
        EliteMobs.register();
        LOGGER.info("Thirteen Blade initialized (Minecraft 1.20.1)");
    }
}
