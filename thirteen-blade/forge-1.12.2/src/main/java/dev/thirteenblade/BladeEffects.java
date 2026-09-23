package dev.thirteenblade;

import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.potion.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Forge events preserve ordinary potions without requiring a coremod or Mixin on 1.12.2. */
public final class BladeEffects {
    private static final String SAVED = "thirteenblade.effects";
    private static final Map<UUID, Map<Potion, Grant>> GRANTS = new HashMap<>();
    private static boolean applying;
    private static final class Grant {
        final ItemStack sword; final long expiresAt;
        long leaveUntil = -1; PotionEffect instance;
        Grant(ItemStack sword, long expiresAt) { this.sword = sword; this.expiresAt = expiresAt; }
    }
    private static void persist(EntityPlayerMP player) {
        NBTTagList list = new NBTTagList(); Map<Potion, Grant> grants = GRANTS.get(player.getUniqueID());
        if (grants != null) for (Potion type : grants.keySet()) list.appendTag(new NBTTagString(type.getRegistryName().toString()));
        player.getEntityData().setTag(SAVED, list);
    }
    public static void cleanSaved(EntityPlayerMP player) {
        NBTTagList list = player.getEntityData().getTagList(SAVED, 8);
        for (int i = 0; i < list.tagCount(); i++) {
            try { Potion type = ForgeRegistries.POTIONS.getValue(new ResourceLocation(list.getStringTagAt(i))); if (type != null) player.removePotionEffect(type); }
            catch (RuntimeException invalid) { /* Ignore invalid saved registry IDs. */ }
        }
        player.getEntityData().removeTag(SAVED); DragonFlight.release(player);
    }
    private static Grant owned(EntityPlayerMP p, Potion type) {
        Map<Potion, Grant> grants = GRANTS.get(p.getUniqueID()); Grant grant = grants == null ? null : grants.get(type);
        return grant != null && grant.instance == p.getActivePotionEffect(type) ? grant : null;
    }
    private static void saveShield(EntityPlayerMP player, Potion type, Grant grant) {
        if (type == MobEffects.ABSORPTION && grant != null) BladeData.saveShield(grant.sword, player.getAbsorptionAmount());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public void external(PotionEvent.PotionApplicableEvent event) {
        if (applying || event.getResult() == Event.Result.DENY || !(event.getEntityLiving() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP)event.getEntityLiving(); Potion type = event.getPotionEffect().getPotion();
        Grant grant = owned(player, type);
        if (grant != null) {
            saveShield(player, type, grant);
            // Remove the enormous sword duration before vanilla combines the real potion.
            player.removePotionEffect(type);
            GRANTS.get(player.getUniqueID()).remove(type); persist(player);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public void removed(PotionEvent.PotionRemoveEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            EntityPlayerMP p = (EntityPlayerMP)event.getEntityLiving(); saveShield(p, event.getPotion(), owned(p, event.getPotion()));
        }
    }
    @SubscribeEvent public void applicable(PotionEvent.PotionApplicableEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP && event.getPotionEffect().getPotion() == MobEffects.HUNGER
                && SoulPower.ZOMBIE.known(BladeInventory.activeSword((EntityPlayerMP)event.getEntityLiving()))) event.setResult(Event.Result.DENY);
    }
    public static void release(EntityPlayerMP player) {
        Map<Potion, Grant> grants = GRANTS.get(player.getUniqueID());
        if (grants != null) for (Potion type : new ArrayList<>(grants.keySet())) if (owned(player, type) != null) player.removePotionEffect(type);
        GRANTS.remove(player.getUniqueID()); player.getEntityData().removeTag(SAVED); DragonFlight.release(player);
    }
    public static void clear() { GRANTS.clear(); DragonFlight.clear(); }
    public static void refresh(EntityPlayerMP player) {
        if (player.connection == null) return;
        if (!player.isEntityAlive() || player.isSpectator()) { release(player); return; }
        DragonFlight.refresh(player);
        ItemStack sword = BladeInventory.activeSword(player); long now = System.currentTimeMillis(), tick = BladeGameplay.tick(player);
        Map<Potion, BladeData.StoredEffect> desired = sword.isEmpty() ? Collections.emptyMap() : BladeData.desiredEffects(sword, now);
        Map<Potion, Grant> grants = GRANTS.computeIfAbsent(player.getUniqueID(), key -> new HashMap<>());
        grants.entrySet().removeIf(entry -> entry.getValue().instance != player.getActivePotionEffect(entry.getKey()));
        for (Map.Entry<Potion, Grant> entry : new ArrayList<>(grants.entrySet())) {
            Potion type = entry.getKey(); Grant grant = entry.getValue(); PotionEffect effect = grant.instance;
            BladeData.StoredEffect target = desired.get(type);
            if (grant.sword == sword && target != null && grant.leaveUntil < 0 && target.amplifier == effect.getAmplifier() && target.expiresAt == grant.expiresAt) continue;
            if (target != null || grant.sword == sword || grant.expiresAt >= 0 && grant.expiresAt <= now || grant.leaveUntil >= 0 && tick >= grant.leaveUntil) {
                player.removePotionEffect(type); grants.remove(type); continue;
            }
            if (grant.leaveUntil < 0) {
                int duration = Math.min(100, effect.getDuration()); grant.leaveUntil = tick + duration;
                player.removePotionEffect(type); apply(player, type, grant, effect.getAmplifier(), duration);
            }
        }
        if (SoulPower.ZOMBIE.known(sword)) player.removePotionEffect(MobEffects.HUNGER);
        for (BladeData.StoredEffect target : desired.values()) {
            if (player.isPotionActive(target.effect)) continue;
            Grant grant = new Grant(sword, target.expiresAt); grants.put(target.effect, grant);
            if (!apply(player, target.effect, grant, target.amplifier, target.ticks(now))) grants.remove(target.effect);
        }
        saveShield(player, MobEffects.ABSORPTION, owned(player, MobEffects.ABSORPTION)); persist(player);
        if (grants.isEmpty()) GRANTS.remove(player.getUniqueID());
    }
    private static boolean apply(EntityPlayerMP player, Potion type, Grant grant, int amplifier, int duration) {
        PotionEffect effect = new PotionEffect(type, duration, amplifier, true, false);
        applying = true;
        try { player.addPotionEffect(effect); } finally { applying = false; }
        grant.instance = player.getActivePotionEffect(type);
        if (grant.instance != effect) return false;
        if (type == MobEffects.ABSORPTION) player.setAbsorptionAmount(BladeData.shield(grant.sword, amplifier));
        return true;
    }
    public static void replenishShield(EntityPlayerMP player, ItemStack sword) {
        BladeData.StoredEffect target = BladeData.desiredEffects(sword, System.currentTimeMillis()).get(MobEffects.ABSORPTION);
        if (target == null) return;
        float capacity = 4 * (target.amplifier + 1); Grant grant = owned(player, MobEffects.ABSORPTION);
        if (grant != null && grant.sword == sword) player.setAbsorptionAmount(capacity);
        BladeData.saveShield(sword, capacity);
    }
}
