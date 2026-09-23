package dev.thirteenblade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ThirteenBlade.ID)
public final class ThirteenBlade {
    public static final String ID = "thirteenblade";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final RegistryObject<ThirteenBladeItem> SWORD = ITEMS.register("thirteen_blade", () -> new ThirteenBladeItem(false));
    public static final RegistryObject<ThirteenBladeItem> DRAGON_SWORD = ITEMS.register("dragon_thirteen_blade", () -> new ThirteenBladeItem(true));
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ID);
    public static final RegistryObject<SimpleCraftingRecipeSerializer<DragonUpgradeRecipe>> DRAGON_UPGRADE =
            RECIPES.register("dragon_upgrade", () -> new SimpleCraftingRecipeSerializer<>(DragonUpgradeRecipe::new));
    public static BalanceConfig balance = new BalanceConfig(), localBalance = balance;

    public ThirteenBlade() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        localBalance = BalanceConfig.load(FMLPaths.CONFIGDIR.get().resolve("thirteenblade.json"));
        balance = localBalance;
        ITEMS.register(bus); RECIPES.register(bus);
        bus.addListener(this::creativeTab);
        BladeNetwork.register(); BladeGameplay.register(); EliteMobs.register();
    }
    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SWORD.get().getDefaultInstance()); event.accept(DRAGON_SWORD.get().getDefaultInstance());
        }
    }
    public static ResourceLocation id(String path) { return new ResourceLocation(ID, path); }
    public static boolean isSword(net.minecraft.world.item.ItemStack stack) { return stack.getItem() instanceof ThirteenBladeItem; }
}
