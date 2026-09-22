package dev.thirteenblade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BladeGameplay {
    public static final Identifier ABSORB = ThirteenBlade.id("absorb");
    public static final Identifier STATUS = ThirteenBlade.id("status");
    public static final Identifier BALANCE = ThirteenBlade.id("balance");
    private static final UUID DAMAGE_ID = UUID.fromString("aa53b403-6899-4810-a5e0-ebd6a573d6da");
    private static final UUID HEALTH_ID = UUID.fromString("8c7594bb-a3ad-451e-aa51-d63a07f0d96a");
    private static final Map<UUID, Armed> ARMED = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();

    private record Armed(ItemStack sword, long expiresAt) {}
    private BladeGameplay() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(BladeGameplay::onDeath);
        ServerPlayNetworking.registerGlobalReceiver(ABSORB, (server, player, handler, buf, sender) ->
                server.execute(() -> toggleAbsorption(player)));
        ServerTickEvents.END_SERVER_TICK.register(BladeGameplay::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendBalance(handler.player);
            sendStatus(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BladeEffects.release(handler.player);
            ARMED.remove(handler.player.getUuid());
            LAST_REQUEST.remove(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ThirteenBlade.balance = ThirteenBlade.localBalance);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> server.getPlayerManager().getPlayerList().forEach(BladeEffects::release));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ARMED.clear();
            LAST_REQUEST.clear();
            BladeEffects.clear();
        });
    }

    public static boolean validVictim(LivingEntity victim) {
        return victim instanceof MobEntity mob && !mob.isBaby()
                && !(mob instanceof MerchantEntity)
                && !(mob instanceof TameableEntity pet && pet.isTamed());
    }

    private static void onDeath(LivingEntity victim, DamageSource source) {
        if (victim instanceof ServerPlayerEntity deadPlayer) BladeEffects.release(deadPlayer);
        if (!source.isOf(DamageTypes.PLAYER_ATTACK)
                || !(source.getAttacker() instanceof ServerPlayerEntity player)
                || source.getSource() != player || !validVictim(victim)) return;
        ItemStack sword = BladeInventory.activeSword(player);
        if (!sword.isOf(ThirteenBlade.SWORD)) return;

        int previous = BladeData.level(sword);
        BladeData.addKill(sword);
        refreshAttributes(player);
        if (BladeData.level(sword) > previous) {
            player.sendMessage(Text.translatable("message.thirteenblade.level", BladeData.level(sword)), false);
            player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.65f, 1.3f);
            player.getServerWorld().spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1,
                    player.getZ(), 24, 0.5, 0.6, 0.5, 0.2);
        }

        Armed armed = ARMED.get(player.getUuid());
        if (armed != null && armed.sword == sword && armed.expiresAt > worldTime(player)) {
            // Consume before awarding: sweeping attacks cannot absorb multiple targets.
            ARMED.remove(player.getUuid());
            String power = victim instanceof ZombieEntity ? BladeData.HUNGER_WARD
                    : victim instanceof AbstractSkeletonEntity ? BladeData.NIGHT_SIGHT
                    : victim instanceof SpiderEntity ? BladeData.SLOW_FALL
                    : victim instanceof CreeperEntity ? BladeData.CREEPER_SHIELD : null;
            boolean changed = power != null && BladeData.unlock(sword, power);
            if (changed) {
                String name = power.equals(BladeData.HUNGER_WARD) ? "power.thirteenblade.hunger"
                        : power.equals(BladeData.NIGHT_SIGHT) ? "power.thirteenblade.night"
                        : power.equals(BladeData.SLOW_FALL) ? "power.thirteenblade.slow_fall" : "power.thirteenblade.shield";
                player.sendMessage(Text.translatable("message.thirteenblade.absorbed", Text.translatable(name)), false);
            }
            boolean hadBuff = false;
            boolean refillShield = victim instanceof CreeperEntity;
            for (StatusEffectInstance effect : victim.getStatusEffects()) {
                if (!BladeData.stealable(effect.getEffectType())) continue;
                hadBuff = true;
                if (effect.getEffectType() == StatusEffects.ABSORPTION) refillShield = true;
                if (BladeData.capture(sword, effect, System.currentTimeMillis())) {
                    changed = true;
                    player.sendMessage(Text.translatable("message.thirteenblade.stolen", effect.getEffectType().getName(),
                            Math.min(effect.getAmplifier() + 1, ThirteenBlade.balance.maxStolenEffectLevel)), false);
                }
            }
            BladeEffects.refresh(player);
            if (refillShield) {
                BladeEffects.replenishShield(player, sword);
                player.sendMessage(Text.translatable("message.thirteenblade.shield_refilled"), false);
            }
            if (power == null && !hadBuff) {
                player.sendMessage(Text.translatable("message.thirteenblade.no_power"), false);
            } else if (!changed && !refillShield) {
                player.sendMessage(Text.translatable("message.thirteenblade.known_power"), false);
            }
            if (changed || refillShield) {
                player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1, 0.8f);
                player.getServerWorld().spawnParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 0.6,
                        victim.getZ(), 18, 0.3, 0.4, 0.3, 0.04);
            }
        }
        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
        sendStatus(player);
    }

    private static long worldTime(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    private static void toggleAbsorption(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator()) return;
        long tick = worldTime(player);
        Long last = LAST_REQUEST.put(player.getUuid(), tick);
        if (last != null && tick - last < 5) return;
        if (ARMED.remove(player.getUuid()) != null) {
            player.sendMessage(Text.translatable("message.thirteenblade.cancelled"), true);
            sendStatus(player);
            return;
        }
        ItemStack sword = BladeInventory.activeSword(player);
        if (!sword.isOf(ThirteenBlade.SWORD)) {
            player.sendMessage(Text.translatable("message.thirteenblade.hold"), true);
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = BladeData.cooldownRemaining(sword, now);
        if (cooldown > 0) {
            player.sendMessage(Text.translatable("message.thirteenblade.cooldown", (cooldown + 999) / 1000), true);
            return;
        }
        BalanceConfig config = ThirteenBlade.balance;
        BladeData.write(sword).putLong("CooldownUntil", now + config.absorptionCooldownSeconds * 1000L);
        ARMED.put(player.getUuid(), new Armed(sword, tick + config.absorptionWindowSeconds * 20L));
        player.sendMessage(Text.translatable("message.thirteenblade.armed", config.absorptionWindowSeconds), true);
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.1f);
        sendStatus(player);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            refreshAttributes(player);
            BladeEffects.refresh(player);
            ItemStack sword = BladeInventory.activeSword(player);
            Armed armed = ARMED.get(player.getUuid());
            if (armed != null && (!player.isAlive() || player.isSpectator() || sword != armed.sword
                    || worldTime(player) >= armed.expiresAt)) {
                ARMED.remove(player.getUuid());
                player.sendMessage(Text.translatable("message.thirteenblade.ended"), true);
                sendStatus(player);
            }
            if (server.getTicks() % 20 == 0) sendStatus(player);
        }
    }

    public static void refreshAttributes(ServerPlayerEntity player) {
        ItemStack sword = BladeInventory.activeSword(player);
        boolean active = player.isAlive() && !player.isSpectator() && sword.isOf(ThirteenBlade.SWORD);
        setModifier(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), DAMAGE_ID,
                "Thirteen Blade growth", active ? BladeData.damageBonus(sword) : 0);
        setModifier(player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH), HEALTH_ID,
                "Thirteen Blade vitality", player.isAlive() && !player.isSpectator() ? BladeInventory.healthBonus(player) : 0);
        // Never heal on equip: switching swords cannot be used as a healing exploit.
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(EntityAttributeInstance attribute, UUID id, String name, double value) {
        if (attribute == null) return;
        EntityAttributeModifier existing = attribute.getModifier(id);
        if (existing != null && existing.getValue() == value) return;
        if (existing != null) attribute.removeModifier(id);
        if (value != 0) attribute.addTemporaryModifier(new EntityAttributeModifier(id, name, value,
                EntityAttributeModifier.Operation.ADDITION));
    }

    private static void sendStatus(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, STATUS)) return;
        Armed armed = ARMED.get(player.getUuid());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(armed == null ? 0 : (int) Math.max(0, (armed.expiresAt - worldTime(player) + 19) / 20));
        ItemStack sword = BladeInventory.activeSword(player);
        buf.writeVarInt(sword.isOf(ThirteenBlade.SWORD)
                ? (int) ((BladeData.cooldownRemaining(sword, System.currentTimeMillis()) + 999) / 1000) : 0);
        ServerPlayNetworking.send(player, STATUS, buf);
    }

    private static void sendBalance(ServerPlayerEntity player) {
        BalanceConfig config = ThirteenBlade.balance;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(config.killsPerLevel);
        buf.writeDouble(config.damagePerLevel).writeDouble(config.healthPerLevel);
        buf.writeVarInt(config.absorptionWindowSeconds).writeVarInt(config.absorptionCooldownSeconds);
        buf.writeDouble(config.eliteSpawnChance).writeDouble(config.eliteHealthMultiplier);
        buf.writeVarInt(config.eliteMaxEffects).writeVarInt(config.eliteMaxEffectLevel);
        buf.writeVarInt(config.maxStolenEffectLevel).writeInt(config.stolenEffectDurationSeconds);
        ServerPlayNetworking.send(player, BALANCE, buf);
    }
}
