package dev.thirteenblade;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ThirteenBlade.ID)
public final class ThirteenBlade {
    public static final String ID = "thirteenblade";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredItem<ThirteenBladeItem> SWORD = ITEMS.register("thirteen_blade", () -> new ThirteenBladeItem(false));
    public static final DeferredItem<ThirteenBladeItem> DRAGON_SWORD = ITEMS.register("dragon_thirteen_blade", () -> new ThirteenBladeItem(true));
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<DragonUpgradeRecipe>> DRAGON_UPGRADE =
            RECIPES.register("dragon_upgrade", () -> new SimpleCraftingRecipeSerializer<>(DragonUpgradeRecipe::new));
    public static BalanceConfig balance = new BalanceConfig();
    public static BalanceConfig localBalance = balance;

    public ThirteenBlade(IEventBus bus) {
        localBalance = BalanceConfig.load(FMLPaths.CONFIGDIR.get().resolve("thirteenblade.json"));
        balance = localBalance;
        ITEMS.register(bus);
        RECIPES.register(bus);
        bus.addListener(this::creativeTab);
        bus.addListener(BladeNetwork::register);
        BladeGameplay.register();
        EliteMobs.register();
    }
    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) { event.accept(SWORD); event.accept(DRAGON_SWORD); }
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    public static boolean isSword(net.minecraft.world.item.ItemStack stack) { return stack.getItem() instanceof ThirteenBladeItem; }
}
