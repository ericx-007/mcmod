package dev.thirteenblade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class BladeGameplay {
    private static final ResourceLocation DAMAGE_ID = ThirteenBlade.id("damage_id");
    private static final ResourceLocation HEALTH_ID = ThirteenBlade.id("health_id");
    private static final ResourceLocation TOUGHNESS_ID = ThirteenBlade.id("toughness_id");
    private static final Map<UUID, Armed> ARMED = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();

    private record Armed(ItemStack sword) {}
    private BladeGameplay() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> onDeath(event.getEntity(), event.getSource()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) { BladeNetwork.sendBalance(player); sendStatus(player); }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                BladeEffects.release(player); ARMED.remove(player.getUUID()); LAST_REQUEST.remove(player.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> ThirteenBlade.balance = ThirteenBlade.localBalance);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> event.getServer().getPlayerList().getPlayers().forEach(BladeEffects::release));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> { ARMED.clear(); LAST_REQUEST.clear(); BladeEffects.clear(); });
    }

    public static boolean validVictim(LivingEntity victim) {
        return victim instanceof Mob mob && !mob.isBaby()
                && !(mob instanceof AbstractVillager)
                && !(mob instanceof TamableAnimal pet && pet.isTame());
    }

    private static void onDeath(LivingEntity victim, DamageSource source) {
        if (victim instanceof ServerPlayer deadPlayer) BladeEffects.release(deadPlayer);
        if (!source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() != player || !validVictim(victim)) return;
        ItemStack sword = BladeInventory.activeSword(player);
        if (!ThirteenBlade.isSword(sword)) return;

        int previous = BladeData.level(sword);
        BladeData.addKill(sword);
        refreshAttributes(player);
        if (BladeData.level(sword) > previous) {
            player.displayClientMessage(Component.translatable("message.thirteenblade.level", BladeData.level(sword)), false);
            player.playSound(SoundEvents.PLAYER_LEVELUP, 0.65f, 1.3f);
            player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1,
                    player.getZ(), 24, 0.5, 0.6, 0.5, 0.2);
        }

        Armed armed = ARMED.get(player.getUUID());
        if (armed != null && armed.sword == sword) {
            // Consume before awarding: sweeping attacks cannot absorb multiple targets.
            ARMED.remove(player.getUUID());
            BladeData.setCooldown(sword, System.currentTimeMillis()
                    + ThirteenBlade.balance.absorptionCooldownSeconds * 1000L);
            SoulPower power = SoulPower.of(victim);
            boolean discovered = BladeData.discover(sword, victim);
            boolean changed = power != null && !power.known(sword);
            if (power != null) BladeData.unlock(sword, power.flag);
            if (discovered) player.displayClientMessage(Component.translatable("message.thirteenblade.discovery", victim.getType().getDescription()), false);
            refreshAttributes(player);
            if (changed) {
                player.displayClientMessage(Component.translatable("message.thirteenblade.absorbed", Component.translatable(power.translation)), false);
                refreshAttributes(player);
            }
            boolean hadBuff = false;
            boolean refillShield = victim instanceof Creeper;
            for (MobEffectInstance effect : victim.getActiveEffects()) {
                if (!BladeData.stealable(effect.getEffect())) continue;
                hadBuff = true;
                if (effect.getEffect() == MobEffects.ABSORPTION) refillShield = true;
                if (BladeData.capture(sword, effect, System.currentTimeMillis())) {
                    changed = true;
                    player.displayClientMessage(Component.translatable("message.thirteenblade.stolen", effect.getEffect().value().getDisplayName(),
                            Math.min(effect.getAmplifier() + 1, ThirteenBlade.balance.maxStolenEffectLevel)), false);
                }
            }
            BladeEffects.refresh(player);
            if (refillShield) {
                BladeEffects.replenishShield(player, sword);
                player.displayClientMessage(Component.translatable("message.thirteenblade.shield_refilled"), false);
            }
            if (power == null && !hadBuff && !discovered) {
                player.displayClientMessage(Component.translatable("message.thirteenblade.no_power"), false);
            } else if (!changed && !refillShield && !discovered) {
                player.displayClientMessage(Component.translatable("message.thirteenblade.known_power"), false);
            }
            if (changed || refillShield || discovered) {
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1, 0.8f);
                player.serverLevel().sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 0.6,
                        victim.getZ(), 18, 0.3, 0.4, 0.3, 0.04);
            }
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        sendStatus(player);
    }

    private static long worldTime(ServerPlayer player) {
        return player.getServer().overworld().getGameTime();
    }

    public static void toggleAbsorption(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        long tick = worldTime(player);
        Long last = LAST_REQUEST.put(player.getUUID(), tick);
        if (last != null && tick - last < 5) return;
        if (ARMED.remove(player.getUUID()) != null) {
            player.displayClientMessage(Component.translatable("message.thirteenblade.cancelled"), true);
            sendStatus(player);
            return;
        }
        ItemStack sword = BladeInventory.activeSword(player);
        if (!ThirteenBlade.isSword(sword)) {
            player.displayClientMessage(Component.translatable("message.thirteenblade.hold"), true);
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = BladeData.cooldownRemaining(sword, now);
        if (cooldown > 0) {
            player.displayClientMessage(Component.translatable("message.thirteenblade.cooldown", (cooldown + 999) / 1000), true);
            return;
        }
        ARMED.put(player.getUUID(), new Armed(sword));
        player.displayClientMessage(Component.translatable("message.thirteenblade.armed"), true);
        player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 1.1f);
        sendStatus(player);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshAttributes(player);
            BladeEffects.refresh(player);
            ItemStack sword = BladeInventory.activeSword(player);
            Armed armed = ARMED.get(player.getUUID());
            if (armed != null && (!player.isAlive() || player.isSpectator() || sword != armed.sword)) {
                ARMED.remove(player.getUUID());
                player.displayClientMessage(Component.translatable("message.thirteenblade.ended"), true);
                sendStatus(player);
            }
            if (server.getTickCount() % 20 == 0) sendStatus(player);
        }
    }

    public static void refreshAttributes(ServerPlayer player) {
        ItemStack sword = BladeInventory.activeSword(player);
        boolean active = player.isAlive() && !player.isSpectator() && ThirteenBlade.isSword(sword);
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID,
                "Thirteen Blade growth", active ? BladeData.damageBonus(sword) : 0);
        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID,
                "Thirteen Blade vitality", player.isAlive() && !player.isSpectator() ? BladeInventory.healthBonus(player) : 0);
        setModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID,
                "Thirteen Blade soul toughness", active ? BladeData.toughnessBonus(sword) : 0);
        // Never heal on equip: switching swords cannot be used as a healing exploit.
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(AttributeInstance attribute, ResourceLocation id, String name, double value) {
        if (attribute == null) return;
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && existing.amount() == value) return;
        if (existing != null) attribute.removeModifier(id);
        if (value != 0) attribute.addTransientModifier(new AttributeModifier(id, value,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static void sendStatus(ServerPlayer player) {
        ItemStack sword = BladeInventory.activeSword(player);
        BladeNetwork.sendStatus(player, ARMED.containsKey(player.getUUID()), ThirteenBlade.isSword(sword)
                ? (int) ((BladeData.cooldownRemaining(sword, System.currentTimeMillis()) + 999) / 1000) : 0);
    }
}
