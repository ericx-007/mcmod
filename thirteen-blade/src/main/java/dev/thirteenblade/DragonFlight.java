package dev.thirteenblade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

/** Only revoke flight granted by this sword; creative/spectator abilities remain vanilla-owned. */
public final class DragonFlight {
    private static final Map<UUID, Long> OWNED = new HashMap<>();
    private DragonFlight() {}

    public static void refresh(ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) { OWNED.remove(player.getUuid()); return; }
        long tick = player.getServer().getOverworld().getTime();
        boolean active = player.isAlive() && SoulPower.DRAGON.known(BladeInventory.activeSword(player));
        if (active) {
            if (!player.getAbilities().allowFlying) {
                OWNED.put(player.getUuid(), tick + BladeEffects.LINGER_TICKS);
                player.getAbilities().allowFlying = true;
                player.sendAbilitiesUpdate();
            } else if (OWNED.containsKey(player.getUuid())) OWNED.put(player.getUuid(), tick + BladeEffects.LINGER_TICKS);
        } else if (OWNED.containsKey(player.getUuid()) && tick >= OWNED.get(player.getUuid())) release(player);
    }

    public static void release(ServerPlayerEntity player) {
        if (OWNED.remove(player.getUuid()) == null || player.isCreative() || player.isSpectator()) return;
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.sendAbilitiesUpdate();
    }

    public static void clear() { OWNED.clear(); }
}
