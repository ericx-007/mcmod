package dev.thirteenblade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ThirteenBlade.ID, name = "Thirteen Blade", version = "0.4.1", acceptedMinecraftVersions = "[1.12.2]", dependencies = "required-after:forge@[14.23.5.2847,)")
public final class ThirteenBlade {
    public static final String ID = "thirteenblade";
    public static final Logger LOGGER = LogManager.getLogger(ID);
    public static BalanceConfig balance = new BalanceConfig(), localBalance = balance;
    public static final ThirteenBladeItem SWORD = new ThirteenBladeItem(false);
    public static final ThirteenBladeItem DRAGON_SWORD = new ThirteenBladeItem(true);
    @SidedProxy(clientSide = "dev.thirteenblade.client.ClientProxy", serverSide = "dev.thirteenblade.CommonProxy")
    public static CommonProxy proxy;
    public static boolean isSword(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof ThirteenBladeItem; }
    public static ItemStack sword(boolean advanced) { ItemStack stack = new ItemStack(advanced ? DRAGON_SWORD : SWORD); BladeEnchantments.ensure(stack); return stack; }
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        localBalance = BalanceConfig.load(event.getModConfigurationDirectory().toPath().resolve("thirteenblade.json")); balance = localBalance;
        BladeNetwork.register();
        net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(new ResourceLocation(ID, "fireproof_item"), ThirteenBladeItem.FireproofItem.class,
                "fireproof_item", 0, this, 64, 20, true);
        MinecraftForge.EVENT_BUS.register(new BladeGameplay());
        MinecraftForge.EVENT_BUS.register(new BladeEffects());
        MinecraftForge.EVENT_BUS.register(new EliteMobs());
        FirstAidBridge.initialize();
        BladeGameplay.raiseToughnessLimit();
        proxy.initialize(event.getModConfigurationDirectory().toPath());
    }
    @Mod.EventHandler public void starting(FMLServerStartingEvent event) { balance = localBalance; }
    @Mod.EventHandler public void stopping(FMLServerStoppingEvent event) { BladeGameplay.stop(); }
    @Mod.EventBusSubscriber(modid = ID)
    public static final class Registry {
        @SubscribeEvent public static void items(RegistryEvent.Register<Item> event) { event.getRegistry().registerAll(SWORD, DRAGON_SWORD); }
        @SubscribeEvent public static void recipes(RegistryEvent.Register<IRecipe> event) {
            event.getRegistry().register(new DragonUpgradeRecipe().setRegistryName(new ResourceLocation(ID, "dragon_thirteen_blade")));
        }
    }
}
