package dev.thirteenblade.client;

import dev.thirteenblade.BalanceConfig;
import dev.thirteenblade.BladeData;
import dev.thirteenblade.BladeInventory;
import dev.thirteenblade.BladeGameplay;
import dev.thirteenblade.ThirteenBlade;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ThirteenBladeClient implements ClientModInitializer {
    public static KeyBinding absorbKey;
    public static KeyBinding chatKey;
    public static int armedSeconds;
    public static int cooldownSeconds;

    @Override
    public void onInitializeClient() {
        absorbKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.thirteenblade.absorb",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.thirteenblade"));
        chatKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.thirteenblade.chat",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.thirteenblade"));
        SwordChat.initialize();
        ClientPlayNetworking.registerGlobalReceiver(BladeGameplay.STATUS, (client, handler, buf, sender) -> {
            int armed = buf.readVarInt();
            int cooldown = buf.readVarInt();
            client.execute(() -> { armedSeconds = armed; cooldownSeconds = cooldown; });
        });
        ClientPlayNetworking.registerGlobalReceiver(BladeGameplay.BALANCE, (client, handler, buf, sender) -> {
            BalanceConfig config = new BalanceConfig();
            config.killsPerLevel = buf.readVarInt();
            config.damagePerLevel = buf.readDouble();
            config.healthPerLevel = buf.readDouble();
            config.absorptionWindowSeconds = buf.readVarInt();
            config.absorptionCooldownSeconds = buf.readVarInt();
            config.eliteSpawnChance = buf.readDouble();
            config.eliteHealthMultiplier = buf.readDouble();
            config.eliteMaxEffects = buf.readVarInt();
            config.eliteMaxEffectLevel = buf.readVarInt();
            config.maxStolenEffectLevel = buf.readVarInt();
            config.stolenEffectDurationSeconds = buf.readInt();
            client.execute(() -> ThirteenBlade.balance = config.validated());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            armedSeconds = cooldownSeconds = 0;
            ThirteenBlade.balance = ThirteenBlade.localBalance;
            SwordChat.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (absorbKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null
                        && ClientPlayNetworking.canSend(BladeGameplay.ABSORB))
                    ClientPlayNetworking.send(BladeGameplay.ABSORB, PacketByteBufs.create());
            }
            while (chatKey.wasPressed()) {
                if (client.player == null || client.currentScreen != null) continue;
                ItemStack stack = BladeInventory.activeSword(client.player);
                if (stack.isOf(ThirteenBlade.SWORD)) client.setScreen(new SwordChatScreen(stack));
                else client.player.sendMessage(Text.translatable("message.thirteenblade.hold"), true);
            }
        });
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden || client.currentScreen != null) return;
            ItemStack sword = BladeInventory.activeSword(client.player);
            if (!sword.isOf(ThirteenBlade.SWORD)) return;
            Text status = armedSeconds > 0 ? Text.translatable("hud.thirteenblade.armed", armedSeconds)
                    : cooldownSeconds > 0 ? Text.translatable("hud.thirteenblade.cooldown", cooldownSeconds)
                    : Text.translatable("hud.thirteenblade.ready", absorbKey.getBoundKeyLocalizedText());
            Text level = Text.translatable("hud.thirteenblade.level", BladeData.level(sword), BladeData.kills(sword));
            int w = Math.max(client.textRenderer.getWidth(status), client.textRenderer.getWidth(level)) + 16;
            int x = context.getScaledWindowWidth() - w - 8;
            int y = context.getScaledWindowHeight() - 60;
            context.fill(x, y, x + w, y + 34, 0xBB101522);
            context.fill(x, y, x + 2, y + 34, armedSeconds > 0 ? 0xFFCE98FF : 0xFF5EDADA);
            context.drawTextWithShadow(client.textRenderer, level, x + 8, y + 6, 0xD9E8F2);
            context.drawTextWithShadow(client.textRenderer, status, x + 8, y + 20, armedSeconds > 0 ? 0xD5ACFF : 0x7FE7DE);
        });
    }
}
