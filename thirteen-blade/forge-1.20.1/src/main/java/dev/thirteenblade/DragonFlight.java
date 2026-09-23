package dev.thirteenblade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Only revoke flight granted by this sword; creative/spectator abilities remain vanilla-owned. */
public final class DragonFlight {
    private static final Map<UUID, Long> OWNED = new HashMap<>();
    private DragonFlight() {}

    public static void refresh(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) { OWNED.remove(player.getUUID()); return; }
        long tick = player.getServer().overworld().getGameTime();
        boolean active = player.isAlive() && SoulPower.DRAGON.known(BladeInventory.activeSword(player));
        if (active) {
            if (!player.getAbilities().mayfly) {
                OWNED.put(player.getUUID(), tick + BladeEffects.LINGER_TICKS);
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            } else if (OWNED.containsKey(player.getUUID())) OWNED.put(player.getUUID(), tick + BladeEffects.LINGER_TICKS);
        } else if (OWNED.containsKey(player.getUUID()) && tick >= OWNED.get(player.getUUID())) release(player);
    }

    public static void release(ServerPlayer player) {
        if (OWNED.remove(player.getUUID()) == null || player.isCreative() || player.isSpectator()) return;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    public static void clear() { OWNED.clear(); }
}
