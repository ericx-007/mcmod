package dev.thirteenblade;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.LinkedHashMap;
import java.util.Map;

/** All permanent progression belongs to the individual sword, not the player. */
public final class BladeData {
    private static final String ROOT = "ThirteenBlade";
    public static final String HUNGER_WARD = "HungerWard";
    public static final String NIGHT_SIGHT = "NightSight";
    public static final String SLOW_FALL = "SlowFall";
    public static final String CREEPER_SHIELD = "CreeperShield";

    public record StoredEffect(StatusEffect effect, int amplifier, long expiresAt) {
        public int durationTicks(long now) {
            return expiresAt < 0 ? -1 : (int) Math.max(1, Math.min(Integer.MAX_VALUE, (expiresAt - now + 49) / 50));
        }
    }

    private BladeData() {}

    public static NbtCompound read(ItemStack stack) {
        NbtCompound nbt = stack.getSubNbt(ROOT);
        return nbt == null ? new NbtCompound() : nbt;
    }

    public static NbtCompound write(ItemStack stack) {
        return stack.getOrCreateSubNbt(ROOT);
    }

    public static int kills(ItemStack stack) { return Math.max(0, read(stack).getInt("Kills")); }
    public static String identity(ItemStack stack) {
        NbtCompound data = read(stack);
        return data.containsUuid("Identity") ? data.getUuid("Identity").toString() : "new-sword";
    }
    public static boolean advanced(ItemStack stack) { return stack.isOf(ThirteenBlade.DRAGON_SWORD); }
    public static double baseAttack(ItemStack stack) { return advanced(stack) ? 12 : 6; }
    public static int level(ItemStack stack) {
        int level = Progression.level(kills(stack), ThirteenBlade.balance);
        return advanced(stack) ? level : Math.min(10, level);
    }
    public static int soulCount(ItemStack stack) {
        return discoveries(stack).size();
    }
    public static double toughnessBonus(ItemStack stack) { return 2.0 * soulCount(stack); }

    private static java.util.Set<String> discoveries(ItemStack stack) {
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        NbtList list = read(stack).getList("SoulDiscoveries", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        for (SoulPower power : SoulPower.values()) if (power.known(stack)) result.add("family:" + power.name());
        return result;
    }

    public static boolean discover(ItemStack sword, net.minecraft.entity.LivingEntity victim) {
        SoulPower power = SoulPower.of(victim);
        if (power == null && victim.getType().getSpawnGroup() != net.minecraft.entity.SpawnGroup.MONSTER) return false;
        String key = power == null ? Registries.ENTITY_TYPE.getId(victim.getType()).toString() : "family:" + power.name();
        var entries = discoveries(sword);
        if (!entries.add(key)) return false;
        NbtList saved = new NbtList();
        for (String entry : entries) saved.add(net.minecraft.nbt.NbtString.of(entry));
        write(sword).put("SoulDiscoveries", saved);
        return true;
    }
    public static boolean has(ItemStack stack, String power) { return read(stack).getBoolean(power); }
    public static double damageBonus(ItemStack stack) { return level(stack) * ThirteenBlade.balance.damagePerLevel; }
    public static double healthBonus(ItemStack stack) { return level(stack) * ThirteenBlade.balance.healthPerLevel; }

    public static void addKill(ItemStack stack) {
        write(stack).putInt("Kills", Progression.nextKill(kills(stack)));
    }

    public static boolean unlock(ItemStack stack, String power) {
        if (has(stack, power)) return false;
        write(stack).putBoolean(power, true);
        return true;
    }

    public static boolean stealable(StatusEffect effect) {
        return !effect.isInstant() && effect.getCategory() == StatusEffectCategory.BENEFICIAL;
    }

    public static Map<StatusEffect, StoredEffect> stolenEffects(ItemStack stack, long now) {
        Map<StatusEffect, StoredEffect> result = new LinkedHashMap<>();
        NbtList list = read(stack).getList("StolenEffects", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < Math.min(64, list.size()); i++) {
            NbtCompound data = list.getCompound(i);
            Identifier id = Identifier.tryParse(data.getString("Id"));
            StatusEffect effect = id == null ? null : Registries.STATUS_EFFECT.getOrEmpty(id).orElse(null);
            long expiry = data.getLong("ExpiresAt");
            if (effect == null || !stealable(effect) || (expiry >= 0 && expiry <= now)) continue;
            int amp = Math.max(0, Math.min(ThirteenBlade.balance.maxStolenEffectLevel - 1, data.getInt("Amplifier")));
            StoredEffect old = result.get(effect);
            if (old == null || old.amplifier < amp) result.put(effect, new StoredEffect(effect, amp, expiry));
        }
        return result;
    }

    public static boolean capture(ItemStack sword, StatusEffectInstance effect, long now) {
        if (!stealable(effect.getEffectType())) return false;
        Map<StatusEffect, StoredEffect> effects = stolenEffects(sword, now);
        int amp = Math.max(0, Math.min(ThirteenBlade.balance.maxStolenEffectLevel - 1, effect.getAmplifier()));
        long expiry = ThirteenBlade.balance.stolenEffectDurationSeconds < 0 ? -1
                : now + ThirteenBlade.balance.stolenEffectDurationSeconds * 1000L;
        StoredEffect old = effects.get(effect.getEffectType());
        if (old != null) {
            amp = Math.max(amp, old.amplifier);
            expiry = old.expiresAt < 0 || expiry < 0 ? -1 : Math.max(old.expiresAt, expiry);
            if (amp == old.amplifier && expiry == old.expiresAt) return false;
        } else if (effects.size() >= 64) return false;
        effects.put(effect.getEffectType(), new StoredEffect(effect.getEffectType(), amp, expiry));
        NbtList list = new NbtList();
        for (StoredEffect stored : effects.values()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Id", Registries.STATUS_EFFECT.getId(stored.effect).toString());
            entry.putInt("Amplifier", stored.amplifier);
            entry.putLong("ExpiresAt", stored.expiresAt);
            list.add(entry);
        }
        write(sword).put("StolenEffects", list);
        return true;
    }

    public static Map<StatusEffect, StoredEffect> desiredEffects(ItemStack sword, long now) {
        Map<StatusEffect, StoredEffect> effects = stolenEffects(sword, now);
        for (SoulPower power : SoulPower.values()) {
            if (power.effect == null || !power.known(sword)) continue;
            StoredEffect existing = effects.get(power.effect);
            int amp = existing == null ? power.amplifier : Math.max(power.amplifier, existing.amplifier());
            effects.put(power.effect, new StoredEffect(power.effect, amp, -1));
        }
        return effects;
    }

    public static float shield(ItemStack sword, int amplifier) {
        float capacity = 4 * (amplifier + 1);
        float saved = read(sword).contains("ShieldRemaining", NbtElement.NUMBER_TYPE)
                ? read(sword).getFloat("ShieldRemaining") : capacity;
        return Float.isFinite(saved) ? Math.max(0, Math.min(capacity, saved)) : 0;
    }

    public static void saveShield(ItemStack sword, float amount) {
        float safe = Float.isFinite(amount) ? Math.max(0, amount) : 0;
        if (!read(sword).contains("ShieldRemaining") || read(sword).getFloat("ShieldRemaining") != safe)
            write(sword).putFloat("ShieldRemaining", safe);
    }

    public static long cooldownRemaining(ItemStack stack, long nowMillis) {
        long stored = read(stack).getLong("CooldownUntil");
        // Clamp imported/edited data so it can never lock a sword indefinitely.
        long maximum = ThirteenBlade.balance.absorptionCooldownSeconds * 1000L;
        if (stored <= nowMillis) return 0;
        if (stored > nowMillis + maximum) {
            write(stack).putLong("CooldownUntil", nowMillis + maximum);
            return maximum;
        }
        return stored - nowMillis;
    }
}
