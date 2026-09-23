package dev.thirteenblade;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

public final class BladeNetwork {
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(ThirteenBlade.ID);
    private BladeNetwork() {}
    public static void register() {
        CHANNEL.registerMessage(AbsorbHandler.class, Absorb.class, 0, Side.SERVER);
        CHANNEL.registerMessage(StateHandler.class, State.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(BalanceHandler.class, Balance.class, 2, Side.CLIENT);
    }
    public static void absorb() { CHANNEL.sendToServer(new Absorb()); }
    public static void status(EntityPlayerMP player, boolean armed, int cooldown) { if (connected(player)) CHANNEL.sendTo(new State(armed, cooldown), player); }
    public static void balance(EntityPlayerMP player) { if (connected(player)) CHANNEL.sendTo(new Balance(new Gson().toJson(ThirteenBlade.balance)), player); }
    private static boolean connected(EntityPlayerMP player) { return player.connection != null && player.connection.netManager.isChannelOpen() && !(player instanceof net.minecraftforge.common.util.FakePlayer); }
    public static class Absorb implements IMessage { public void fromBytes(ByteBuf b) {} public void toBytes(ByteBuf b) {} }
    public static class State implements IMessage {
        boolean armed; int cooldown;
        public State() {} State(boolean armed, int cooldown) { this.armed = armed; this.cooldown = cooldown; }
        public void fromBytes(ByteBuf b) { armed = b.readBoolean(); cooldown = Math.max(0, Math.min(3600, b.readInt())); }
        public void toBytes(ByteBuf b) { b.writeBoolean(armed); b.writeInt(cooldown); }
    }
    public static class Balance implements IMessage {
        String json; public Balance() {} Balance(String json) { this.json = json; }
        public void fromBytes(ByteBuf b) { if (b.readableBytes() > 8192) throw new IllegalArgumentException("Oversized balance packet"); json = ByteBufUtils.readUTF8String(b); }
        public void toBytes(ByteBuf b) { ByteBufUtils.writeUTF8String(b, json); }
    }
    public static class AbsorbHandler implements IMessageHandler<Absorb, IMessage> {
        public IMessage onMessage(Absorb message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> BladeGameplay.toggleAbsorption(player)); return null;
        }
    }
    public static class StateHandler implements IMessageHandler<State, IMessage> {
        public IMessage onMessage(State message, MessageContext context) { ThirteenBlade.proxy.status(message.armed, message.cooldown); return null; }
    }
    public static class BalanceHandler implements IMessageHandler<Balance, IMessage> {
        public IMessage onMessage(Balance message, MessageContext context) { ThirteenBlade.proxy.balance(message.json); return null; }
    }
}
