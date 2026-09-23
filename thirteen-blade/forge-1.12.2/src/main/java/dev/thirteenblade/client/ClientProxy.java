package dev.thirteenblade.client;

import com.google.gson.Gson;
import dev.thirteenblade.*;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = ThirteenBlade.ID, value = Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    public static KeyBinding absorbKey, chatKey;
    private static boolean armed; private static int cooldown;
    @Override public void initialize(Path configDirectory) {
        absorbKey = new KeyBinding("key.thirteenblade.absorb", Keyboard.KEY_V, "category.thirteenblade");
        chatKey = new KeyBinding("key.thirteenblade.chat", Keyboard.KEY_J, "category.thirteenblade");
        ClientRegistry.registerKeyBinding(absorbKey); ClientRegistry.registerKeyBinding(chatKey);
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(ThirteenBladeItem.FireproofItem.class,
                manager -> new net.minecraft.client.renderer.entity.RenderEntityItem(manager, Minecraft.getMinecraft().getRenderItem()));
        MinecraftForge.EVENT_BUS.register(this); SwordChat.initialize(configDirectory);
    }
    @SubscribeEvent public static void models(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ThirteenBlade.SWORD, 0, new ModelResourceLocation(ThirteenBlade.SWORD.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(ThirteenBlade.DRAGON_SWORD, 0, new ModelResourceLocation(ThirteenBlade.DRAGON_SWORD.getRegistryName(), "inventory"));
    }
    @Override public void status(boolean active, int seconds) { Minecraft.getMinecraft().addScheduledTask(() -> { armed = active; cooldown = seconds; }); }
    @Override public void balance(String json) { Minecraft.getMinecraft().addScheduledTask(() -> {
        try { BalanceConfig config = new Gson().fromJson(json, BalanceConfig.class); if (config != null) ThirteenBlade.balance = config.validated(); }
        catch (RuntimeException ignored) { /* Keep the last valid server configuration. */ }
    }); }
    @SubscribeEvent public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) { Minecraft.getMinecraft().addScheduledTask(() -> { armed = false; cooldown = 0; SwordChat.clear(); ThirteenBlade.balance = ThirteenBlade.localBalance; }); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.player == null) return;
        while (absorbKey.isPressed()) if (mc.currentScreen == null) BladeNetwork.absorb();
        while (chatKey.isPressed()) {
            if (mc.currentScreen != null) continue;
            ItemStack sword = BladeInventory.activeSword(mc.player);
            if (!sword.isEmpty()) mc.displayGuiScreen(new SwordChatScreen(sword));
            else mc.player.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("message.thirteenblade.hold"), true);
        }
    }
    @SubscribeEvent public void hud(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null || mc.gameSettings.hideGUI || mc.currentScreen != null) return;
        ItemStack sword = BladeInventory.activeSword(mc.player); if (sword.isEmpty()) return;
        int y = event.getResolution().getScaledHeight() - 68;
        mc.fontRenderer.drawStringWithShadow(I18n.format("hud.thirteenblade.level", BladeData.level(sword), BladeData.kills(sword)), 8, y, 0xC9B7F5);
        String text = armed ? I18n.format("hud.thirteenblade.armed") : cooldown > 0 ? I18n.format("hud.thirteenblade.cooldown", cooldown) : I18n.format("hud.thirteenblade.ready", absorbKey.getDisplayName());
        mc.fontRenderer.drawStringWithShadow(text, 8, y + 12, 0xDDDDEE);
    }
}
