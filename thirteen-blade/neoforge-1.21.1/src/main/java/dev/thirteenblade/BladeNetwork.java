package dev.thirteenblade;

import com.google.gson.Gson;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BladeNetwork {
    private static final Gson GSON = new Gson();
    public static Consumer<State> clientState = ignored -> {};
    private BladeNetwork() {}

    public record Absorb() implements CustomPacketPayload {
        public static final Type<Absorb> TYPE = new Type<>(ThirteenBlade.id("absorb"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Absorb> CODEC = StreamCodec.unit(new Absorb());
        @Override public Type<Absorb> type() { return TYPE; }
    }
    public record State(boolean armed, int cooldown) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ThirteenBlade.id("status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf buffer) { return new State(buffer.readBoolean(), buffer.readVarInt()); }
            public void encode(RegistryFriendlyByteBuf buffer, State data) { buffer.writeBoolean(data.armed); buffer.writeVarInt(data.cooldown); }
        };
        @Override public Type<State> type() { return TYPE; }
    }
    public record Balance(String json) implements CustomPacketPayload {
        public static final Type<Balance> TYPE = new Type<>(ThirteenBlade.id("balance"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Balance> CODEC = new StreamCodec<>() {
            public Balance decode(RegistryFriendlyByteBuf buffer) { return new Balance(buffer.readUtf(8192)); }
            public void encode(RegistryFriendlyByteBuf buffer, Balance data) { buffer.writeUtf(data.json, 8192); }
        };
        @Override public Type<Balance> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("0.4");
        registrar.playToServer(Absorb.TYPE, Absorb.CODEC, (data, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) BladeGameplay.toggleAbsorption(player);
        }));
        registrar.playToClient(State.TYPE, State.CODEC, (data, context) -> context.enqueueWork(() -> clientState.accept(data)));
        registrar.playToClient(Balance.TYPE, Balance.CODEC, (data, context) -> context.enqueueWork(() ->
                ThirteenBlade.balance = GSON.fromJson(data.json, BalanceConfig.class).validated()));
    }
    public static void sendStatus(ServerPlayer player, boolean armed, int cooldown) {
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer || player.connection == null) return;
        PacketDistributor.sendToPlayer(player, new State(armed, cooldown));
    }
    public static void sendBalance(ServerPlayer player) {
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer || player.connection == null) return;
        PacketDistributor.sendToPlayer(player, new Balance(GSON.toJson(ThirteenBlade.localBalance)));
    }
}
