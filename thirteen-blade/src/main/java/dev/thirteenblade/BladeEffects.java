package dev.thirteenblade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/** Track each effect's source so lingering powers never overwrite ordinary potions or another sword's shield. */
public final class BladeEffects {
    public static final int LINGER_TICKS = 100;
    private static final Map<UUID, Map<StatusEffect, Grant>> GRANTS = new HashMap<>();

    private static final class Grant {
        final ItemStack sword;
        final long expiresAt;
        long leaveUntil = -1;
        Grant(ItemStack sword, long expiresAt) { this.sword = sword; this.expiresAt = expiresAt; }
    }

    private BladeEffects() {}
    public static boolean owned(StatusEffectInstance effect) {
        return effect instanceof BladeOwnedEffect marker && marker.thirteenblade$isOwned();
    }

    public static void beforeRemoval(ServerPlayerEntity player, StatusEffectInstance effect) {
        var grants = GRANTS.get(player.getUuid());
        Grant grant = grants == null ? null : grants.get(effect.getEffectType());
        if (grant != null && owned(effect) && effect.getEffectType() == StatusEffects.ABSORPTION)
            BladeData.saveShield(grant.sword, player.getAbsorptionAmount());
    }

    public static void release(ServerPlayerEntity player) {
        DragonFlight.release(player);
        for (StatusEffectInstance effect : new ArrayList<>(player.getStatusEffects()))
            if (owned(effect)) player.removeStatusEffect(effect.getEffectType());
        GRANTS.remove(player.getUuid());
    }

    public static void clear() { GRANTS.clear(); DragonFlight.clear(); }

    public static void refresh(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator()) { release(player); return; }
        DragonFlight.refresh(player);
        ItemStack sword = BladeInventory.activeSword(player);
        long now = System.currentTimeMillis();
        long tick = player.getServer().getOverworld().getTime();
        var desired = sword.isEmpty() ? Map.<StatusEffect, BladeData.StoredEffect>of() : BladeData.desiredEffects(sword, now);
        var grants = GRANTS.computeIfAbsent(player.getUuid(), ignored -> new HashMap<>());
        grants.entrySet().removeIf(entry -> !owned(player.getStatusEffect(entry.getKey())));

        for (StatusEffectInstance effect : new ArrayList<>(player.getStatusEffects())) {
            if (!owned(effect)) continue;
            StatusEffect type = effect.getEffectType();
            Grant grant = grants.get(type);
            var target = desired.get(type);
            boolean retained = grant != null && grant.sword == sword && target != null;
            if (retained && grant.leaveUntil < 0 && target.amplifier() == effect.getAmplifier()
                    && target.expiresAt() == grant.expiresAt
                    && (target.expiresAt() < 0) == effect.isInfinite()) continue;

            // Changed/expired powers are replaced immediately. A new sword takes over shared effects;
            // unrelated powers from the previous sword finish their grace period.
            if (grant == null || target != null || grant.sword == sword
                    || (grant.expiresAt >= 0 && grant.expiresAt <= now)
                    || (grant.leaveUntil >= 0 && tick >= grant.leaveUntil)) {
                player.removeStatusEffect(type);
                grants.remove(type);
                continue;
            }
            if (grant.leaveUntil < 0) {
                int duration = effect.isInfinite() ? LINGER_TICKS : Math.min(LINGER_TICKS, effect.getDuration());
                grant.leaveUntil = tick + duration;
                // Sync a finite duration once. Restore saved yellow hearts after vanilla applies it.
                player.removeStatusEffect(type);
                apply(player, grant, type, effect.getAmplifier(), duration);
            }
        }
        if (!sword.isEmpty() && BladeData.has(sword, BladeData.HUNGER_WARD)) player.removeStatusEffect(StatusEffects.HUNGER);
        if (SoulPower.WARDEN.known(sword)) {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
            player.removeStatusEffect(StatusEffects.DARKNESS);
        }
        for (var target : desired.values()) {
            if (player.getStatusEffect(target.effect()) != null) continue;
            Grant grant = new Grant(sword, target.expiresAt());
            grants.put(target.effect(), grant);
            if (!apply(player, grant, target.effect(), target.amplifier(), target.durationTicks(now)))
                grants.remove(target.effect());
        }
        StatusEffectInstance shield = player.getStatusEffect(StatusEffects.ABSORPTION);
        Grant shieldGrant = grants.get(StatusEffects.ABSORPTION);
        if (owned(shield) && shieldGrant != null)
            BladeData.saveShield(shieldGrant.sword, player.getAbsorptionAmount());
        if (grants.isEmpty()) GRANTS.remove(player.getUuid());
    }

    private static boolean apply(ServerPlayerEntity player, Grant grant, StatusEffect type, int amplifier, int duration) {
        StatusEffectInstance applied = new StatusEffectInstance(type, duration, amplifier, true, false, true);
        ((BladeOwnedEffect) applied).thirteenblade$setOwned(true);
        boolean added = player.addStatusEffect(applied);
        if (added && type == StatusEffects.ABSORPTION)
            player.setAbsorptionAmount(BladeData.shield(grant.sword, amplifier));
        return added;
    }

    /** Only another absorption kill replenishes yellow hearts. */
    public static void replenishShield(ServerPlayerEntity player, ItemStack sword) {
        var desired = BladeData.desiredEffects(sword, System.currentTimeMillis()).get(StatusEffects.ABSORPTION);
        if (desired == null) return;
        float capacity = 4 * (desired.amplifier() + 1);
        var grants = GRANTS.get(player.getUuid());
        Grant grant = grants == null ? null : grants.get(StatusEffects.ABSORPTION);
        StatusEffectInstance current = player.getStatusEffect(StatusEffects.ABSORPTION);
        if (owned(current) && grant != null && grant.sword == sword) player.setAbsorptionAmount(capacity);
        BladeData.saveShield(sword, capacity);
    }
}
