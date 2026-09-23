package dev.thirteenblade;

import com.google.gson.Gson;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BladeNetwork {
    private static final Gson GSON = new Gson();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ThirteenBlade.id("main"), () -> "1", "1"::equals, "1"::equals);
    public static Consumer<State> clientState = ignored -> {};
    public record Absorb() {}
    public record State(boolean armed, int cooldown) {}
    public record Balance(String json) {}
    private BladeNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(Absorb.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> {}).decoder(buffer -> new Absorb())
            .consumerMainThread((message, context) -> { var player = context.get().getSender(); if (player != null) BladeGameplay.toggleAbsorption(player); }).add();
        CHANNEL.messageBuilder(State.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message, buffer) -> { buffer.writeBoolean(message.armed); buffer.writeVarInt(message.cooldown); })
            .decoder(buffer -> new State(buffer.readBoolean(), buffer.readVarInt()))
            .consumerMainThread((message, context) -> clientState.accept(message)).add();
        CHANNEL.messageBuilder(Balance.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message, buffer) -> buffer.writeUtf(message.json, 8192)).decoder(buffer -> new Balance(buffer.readUtf(8192)))
            .consumerMainThread((message, context) -> ThirteenBlade.balance = GSON.fromJson(message.json, BalanceConfig.class).validated()).add();
    }
    public static void absorb() { CHANNEL.sendToServer(new Absorb()); }
    public static void sendStatus(ServerPlayer player, boolean armed, int cooldown) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer || player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new State(armed, cooldown));
    }
    public static void sendBalance(ServerPlayer player) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer || player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Balance(GSON.toJson(ThirteenBlade.localBalance)));
    }
}
