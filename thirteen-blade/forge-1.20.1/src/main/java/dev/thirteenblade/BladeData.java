package dev.thirteenblade;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Holder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;


/** All permanent sword state is saved in its own NBT compound. */
public final class BladeData {
    private static final String ROOT = "ThirteenBlade";
    public static final String HUNGER_WARD = "HungerWard", NIGHT_SIGHT = "NightSight", SLOW_FALL = "SlowFall", CREEPER_SHIELD = "CreeperShield";
    private BladeData() {}

    public record StoredEffect(MobEffect effect, int amplifier, long expiresAt) {
        public int durationTicks(long now) {
            return expiresAt < 0 ? -1 : (int) Math.max(1, Math.min(Integer.MAX_VALUE, (expiresAt - now + 49) / 50));
        }
    }

    public static CompoundTag read(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT);
        return tag == null ? new CompoundTag() : tag;
    }
    public static void edit(ItemStack stack, Consumer<CompoundTag> action) {
        action.accept(stack.getOrCreateTagElement(ROOT));
    }
    public static void ensureIdentity(ItemStack stack) {
        if (!read(stack).hasUUID("Identity")) edit(stack, data -> data.putUUID("Identity", java.util.UUID.randomUUID()));
    }
    public static int kills(ItemStack stack) { return Math.max(0, read(stack).getInt("Kills")); }
    public static String identity(ItemStack stack) { var data = read(stack); return data.hasUUID("Identity") ? data.getUUID("Identity").toString() : "new-sword"; }
    public static boolean advanced(ItemStack stack) { return stack.is(ThirteenBlade.DRAGON_SWORD.get()); }
    public static double baseAttack(ItemStack stack) { return advanced(stack) ? 12 : 6; }
    public static int level(ItemStack stack) { int level = Progression.level(kills(stack), ThirteenBlade.balance); return advanced(stack) ? level : Math.min(10, level); }
    public static boolean has(ItemStack stack, String power) { return read(stack).getBoolean(power); }
    public static double damageBonus(ItemStack stack) { return level(stack) * ThirteenBlade.balance.damagePerLevel; }
    public static double healthBonus(ItemStack stack) { return level(stack) * ThirteenBlade.balance.healthPerLevel; }
    public static void addKill(ItemStack stack) { edit(stack, data -> data.putInt("Kills", Progression.nextKill(kills(stack)))); }
    public static boolean unlock(ItemStack stack, String power) { if (has(stack, power)) return false; edit(stack, data -> data.putBoolean(power, true)); return true; }
    public static int soulCount(ItemStack stack) { return discoveries(stack).size(); }
    public static double toughnessBonus(ItemStack stack) { return 2.0 * soulCount(stack); }

    private static Set<String> discoveries(ItemStack stack) {
        Set<String> result = new LinkedHashSet<>();
        ListTag list = read(stack).getList("SoulDiscoveries", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        for (SoulPower power : SoulPower.values()) if (power.known(stack)) result.add("family:" + power.name());
        return result;
    }
    public static boolean discover(ItemStack sword, LivingEntity victim) {
        SoulPower power = SoulPower.of(victim);
        if (power == null && victim.getType().getCategory() != MobCategory.MONSTER) return false;
        String key = power == null ? BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString() : "family:" + power.name();
        var entries = discoveries(sword);
        if (!entries.add(key)) return false;
        ListTag saved = new ListTag();
        for (String entry : entries) saved.add(StringTag.valueOf(entry));
        edit(sword, data -> data.put("SoulDiscoveries", saved));
        return true;
    }
    public static boolean stealable(MobEffect effect) { return !effect.isInstantenous() && effect.getCategory() == MobEffectCategory.BENEFICIAL; }
    public static Map<MobEffect, StoredEffect> stolenEffects(ItemStack stack, long now) {
        Map<MobEffect, StoredEffect> result = new LinkedHashMap<>();
        ListTag list = read(stack).getList("StolenEffects", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(64, list.size()); i++) {
            CompoundTag data = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(data.getString("Id"));
            MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
            long expiry = data.getLong("ExpiresAt");
            if (effect == null || !stealable(effect) || (expiry >= 0 && expiry <= now)) continue;
            int amp = Math.max(0, Math.min(ThirteenBlade.balance.maxStolenEffectLevel - 1, data.getInt("Amplifier")));
            StoredEffect old = result.get(effect);
            if (old == null || old.amplifier() < amp) result.put(effect, new StoredEffect(effect, amp, expiry));
        }
        return result;
    }
    public static boolean capture(ItemStack sword, MobEffectInstance effect, long now) {
        if (!stealable(effect.getEffect())) return false;
        var effects = stolenEffects(sword, now);
        int amp = Math.max(0, Math.min(ThirteenBlade.balance.maxStolenEffectLevel - 1, effect.getAmplifier()));
        long expiry = ThirteenBlade.balance.stolenEffectDurationSeconds < 0 ? -1 : now + ThirteenBlade.balance.stolenEffectDurationSeconds * 1000L;
        StoredEffect old = effects.get(effect.getEffect());
        if (old != null) {
            amp = Math.max(amp, old.amplifier());
            expiry = old.expiresAt() < 0 || expiry < 0 ? -1 : Math.max(old.expiresAt(), expiry);
            if (amp == old.amplifier() && expiry == old.expiresAt()) return false;
        } else if (effects.size() >= 64) return false;
        effects.put(effect.getEffect(), new StoredEffect(effect.getEffect(), amp, expiry));
        ListTag list = new ListTag();
        for (StoredEffect stored : effects.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", BuiltInRegistries.MOB_EFFECT.getKey(stored.effect()).toString());
            entry.putInt("Amplifier", stored.amplifier());
            entry.putLong("ExpiresAt", stored.expiresAt());
            list.add(entry);
        }
        edit(sword, data -> data.put("StolenEffects", list));
        return true;
    }
    public static Map<MobEffect, StoredEffect> desiredEffects(ItemStack sword, long now) {
        var effects = stolenEffects(sword, now);
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
        float saved = read(sword).contains("ShieldRemaining", Tag.TAG_ANY_NUMERIC) ? read(sword).getFloat("ShieldRemaining") : capacity;
        return Float.isFinite(saved) ? Math.max(0, Math.min(capacity, saved)) : 0;
    }
    public static void saveShield(ItemStack sword, float amount) {
        float safe = Float.isFinite(amount) ? Math.max(0, amount) : 0;
        if (!read(sword).contains("ShieldRemaining") || read(sword).getFloat("ShieldRemaining") != safe)
            edit(sword, data -> data.putFloat("ShieldRemaining", safe));
    }
    public static void setCooldown(ItemStack sword, long until) { edit(sword, data -> data.putLong("CooldownUntil", until)); }
    public static long cooldownRemaining(ItemStack sword, long now) {
        long stored = read(sword).getLong("CooldownUntil"), maximum = ThirteenBlade.balance.absorptionCooldownSeconds * 1000L;
        if (stored <= now) return 0;
        if (stored > now + maximum) { setCooldown(sword, now + maximum); return maximum; }
        return stored - now;
    }
}
