package dev.thirteenblade;

import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class BladeData {
    private static final String ROOT = "ThirteenBlade";
    private BladeData() {}
    public static NBTTagCompound read(ItemStack stack) { return stack.hasTagCompound() ? stack.getTagCompound().getCompoundTag(ROOT) : new NBTTagCompound(); }
    public static NBTTagCompound write(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        if (!stack.getTagCompound().hasKey(ROOT, 10)) stack.getTagCompound().setTag(ROOT, new NBTTagCompound());
        return stack.getTagCompound().getCompoundTag(ROOT);
    }
    public static void ensureIdentity(ItemStack stack) { if (!read(stack).hasUniqueId("Identity")) write(stack).setUniqueId("Identity", UUID.randomUUID()); }
    public static String identity(ItemStack stack) { return read(stack).hasUniqueId("Identity") ? read(stack).getUniqueId("Identity").toString() : "new-sword"; }
    public static int kills(ItemStack stack) { return Math.max(0, read(stack).getInteger("Kills")); }
    public static boolean advanced(ItemStack stack) { return stack.getItem() == ThirteenBlade.DRAGON_SWORD; }
    public static double baseAttack(ItemStack stack) { return advanced(stack) ? 12 : 6; }
    public static int level(ItemStack stack) { int raw = Progression.level(kills(stack), ThirteenBlade.balance); return advanced(stack) ? raw : Math.min(10, raw); }
    public static void addKill(ItemStack stack) { write(stack).setInteger("Kills", Progression.nextKill(kills(stack))); }
    public static boolean has(ItemStack stack, String flag) { return read(stack).getBoolean(flag); }
    public static boolean unlock(ItemStack stack, String flag) { boolean fresh = !has(stack, flag); write(stack).setBoolean(flag, true); return fresh; }
    public static double damageBonus(ItemStack stack) { return level(stack) * ThirteenBlade.balance.damagePerLevel; }
    public static double healthBonus(ItemStack stack) { return level(stack) * ThirteenBlade.balance.healthPerLevel; }
    private static Set<String> discoveries(ItemStack stack) {
        Set<String> found = new LinkedHashSet<>();
        NBTTagList list = read(stack).getTagList("SoulDiscoveries", 8);
        for (int i = 0; i < list.tagCount(); i++) found.add(list.getStringTagAt(i));
        for (SoulPower power : SoulPower.values()) if (power.known(stack)) found.add("family:" + power.name());
        return found;
    }
    public static int soulCount(ItemStack stack) { return discoveries(stack).size(); }
    public static double toughnessBonus(ItemStack stack) { return soulCount(stack) * 2.0; }
    public static boolean discover(ItemStack stack, EntityLivingBase victim) {
        SoulPower power = SoulPower.of(victim);
        if (power == null && !victim.isCreatureType(EnumCreatureType.MONSTER, false)) return false;
        ResourceLocation entityId = net.minecraft.entity.EntityList.getKey(victim);
        if (power == null && entityId == null) return false;
        Set<String> found = discoveries(stack);
        if (!found.add(power == null ? entityId.toString() : "family:" + power.name())) return false;
        NBTTagList list = new NBTTagList();
        for (String id : found) list.appendTag(new NBTTagString(id));
        write(stack).setTag("SoulDiscoveries", list); return true;
    }
    public static final class StoredEffect {
        public final Potion effect; public final int amplifier; public final long expiresAt;
        StoredEffect(Potion effect, int amplifier, long expiresAt) { this.effect = effect; this.amplifier = amplifier; this.expiresAt = expiresAt; }
        public int ticks(long now) { return expiresAt < 0 ? Integer.MAX_VALUE : (int)Math.max(1, Math.min(Integer.MAX_VALUE, (expiresAt - now + 49) / 50)); }
    }
    public static boolean stealable(Potion potion) { return !potion.isBadEffect() && !potion.isInstant(); }
    public static Map<Potion, StoredEffect> stolenEffects(ItemStack stack, long now) {
        Map<Potion, StoredEffect> found = new LinkedHashMap<>();
        NBTTagList list = read(stack).getTagList("StolenEffects", 10);
        for (int i = 0; i < Math.min(64, list.tagCount()); i++) {
            NBTTagCompound data = list.getCompoundTagAt(i);
            Potion potion;
            try { potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(data.getString("Id"))); }
            catch (RuntimeException invalid) { continue; }
            long expiry = data.getLong("ExpiresAt");
            if (potion == null || !stealable(potion) || (expiry >= 0 && expiry <= now)) continue;
            int amp = Math.max(0, Math.min(ThirteenBlade.balance.maxStolenEffectLevel - 1, data.getInteger("Amplifier")));
            found.put(potion, new StoredEffect(potion, amp, expiry));
        }
        return found;
    }
    public static Map<Potion, StoredEffect> desiredEffects(ItemStack stack, long now) {
        Map<Potion, StoredEffect> found = stolenEffects(stack, now);
        for (SoulPower power : SoulPower.values()) if (power.effect != null && power.known(stack)) {
            StoredEffect old = found.get(power.effect);
            found.put(power.effect, new StoredEffect(power.effect, old == null ? 0 : old.amplifier, -1));
        }
        return found;
    }
    public static boolean capture(ItemStack stack, PotionEffect effect, long now) {
        Potion potion = effect.getPotion(); if (!stealable(potion)) return false;
        Map<Potion, StoredEffect> found = stolenEffects(stack, now);
        int amp = Math.max(0, Math.min(ThirteenBlade.balance.maxStolenEffectLevel - 1, effect.getAmplifier()));
        long expiry = ThirteenBlade.balance.stolenEffectDurationSeconds < 0 ? -1 : now + 1000L * ThirteenBlade.balance.stolenEffectDurationSeconds;
        StoredEffect old = found.get(potion);
        if (old != null) {
            amp = Math.max(amp, old.amplifier);
            expiry = expiry < 0 || old.expiresAt < 0 ? -1 : Math.max(expiry, old.expiresAt);
            if (amp == old.amplifier && expiry == old.expiresAt) return false;
        } else if (found.size() >= 64) return false;
        found.put(potion, new StoredEffect(potion, amp, expiry));
        NBTTagList list = new NBTTagList();
        for (StoredEffect stored : found.values()) {
            NBTTagCompound data = new NBTTagCompound(); data.setString("Id", stored.effect.getRegistryName().toString());
            data.setInteger("Amplifier", stored.amplifier); data.setLong("ExpiresAt", stored.expiresAt); list.appendTag(data);
        }
        write(stack).setTag("StolenEffects", list); return true;
    }
    public static long cooldownRemaining(ItemStack stack, long now) {
        long remaining = read(stack).getLong("CooldownUntil") - now, maximum = ThirteenBlade.balance.absorptionCooldownSeconds * 1000L;
        if (remaining > maximum) { write(stack).setLong("CooldownUntil", now + maximum); return maximum; }
        return Math.max(0, remaining);
    }
    public static float shield(ItemStack stack, int amp) {
        float value = read(stack).hasKey("ShieldRemaining") ? read(stack).getFloat("ShieldRemaining") : 4 * (amp + 1);
        return Float.isFinite(value) ? Math.max(0, Math.min(4 * (amp + 1), value)) : 0;
    }
    public static void saveShield(ItemStack stack, float value) { write(stack).setFloat("ShieldRemaining", Float.isFinite(value) ? Math.max(0, value) : 0); }
}
