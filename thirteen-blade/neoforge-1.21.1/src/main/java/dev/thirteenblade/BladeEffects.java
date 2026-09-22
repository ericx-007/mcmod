package dev.thirteenblade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/** Track each effect's source so lingering powers never overwrite ordinary potions or another sword's shield. */
public final class BladeEffects {
    public static final int LINGER_TICKS = 100;
    private static final Map<UUID, Map<net.minecraft.core.Holder<MobEffect>, Grant>> GRANTS = new HashMap<>();

    private static final class Grant {
        final ItemStack sword;
        final long expiresAt;
        long leaveUntil = -1;
        Grant(ItemStack sword, long expiresAt) { this.sword = sword; this.expiresAt = expiresAt; }
    }

    private BladeEffects() {}
    public static boolean owned(MobEffectInstance effect) {
        return effect instanceof BladeOwnedEffect marker && marker.thirteenblade$isOwned();
    }

    public static void beforeRemoval(ServerPlayer player, MobEffectInstance effect) {
        var grants = GRANTS.get(player.getUUID());
        Grant grant = grants == null ? null : grants.get(effect.getEffect());
        if (grant != null && owned(effect) && effect.getEffect() == MobEffects.ABSORPTION)
            BladeData.saveShield(grant.sword, player.getAbsorptionAmount());
    }

    public static void release(ServerPlayer player) {
        DragonFlight.release(player);
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects()))
            if (owned(effect)) player.removeEffect(effect.getEffect());
        GRANTS.remove(player.getUUID());
    }

    public static void clear() { GRANTS.clear(); DragonFlight.clear(); }

    public static void refresh(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) { release(player); return; }
        DragonFlight.refresh(player);
        ItemStack sword = BladeInventory.activeSword(player);
        long now = System.currentTimeMillis();
        long tick = player.getServer().overworld().getGameTime();
        var desired = sword.isEmpty() ? Map.<net.minecraft.core.Holder<MobEffect>, BladeData.StoredEffect>of() : BladeData.desiredEffects(sword, now);
        var grants = GRANTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        grants.entrySet().removeIf(entry -> !owned(player.getEffect(entry.getKey())));

        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (!owned(effect)) continue;
            net.minecraft.core.Holder<MobEffect> type = effect.getEffect();
            Grant grant = grants.get(type);
            var target = desired.get(type);
            boolean retained = grant != null && grant.sword == sword && target != null;
            if (retained && grant.leaveUntil < 0 && target.amplifier() == effect.getAmplifier()
                    && target.expiresAt() == grant.expiresAt
                    && (target.expiresAt() < 0) == effect.isInfiniteDuration()) continue;

            // Changed/expired powers are replaced immediately. A new sword takes over shared effects;
            // unrelated powers from the previous sword finish their grace period.
            if (grant == null || target != null || grant.sword == sword
                    || (grant.expiresAt >= 0 && grant.expiresAt <= now)
                    || (grant.leaveUntil >= 0 && tick >= grant.leaveUntil)) {
                player.removeEffect(type);
                grants.remove(type);
                continue;
            }
            if (grant.leaveUntil < 0) {
                int duration = effect.isInfiniteDuration() ? LINGER_TICKS : Math.min(LINGER_TICKS, effect.getDuration());
                grant.leaveUntil = tick + duration;
                // Sync a finite duration once. Restore saved yellow hearts after vanilla applies it.
                player.removeEffect(type);
                apply(player, grant, type, effect.getAmplifier(), duration);
            }
        }
        if (!sword.isEmpty() && BladeData.has(sword, BladeData.HUNGER_WARD)) player.removeEffect(MobEffects.HUNGER);
        if (SoulPower.WARDEN.known(sword)) {
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
        }
        for (var target : desired.values()) {
            if (player.getEffect(target.effect()) != null) continue;
            Grant grant = new Grant(sword, target.expiresAt());
            grants.put(target.effect(), grant);
            if (!apply(player, grant, target.effect(), target.amplifier(), target.durationTicks(now)))
                grants.remove(target.effect());
        }
        MobEffectInstance shield = player.getEffect(MobEffects.ABSORPTION);
        Grant shieldGrant = grants.get(MobEffects.ABSORPTION);
        if (owned(shield) && shieldGrant != null)
            BladeData.saveShield(shieldGrant.sword, player.getAbsorptionAmount());
        if (grants.isEmpty()) GRANTS.remove(player.getUUID());
    }

    private static boolean apply(ServerPlayer player, Grant grant, net.minecraft.core.Holder<MobEffect> type, int amplifier, int duration) {
        MobEffectInstance applied = new MobEffectInstance(type, duration, amplifier, true, false, true);
        ((BladeOwnedEffect) applied).thirteenblade$setOwned(true);
        boolean added = player.addEffect(applied);
        if (added && type == MobEffects.ABSORPTION)
            player.setAbsorptionAmount(BladeData.shield(grant.sword, amplifier));
        return added;
    }

    /** Only another absorption kill replenishes yellow hearts. */
    public static void replenishShield(ServerPlayer player, ItemStack sword) {
        var desired = BladeData.desiredEffects(sword, System.currentTimeMillis()).get(MobEffects.ABSORPTION);
        if (desired == null) return;
        float capacity = 4 * (desired.amplifier() + 1);
        var grants = GRANTS.get(player.getUUID());
        Grant grant = grants == null ? null : grants.get(MobEffects.ABSORPTION);
        MobEffectInstance current = player.getEffect(MobEffects.ABSORPTION);
        if (owned(current) && grant != null && grant.sword == sword) player.setAbsorptionAmount(capacity);
        BladeData.saveShield(sword, capacity);
    }
}
