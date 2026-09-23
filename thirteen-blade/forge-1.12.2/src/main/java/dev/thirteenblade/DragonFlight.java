package dev.thirteenblade;

import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;

public final class DragonFlight {
    private static final String SAVED = "thirteenblade.flight";
    private static final Map<UUID, Long> OWNED = new HashMap<>();
    private DragonFlight() {}
    public static void refresh(EntityPlayerMP player) {
        if (player.isCreative() || player.isSpectator()) { OWNED.remove(player.getUniqueID()); player.getEntityData().removeTag(SAVED); return; }
        long tick = BladeGameplay.tick(player);
        boolean active = player.isEntityAlive() && SoulPower.DRAGON.known(BladeInventory.activeSword(player));
        if (active) {
            if (!player.capabilities.allowFlying) {
                OWNED.put(player.getUniqueID(), tick + 100); player.getEntityData().setBoolean(SAVED, true);
                player.capabilities.allowFlying = true; player.sendPlayerAbilities();
            } else if (OWNED.containsKey(player.getUniqueID())) OWNED.put(player.getUniqueID(), tick + 100);
        } else if (OWNED.containsKey(player.getUniqueID()) && tick >= OWNED.get(player.getUniqueID())) release(player);
    }
    public static void release(EntityPlayerMP player) {
        boolean owned = OWNED.remove(player.getUniqueID()) != null || player.getEntityData().getBoolean(SAVED);
        player.getEntityData().removeTag(SAVED);
        if (owned && !player.isCreative() && !player.isSpectator()) { player.capabilities.allowFlying = false; player.capabilities.isFlying = false; player.sendPlayerAbilities(); }
    }
    public static void clear() { OWNED.clear(); }
}
