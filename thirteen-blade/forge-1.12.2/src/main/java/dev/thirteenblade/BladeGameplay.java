package dev.thirteenblade;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.*;

public final class BladeGameplay {
    private static final UUID DAMAGE = UUID.fromString("aa53b403-6899-4810-a5e0-ebd6a573d6da"), HEALTH = UUID.fromString("8c7594bb-a3ad-451e-aa51-d63a07f0d96a"), TOUGHNESS = UUID.fromString("d1830e61-bba7-4a88-8c7f-476e6cfe9bf6");
    private static final Map<UUID, ItemStack> ARMED = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    public static void raiseToughnessLimit() {
        try {
            java.lang.reflect.Field field = ObfuscationReflectionHelper.findField(RangedAttribute.class, "field_111118_b");
            field.setDouble(SharedMonsterAttributes.ARMOR_TOUGHNESS, Math.max(1024, field.getDouble(SharedMonsterAttributes.ARMOR_TOUGHNESS)));
        } catch (ReflectiveOperationException e) { ThirteenBlade.LOGGER.warn("Could not raise the vanilla toughness limit", e); }
    }
    public static boolean validVictim(EntityLivingBase victim) {
        return victim instanceof EntityLiving && !victim.isChild() && !(victim instanceof EntityVillager)
                && !(victim instanceof EntityTameable && ((EntityTameable)victim).isTamed());
    }
    public static long tick(EntityPlayerMP player) { return player.getServer().getWorld(0).getTotalWorldTime(); }
    public static void refreshAttributes(EntityPlayerMP player) {
        ItemStack sword = BladeInventory.activeSword(player); BladeEnchantments.ensure(sword);
        boolean alive = player.isEntityAlive() && !player.isSpectator(), active = alive && ThirteenBlade.isSword(sword);
        modifier(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE), DAMAGE, "Thirteen Blade growth", active ? BladeData.damageBonus(sword) : 0);
        boolean healthChanged = modifier(player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH), HEALTH, "Thirteen Blade vitality", alive ? BladeInventory.healthBonus(player) : 0);
        modifier(player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS), TOUGHNESS, "Thirteen Blade soul toughness", active ? BladeData.toughnessBonus(sword) : 0);
        if (healthChanged) FirstAidBridge.rescale(player);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
    private static boolean modifier(IAttributeInstance attribute, UUID id, String name, double value) {
        if (attribute == null) return false;
        AttributeModifier old = attribute.getModifier(id);
        if (old == null && value == 0 || old != null && old.getAmount() == value) return false;
        if (old != null) attribute.removeModifier(old);
        if (value != 0) attribute.applyModifier(new AttributeModifier(id, name, value, 0).setSaved(false));
        return true;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void attack(AttackEntityEvent event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) refreshAttributes((EntityPlayerMP)event.getEntityPlayer());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public void damage(LivingDamageEvent event) {
        EntityPlayerMP player = attacker(event.getSource());
        if (player != null && event.getAmount() > 0 && SoulPower.VINDICATOR.known(BladeInventory.activeSword(player)))
            event.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100));
    }
    private static EntityPlayerMP attacker(DamageSource source) {
        return "player".equals(source.getDamageType()) && source.getTrueSource() instanceof EntityPlayerMP && source.getImmediateSource() == source.getTrueSource()
                ? (EntityPlayerMP)source.getTrueSource() : null;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public void death(LivingDeathEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim instanceof EntityPlayerMP) { BladeEffects.release((EntityPlayerMP)victim); ARMED.remove(victim.getUniqueID()); }
        EntityPlayerMP player = attacker(event.getSource());
        if (player == null || !validVictim(victim)) return;
        ItemStack sword = BladeInventory.activeSword(player);
        if (!ThirteenBlade.isSword(sword)) return;
        int previous = BladeData.level(sword); BladeData.addKill(sword); refreshAttributes(player);
        if (BladeData.level(sword) > previous) {
            player.setHealth(player.getMaxHealth()); FirstAidBridge.healFully(player);
            message(player, "message.thirteenblade.level", BladeData.level(sword));
            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.util.SoundCategory.PLAYERS, .65f, 1.3f);
        }
        if (ARMED.get(player.getUniqueID()) == sword) {
            ARMED.remove(player.getUniqueID());
            BladeData.write(sword).setLong("CooldownUntil", System.currentTimeMillis() + ThirteenBlade.balance.absorptionCooldownSeconds * 1000L);
            SoulPower power = SoulPower.of(victim);
            boolean discovered = BladeData.discover(sword, victim), changed = power != null && !power.known(sword), hadBuff = false;
            if (power != null) BladeData.unlock(sword, power.flag);
            if (discovered) message(player, "message.thirteenblade.discovery", victim.getDisplayName());
            if (changed) message(player, "message.thirteenblade.absorbed", new TextComponentTranslation(power.translation));
            boolean refill = victim instanceof EntityCreeper;
            for (PotionEffect effect : new ArrayList<>(victim.getActivePotionEffects())) {
                if (!BladeData.stealable(effect.getPotion())) continue;
                hadBuff = true; refill |= effect.getPotion() == MobEffects.ABSORPTION;
                if (BladeData.capture(sword, effect, System.currentTimeMillis())) {
                    changed = true; message(player, "message.thirteenblade.stolen", new TextComponentTranslation(effect.getEffectName()), Math.min(effect.getAmplifier() + 1, ThirteenBlade.balance.maxStolenEffectLevel));
                }
            }
            refreshAttributes(player); BladeEffects.refresh(player);
            if (refill) { BladeEffects.replenishShield(player, sword); message(player, "message.thirteenblade.shield_refilled"); }
            if (power == null && !hadBuff && !discovered) message(player, "message.thirteenblade.no_power");
            else if (!changed && !refill && !discovered) message(player, "message.thirteenblade.known_power");
        }
        player.inventory.markDirty(); player.inventoryContainer.detectAndSendChanges(); sendStatus(player);
    }
    public static void toggleAbsorption(EntityPlayerMP player) {
        if (!player.isEntityAlive() || player.isSpectator()) return;
        long now = tick(player); Long last = LAST_REQUEST.put(player.getUniqueID(), now);
        if (last != null && now - last < 5) return;
        if (ARMED.remove(player.getUniqueID()) != null) { message(player, "message.thirteenblade.cancelled"); sendStatus(player); return; }
        ItemStack sword = BladeInventory.activeSword(player);
        if (!ThirteenBlade.isSword(sword)) { message(player, "message.thirteenblade.hold"); return; }
        long cooldown = BladeData.cooldownRemaining(sword, System.currentTimeMillis());
        if (cooldown > 0) { message(player, "message.thirteenblade.cooldown", (cooldown + 999) / 1000); return; }
        ARMED.put(player.getUniqueID(), sword); message(player, "message.thirteenblade.armed"); sendStatus(player);
    }
    @SubscribeEvent public void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP)event.player;
        refreshAttributes(player); BladeEffects.refresh(player);
        ItemStack armed = ARMED.get(player.getUniqueID());
        if (armed != null && (!player.isEntityAlive() || player.isSpectator() || armed != BladeInventory.activeSword(player))) {
            ARMED.remove(player.getUniqueID()); message(player, "message.thirteenblade.ended"); sendStatus(player);
        }
        if (player.ticksExisted % 20 == 0) sendStatus(player);
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) { EntityPlayerMP p = (EntityPlayerMP)event.player; BladeEffects.cleanSaved(p); BladeNetwork.balance(p); sendStatus(p); }
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) BladeEffects.release((EntityPlayerMP)event.player);
        ARMED.remove(event.player.getUniqueID()); LAST_REQUEST.remove(event.player.getUniqueID());
    }
    public static void stop() {
        net.minecraft.server.MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) for (EntityPlayerMP p : server.getPlayerList().getPlayers()) BladeEffects.release(p);
        ARMED.clear(); LAST_REQUEST.clear(); BladeEffects.clear();
    }
    private static void sendStatus(EntityPlayerMP p) {
        ItemStack sword = BladeInventory.activeSword(p);
        BladeNetwork.status(p, ARMED.containsKey(p.getUniqueID()), sword.isEmpty() ? 0 : (int)((BladeData.cooldownRemaining(sword, System.currentTimeMillis()) + 999) / 1000));
    }
    private static void message(EntityPlayerMP p, String key, Object... args) { p.sendMessage(new TextComponentTranslation(key, args)); }
}
