package dev.thirteenblade.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.thirteenblade.BladeData;
import dev.thirteenblade.BladeInventory;
import dev.thirteenblade.BladeNetwork;
import dev.thirteenblade.ThirteenBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ThirteenBlade.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ThirteenBladeClient {
    public static final KeyMapping absorbKey = new KeyMapping("key.thirteenblade.absorb", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.thirteenblade");
    public static final KeyMapping chatKey = new KeyMapping("key.thirteenblade.chat", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.thirteenblade");
    public static int armedSeconds, cooldownSeconds;

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(absorbKey); event.register(chatKey); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SwordChat.initialize();
            BladeNetwork.clientState = state -> { armedSeconds = state.armed() ? 1 : 0; cooldownSeconds = state.cooldown(); };
            NeoForge.EVENT_BUS.addListener(ThirteenBladeClient::tick);
            NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut ignored) -> {
                armedSeconds = cooldownSeconds = 0;
                ThirteenBlade.balance = ThirteenBlade.localBalance;
                SwordChat.clear();
            });
        });
    }
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ThirteenBlade.id("blade_status"), (graphics, tracker) -> renderHud(graphics));
    }
    private static void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        while (absorbKey.consumeClick()) {
            if (client.player != null && client.screen == null) PacketDistributor.sendToServer(new BladeNetwork.Absorb());
        }
        while (chatKey.consumeClick()) {
            if (client.player == null || client.screen != null) continue;
            var sword = BladeInventory.activeSword(client.player);
            if (ThirteenBlade.isSword(sword)) client.setScreen(new SwordChatScreen(sword));
            else client.player.displayClientMessage(Component.translatable("message.thirteenblade.hold"), true);
        }
    }
    private static void renderHud(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || client.screen != null) return;
        var sword = BladeInventory.activeSword(client.player);
        if (!ThirteenBlade.isSword(sword)) return;
        Component status = armedSeconds > 0 ? Component.translatable("hud.thirteenblade.armed")
                : cooldownSeconds > 0 ? Component.translatable("hud.thirteenblade.cooldown", cooldownSeconds)
                : Component.translatable("hud.thirteenblade.ready", absorbKey.getTranslatedKeyMessage());
        Component level = Component.translatable("hud.thirteenblade.level", BladeData.level(sword), BladeData.kills(sword));
        int w = Math.max(client.font.width(status), client.font.width(level)) + 16;
        int x = graphics.guiWidth() - w - 8, y = graphics.guiHeight() - 60;
        graphics.fill(x, y, x + w, y + 34, 0xBB101522);
        graphics.fill(x, y, x + 2, y + 34, armedSeconds > 0 ? 0xFFCE98FF : 0xFF5EDADA);
        graphics.drawString(client.font, level, x + 8, y + 6, 0xD9E8F2);
        graphics.drawString(client.font, status, x + 8, y + 20, armedSeconds > 0 ? 0xD5ACFF : 0x7FE7DE);
    }
}
