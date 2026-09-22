package dev.thirteenblade;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class BladeGameTests implements FabricGameTest {
    private static ServerPlayerEntity player(TestContext context, ItemStack sword) {
        var world = context.getWorld();
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), "BladeTest"));
        player.networkHandler = new ServerPlayNetworkHandler(world.getServer(),
                new ClientConnection(NetworkSide.SERVERBOUND), player);
        player.setStackInHand(Hand.MAIN_HAND, sword);
        BlockPos pos = context.getAbsolutePos(new BlockPos(2, 2, 2));
        player.setPosition(pos.getX(), pos.getY(), pos.getZ());
        return player;
    }

    private static ZombieEntity zombie(TestContext context) {
        ZombieEntity zombie = new ZombieEntity(EntityType.ZOMBIE, context.getWorld());
        zombie.setAiDisabled(true);
        zombie.setHealth(0.1f);
        BlockPos pos = context.getAbsolutePos(new BlockPos(3, 2, 2));
        zombie.setPosition(pos.getX(), pos.getY(), pos.getZ());
        context.getWorld().spawnEntity(zombie);
        return zombie;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void swordSurvivesHitsAndNbtRoundTrip(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        ServerPlayerEntity player = player(context, sword);
        BladeData.write(sword).putInt("Kills", 39);
        BladeData.unlock(sword, BladeData.HUNGER_WARD);
        ThirteenBlade.SWORD.inventoryTick(sword, context.getWorld(), player, 0, true);
        for (int i = 0; i < 1000; i++) sword.damage(1, player, ignored -> {});
        context.assertTrue(!sword.isEmpty() && !sword.isDamageable(), "Sword must not consume durability");
        ItemStack restored = ItemStack.fromNbt(sword.writeNbt(new NbtCompound()));
        context.assertTrue(BladeData.kills(restored) == 39 && BladeData.level(restored) == 3, "Kills must survive save/load");
        context.assertTrue(BladeData.has(restored, BladeData.HUNGER_WARD), "Power must survive save/load");
        context.assertTrue(BladeData.identity(restored).equals(BladeData.identity(sword)), "Identity must survive save/load");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void realMeleeKillsGrowExactlyAtThirteen(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        ServerPlayerEntity player = player(context, sword);
        for (int i = 0; i < 12; i++) player.attack(zombie(context));
        context.assertTrue(BladeData.kills(sword) == 12 && BladeData.level(sword) == 0, "First 12 kills must not level");
        context.assertTrue(player.getMaxHealth() == 20, "Initial health must be 20");
        player.attack(zombie(context));
        context.assertTrue(BladeData.kills(sword) == 13 && BladeData.level(sword) == 1, "Kill 13 must level once");
        context.assertTrue(player.getMaxHealth() == 22 && player.getHealth() == 20, "Level raises cap without free healing");
        for (int i = 0; i < 20; i++) BladeGameplay.refreshAttributes(player);
        context.assertTrue(player.getMaxHealth() == 22, "Repeated updates must not stack health");
        player.setHealth(22);
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        // The attack-head mixin must revoke bonuses even before the next server tick.
        player.attack(zombie(context));
        context.assertTrue(player.getMaxHealth() == 20 && player.getHealth() == 20, "Unequip must revoke and clamp health");
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == 1,
                "Unequip must immediately revoke the growth attack modifier");
        context.assertTrue(BladeData.kills(sword) == 13, "Bare-hand kills must not advance the sword");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void projectilesAndExcludedMobsDoNotCount(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        ServerPlayerEntity player = player(context, sword);
        ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, context.getWorld());
        arrow.setOwner(player);
        zombie(context).damage(player.getDamageSources().arrow(arrow, player), 100);
        context.assertTrue(BladeData.kills(sword) == 0, "Holding a sword must not convert arrow kills into growth");
        ZombieEntity baby = zombie(context);
        baby.setBaby(true);
        player.attack(baby);
        context.assertTrue(BladeData.kills(sword) == 0, "Baby mobs must not count");
        context.assertTrue(!BladeGameplay.validVictim(new VillagerEntity(EntityType.VILLAGER, context.getWorld())), "Villagers must not count");
        WolfEntity pet = new WolfEntity(EntityType.WOLF, context.getWorld());
        pet.setTamed(true);
        context.assertTrue(!BladeGameplay.validVictim(pet), "Tamed wolves must not count");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void absorptionConsumesOnlyTheNextKillAndKeepsCooldown(TestContext context) throws Exception {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        ServerPlayerEntity player = player(context, sword);
        var toggle = BladeGameplay.class.getDeclaredMethod("toggleAbsorption", ServerPlayerEntity.class);
        toggle.setAccessible(true);
        toggle.invoke(null, player);
        context.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) == 0, "Arming must not start cooldown");
        player.attack(zombie(context));
        context.assertTrue(BladeData.has(sword, BladeData.HUNGER_WARD), "Zombie soul must unlock hunger immunity");
        var skeleton = EntityType.SKELETON.create(context.getWorld());
        skeleton.setHealth(0.1f);
        player.attack(skeleton);
        context.assertTrue(!BladeData.has(sword, BladeData.NIGHT_SIGHT), "A second kill must not reuse absorption");
        context.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) > 0, "Consuming absorption must not refund cooldown");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void hungerImmunityFollowsTheEquippedSword(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        BladeData.unlock(sword, BladeData.HUNGER_WARD);
        ServerPlayerEntity player = player(context, sword);
        boolean accepted = player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200));
        context.assertTrue(!accepted && !player.hasStatusEffect(StatusEffects.HUNGER), "Mixin must reject Hunger while holding the unlocked sword");
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        accepted = player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200));
        context.assertTrue(accepted && player.hasStatusEffect(StatusEffects.HUNGER), "Hunger must work normally after unequipping");
        context.complete();
    }
}
