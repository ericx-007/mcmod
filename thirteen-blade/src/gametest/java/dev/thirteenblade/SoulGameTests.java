package dev.thirteenblade;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
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

public final class SoulGameTests implements FabricGameTest {
    static ServerPlayerEntity player(TestContext context, ItemStack sword) {
        var world = context.getWorld();
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), "SoulTest"));
        player.networkHandler = new ServerPlayNetworkHandler(world.getServer(), new ClientConnection(NetworkSide.SERVERBOUND), player);
        // A newly constructed server player normally has a login damage grace period.
        try {
            var grace = ServerPlayerEntity.class.getDeclaredField("joinInvulnerabilityTicks");
            grace.setAccessible(true);
            grace.setInt(player, 0);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        player.setStackInHand(Hand.MAIN_HAND, sword);
        return player;
    }

    static void arm(ServerPlayerEntity player) throws Exception {
        var toggle = BladeGameplay.class.getDeclaredMethod("toggleAbsorption", ServerPlayerEntity.class);
        toggle.setAccessible(true);
        toggle.invoke(null, player);
    }

    static void kill(TestContext context, ServerPlayerEntity player, MobEntity mob) {
        BlockPos pos = context.getAbsolutePos(new BlockPos(3, 2, 2));
        mob.setPosition(pos.getX(), pos.getY(), pos.getZ());
        context.getWorld().spawnEntity(mob);
        mob.damage(player.getDamageSources().playerAttack(player), 10000);
    }

    private static MobEntity spawn(TestContext context, SpawnReason reason) {
        var world = context.getWorld();
        var mob = EntityType.CREEPER.create(world);
        BlockPos pos = context.getAbsolutePos(new BlockPos(3, 2, 2));
        mob.setPosition(pos.getX(), pos.getY(), pos.getZ());
        mob.initialize(world, world.getLocalDifficulty(pos), reason, null, null);
        world.spawnEntity(mob);
        return mob;
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void naturalSpawnBecomesEliteOnceAndKeepsAttributesAfterReload(TestContext context) {
        double oldChance = ThirteenBlade.balance.eliteSpawnChance;
        ThirteenBlade.balance.eliteSpawnChance = 1;
        try {
            MobEntity elite = spawn(context, SpawnReason.NATURAL);
            context.assertTrue(elite.getCommandTags().contains(EliteMobs.ELITE_TAG), "A 100% natural spawn must become elite");
            context.assertTrue(elite.getMaxHealth() == 30 && elite.getHealth() == 30, "Default elite has 1.5x full health");
            context.assertTrue(elite.isGlowing(), "Elite must be identifiable");
            context.assertTrue(elite.getStatusEffects().size() >= 1 && elite.getStatusEffects().size() <= 2, "Elite needs 1-2 distinct buffs");
            context.assertTrue(elite.getStatusEffects().stream().allMatch(StatusEffectInstance::isInfinite), "Elite buffs must not expire");
            NbtCompound saved = new NbtCompound();
            elite.writeNbt(saved);
            MobEntity restored = EntityType.CREEPER.create(context.getWorld());
            restored.readNbt(saved);
            ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(restored, context.getWorld());
            ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(restored, context.getWorld());
            context.assertTrue(restored.getMaxHealth() == 30, "Chunk reload must not stack elite health");
            context.assertTrue(restored.getStatusEffects().size() == elite.getStatusEffects().size(), "Saved buffs must not reroll");
        } finally { ThirteenBlade.balance.eliteSpawnChance = oldChance; }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void spawnRestrictionsAndZeroChanceAreRespected(TestContext context) {
        double oldChance = ThirteenBlade.balance.eliteSpawnChance;
        try {
            ThirteenBlade.balance.eliteSpawnChance = 1;
            for (SpawnReason reason : new SpawnReason[]{SpawnReason.SPAWNER, SpawnReason.SPAWN_EGG, SpawnReason.COMMAND})
                context.assertTrue(!spawn(context, reason).getCommandTags().contains(EliteMobs.ELITE_TAG), "Artificial spawns must not become elites");
            ThirteenBlade.balance.eliteSpawnChance = 0;
            context.assertTrue(!spawn(context, SpawnReason.NATURAL).getCommandTags().contains(EliteMobs.ELITE_TAG), "Zero chance must disable elites");
            context.assertTrue(!EliteMobs.eligible(EntityType.WARDEN.create(context.getWorld())), "Wardens must be excluded");
            context.assertTrue(!EliteMobs.eligible(EntityType.WITHER.create(context.getWorld())), "Bosses must be excluded");
            var baby = EntityType.ZOMBIE.create(context.getWorld());
            baby.setBaby(true);
            context.assertTrue(!EliteMobs.eligible(baby), "Baby monsters must be excluded");
        } finally { ThirteenBlade.balance.eliteSpawnChance = oldChance; }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void undeadElitesAlwaysReceiveUsableBuffs(TestContext context) {
        double oldChance = ThirteenBlade.balance.eliteSpawnChance;
        ThirteenBlade.balance.eliteSpawnChance = 1;
        try {
            var world = context.getWorld();
            BlockPos pos = context.getAbsolutePos(new BlockPos(3, 2, 2));
            for (int seed = 0; seed < 24; seed++) {
                var zombie = EntityType.ZOMBIE.create(world);
                zombie.setPosition(pos.getX(), pos.getY(), pos.getZ());
                zombie.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);
                zombie.setBaby(false);
                zombie.getRandom().setSeed(seed);
                world.spawnEntity(zombie);
                context.assertTrue(!zombie.getStatusEffects().isEmpty(), "Every undead elite must get at least one effective buff");
                context.assertTrue(!zombie.hasStatusEffect(StatusEffects.REGENERATION), "Do not select a buff this species rejects");
                zombie.discard();
            }
        } finally { ThirteenBlade.balance.eliteSpawnChance = oldChance; }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void spiderAndCreeperKillsUnlockInfiniteEffects(TestContext context) throws Exception {
        ItemStack spiderSword = new ItemStack(ThirteenBlade.SWORD);
        var spiderPlayer = player(context, spiderSword);
        arm(spiderPlayer);
        kill(context, spiderPlayer, EntityType.CAVE_SPIDER.create(context.getWorld()));
        context.assertTrue(BladeData.has(spiderSword, BladeData.SLOW_FALL), "Spiders and cave spiders must unlock slow falling");
        context.assertTrue(spiderPlayer.getStatusEffect(StatusEffects.SLOW_FALLING).isInfinite(), "Slow falling must have infinite duration");
        ItemStack creeperSword = new ItemStack(ThirteenBlade.SWORD);
        var creeperPlayer = player(context, creeperSword);
        arm(creeperPlayer);
        kill(context, creeperPlayer, EntityType.CREEPER.create(context.getWorld()));
        context.assertTrue(BladeData.has(creeperSword, BladeData.CREEPER_SHIELD), "Creeper must unlock absorption");
        context.assertTrue(creeperPlayer.getStatusEffect(StatusEffects.ABSORPTION).isInfinite(), "Absorption duration must be infinite");
        context.assertTrue(creeperPlayer.getAbsorptionAmount() == 4, "Absorption I grants two yellow hearts");
        BladeEffects.release(spiderPlayer);
        BladeEffects.release(creeperPlayer);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void armedKillStealsAllLastingBuffsButNoDebuffsOrInstantEffects(TestContext context) throws Exception {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        var player = player(context, sword);
        var victim = EntityType.HUSK.create(context.getWorld());
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 1));
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 1));
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 0));
        arm(player);
        kill(context, player, victim);
        context.assertTrue(BladeData.has(sword, BladeData.HUNGER_WARD), "Species power and buffs must be awarded together");
        context.assertTrue(BladeData.stolenEffects(sword, System.currentTimeMillis()).size() == 2, "Steal only the two beneficial buffs");
        context.assertTrue(player.getStatusEffect(StatusEffects.SPEED).isInfinite(), "A finite target buff must become infinite by default");
        context.assertTrue(player.getStatusEffect(StatusEffects.SPEED).getAmplifier() == 1, "Preserve the effect's level");
        context.assertTrue(!BladeData.capture(sword, new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1), System.currentTimeMillis()), "Instant healing must not become infinite healing");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void unarmedKillCannotStealBuffs(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        var player = player(context, sword);
        var victim = EntityType.SPIDER.create(context.getWorld());
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, -1, 1));
        kill(context, player, victim);
        context.assertTrue(BladeData.kills(sword) == 1 && !BladeData.has(sword, BladeData.SLOW_FALL), "Normal kill only advances growth");
        context.assertTrue(BladeData.stolenEffects(sword, System.currentTimeMillis()).isEmpty(), "No buff theft without arming");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void stolenBuffsPersistAndRepeatsNeverStackLevels(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        long now = System.currentTimeMillis();
        BladeData.capture(sword, new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0), now);
        BladeData.capture(sword, new StatusEffectInstance(StatusEffects.STRENGTH, 40, 100), now);
        BladeData.capture(sword, new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0), now);
        var saved = ItemStack.fromNbt(sword.writeNbt(new NbtCompound()));
        var effects = BladeData.stolenEffects(saved, now);
        context.assertTrue(effects.size() == 1 && effects.get(StatusEffects.STRENGTH).amplifier() == 2,
                "Repeats keep max level, limited to III by default");
        context.assertTrue(effects.get(StatusEffects.STRENGTH).expiresAt() == -1, "Infinite duration must persist");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void yellowHeartsCannotBeRefilledByTicksSwitchingOrMilk(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        BladeData.unlock(sword, BladeData.CREEPER_SHIELD);
        var player = player(context, sword);
        BladeEffects.refresh(player);
        player.damage(player.getDamageSources().generic(), 2);
        context.assertTrue(player.getAbsorptionAmount() == 2, "Incoming damage consumes yellow hearts; actual=" + player.getAbsorptionAmount());
        for (int i = 0; i < 100; i++) BladeEffects.refresh(player);
        context.assertTrue(player.getAbsorptionAmount() == 2, "Effect ticks must not refill shield");
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        context.assertTrue(player.getStatusEffect(StatusEffects.ABSORPTION).getDuration() == 100 && player.getAbsorptionAmount() == 2,
                "Unequipping retains only the remaining shield for five seconds");
        player.setStackInHand(Hand.MAIN_HAND, sword);
        BladeEffects.refresh(player);
        context.assertTrue(player.getAbsorptionAmount() == 2, "Re-equipping must restore remaining hearts only");
        player.clearStatusEffects();
        BladeEffects.refresh(player);
        context.assertTrue(player.getAbsorptionAmount() == 2, "Milk must not refill the sword's shield");
        BladeEffects.replenishShield(player, sword);
        context.assertTrue(player.getAbsorptionAmount() == 4, "An explicit absorption reward can replenish the shield");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void ordinaryPotionSurvivesUnequipAndOwnedEffectsPersistTheirMarker(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        BladeData.capture(sword, new StatusEffectInstance(StatusEffects.SPEED, 40, 1), System.currentTimeMillis());
        var player = player(context, sword);
        BladeEffects.refresh(player);
        var owned = player.getStatusEffect(StatusEffects.SPEED);
        var saved = StatusEffectInstance.fromNbt(owned.writeNbt(new NbtCompound()));
        context.assertTrue(BladeEffects.owned(saved), "Effect ownership must survive player saves");
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 0));
        context.assertTrue(!BladeEffects.owned(player.getStatusEffect(StatusEffects.SPEED)), "Even a weaker real potion must be usable");
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        context.assertTrue(player.hasStatusEffect(StatusEffects.SPEED) && player.getStatusEffect(StatusEffects.SPEED).getDuration() == 200,
                "Unequipping must not strip a real potion");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void finiteStolenBuffsExpireWithoutBeingRenewed(TestContext context) {
        int oldDuration = ThirteenBlade.balance.stolenEffectDurationSeconds;
        ThirteenBlade.balance.stolenEffectDurationSeconds = 2;
        try {
            ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
            long now = System.currentTimeMillis();
            BladeData.capture(sword, new StatusEffectInstance(StatusEffects.SPEED, 20), now);
            context.assertTrue(!BladeData.stolenEffects(sword, now + 1999).isEmpty(), "Finite capture persists before deadline");
            context.assertTrue(BladeData.stolenEffects(sword, now + 2000).isEmpty(), "Finite capture expires exactly at deadline");
            var player = player(context, sword);
            BladeEffects.refresh(player);
            context.assertTrue(!player.getStatusEffect(StatusEffects.SPEED).isInfinite(), "Finite configuration must produce finite effects");
            BladeData.write(sword).getList("StolenEffects", 10).getCompound(0).putLong("ExpiresAt", now - 1);
            BladeEffects.refresh(player);
            context.assertTrue(!player.hasStatusEffect(StatusEffects.SPEED), "Expired sword power must be removed rather than renewed");
            BladeEffects.release(player);
        } finally { ThirteenBlade.balance.stolenEffectDurationSeconds = oldDuration; }
        context.complete();
    }
}
