package dev.thirteenblade;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thirteenblade")
@PrefixGameTestTemplate(false)
public final class BladeGameTests {
    @GameTest(template = "empty")
    public static void lethalDragonHitUnlocksFlight(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
        var player = player(h, sword);
        var dragon = EntityType.ENDER_DRAGON.create(h.getLevel());
        dragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
        BladeGameplay.toggleAbsorption(player);
        dragon.hurt(dragon.head, player.damageSources().playerAttack(player), 10000);
        h.assertTrue(SoulPower.DRAGON.known(sword) && player.getAbilities().mayfly, "Lethal dragon part hit must grant flight");
        h.assertTrue(BladeData.kills(sword) == 1, "Dragon counts once");
        BladeEffects.release(player);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void naturalElitesPersistWithoutReroll(GameTestHelper h) {
        double chance = ThirteenBlade.balance.eliteSpawnChance;
        ThirteenBlade.balance.eliteSpawnChance = 1;
        try {
            var mob = EntityType.CREEPER.create(h.getLevel());
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, h.getLevel(), h.getLevel().getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
            h.getLevel().addFreshEntity(mob);
            h.assertTrue(mob.getTags().contains(EliteMobs.ELITE_TAG), "Natural spawn hook creates elite");
            h.assertTrue(mob.getMaxHealth() == 30 && mob.getHealth() == 30, "Elite gains full 1.5x health");
            h.assertTrue(mob.isCurrentlyGlowing() && !mob.getActiveEffects().isEmpty(), "Elite has glow and buffs");
            var saved = new CompoundTag();
            mob.saveWithoutId(saved);
            var restored = EntityType.CREEPER.create(h.getLevel());
            restored.load(saved);
            EliteMobs.processSpawn(restored);
            h.assertTrue(restored.getMaxHealth() == 30 && restored.getActiveEffects().size() == mob.getActiveEffects().size(), "Reload keeps attributes and buffs");
            for (var reason : List.of(net.minecraft.world.entity.MobSpawnType.COMMAND, net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, net.minecraft.world.entity.MobSpawnType.SPAWNER)) {
                var artificial = EntityType.CREEPER.create(h.getLevel());
                EliteMobs.markSpawn(artificial, reason);
                EliteMobs.processSpawn(artificial);
                h.assertTrue(!artificial.getTags().contains(EliteMobs.ELITE_TAG), "Artificial spawn excluded");
            }
            h.assertTrue(!EliteMobs.eligible(EntityType.WARDEN.create(h.getLevel())), "Warden excluded");
        } finally { ThirteenBlade.balance.eliteSpawnChance = chance; }
        h.succeed();
    }

    static FakePlayer player(GameTestHelper h, ItemStack sword) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "BladeTest"));
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        return player;
    }
    static void kill(GameTestHelper h, FakePlayer player, Mob victim) {
        victim.setPos(h.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3)).getCenter());
        h.getLevel().addFreshEntity(victim);
        victim.hurt(player.damageSources().playerAttack(player), 10000);
    }

    @GameTest(template = "empty")
    public static void upgradeCopiesComponentsAndUnlocksGrowth(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.SWORD.get());
        BladeData.ensureIdentity(sword);
        BladeData.edit(sword, data -> data.putInt("Kills", 1300));
        BladeData.setCooldown(sword, System.currentTimeMillis() + 30000);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Ancient blade"));
        BladeData.unlock(sword, SoulPower.DRAGON.flag);
        BladeData.saveShield(sword, 1);
        h.assertTrue(BladeData.level(sword) == 10, "Base caps at ten");
        var recipe = new DragonUpgradeRecipe(CraftingBookCategory.EQUIPMENT);
        var input = CraftingInput.of(2, 2, List.of(sword, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.DRAGON_EGG)));
        h.assertTrue(recipe.matches(input, h.getLevel()), "Sword and egg match");
        var upgraded = recipe.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(upgraded.is(ThirteenBlade.DRAGON_SWORD.get()) && BladeData.level(upgraded) == 100, "Ascension unlocks stored growth");
        h.assertTrue(upgraded.getComponentsPatch().equals(sword.getComponentsPatch()), "All custom components survive");
        h.assertTrue(!upgraded.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(sword.get(DataComponents.ATTRIBUTE_MODIFIERS)), "Advanced default attack must remain stronger");
        BladeData.addKill(upgraded);
        h.assertTrue(BladeData.kills(sword) == 1300, "Preview must not mutate source");
        var bad = CraftingInput.of(3, 1, List.of(sword, new ItemStack(Items.DRAGON_EGG), new ItemStack(Items.DIRT)));
        h.assertTrue(!recipe.matches(bad, h.getLevel()), "Reject extra ingredients");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyFamilyIncludingBreezeAwardsPower(GameTestHelper h) {
        EntityType<?>[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
            EntityType.BLAZE, EntityType.WITHER, EntityType.PILLAGER, EntityType.PHANTOM, EntityType.WITCH,
            EntityType.SLIME, EntityType.VINDICATOR, EntityType.GUARDIAN, EntityType.WARDEN, EntityType.SHULKER, EntityType.BREEZE};
        for (var type : types) {
            var sword = new ItemStack(ThirteenBlade.SWORD.get());
            var player = player(h, sword);
            var victim = (Mob) type.create(h.getLevel());
            var power = SoulPower.of(victim);
            BladeGameplay.toggleAbsorption(player);
            h.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) == 0, "Arming is free");
            kill(h, player, victim);
            h.assertTrue(power.known(sword), "Missing soul: " + type);
            h.assertTrue(BladeData.soulCount(sword) == 1 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 2, "First soul grants two toughness");
            h.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) > 0, "Trigger starts cooldown");
            if (power.effect != null) {
                var effect = player.getEffect(power.effect);
                h.assertTrue(effect != null && effect.isInfiniteDuration() && effect.getAmplifier() == power.amplifier, "Wrong buff: " + type);
            }
            BladeEffects.release(player);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void familyDeduplicationAndLegacySpider(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.SWORD.get());
        BladeData.unlock(sword, BladeData.SLOW_FALL);
        h.assertTrue(!BladeData.discover(sword, EntityType.CAVE_SPIDER.create(h.getLevel())), "Legacy spider counts once");
        h.assertTrue(BladeData.discover(sword, EntityType.ZOMBIE.create(h.getLevel())), "First zombie");
        h.assertTrue(!BladeData.discover(sword, EntityType.HUSK.create(h.getLevel())), "Husk shares family");
        h.assertTrue(BladeData.discover(sword, EntityType.GHAST.create(h.getLevel())), "Unlisted hostile grants toughness");
        h.assertTrue(!BladeData.discover(sword, EntityType.COW.create(h.getLevel())), "Passive mob does not");
        var player = player(h, sword);
        BladeEffects.refresh(player);
        h.assertTrue(player.hasEffect(MobEffects.INVISIBILITY) && !player.hasEffect(MobEffects.SLOW_FALLING), "Legacy spider becomes invisibility");
        BladeEffects.release(player);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandToughnessAndImmunities(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
        for (var power : SoulPower.values()) BladeData.unlock(sword, power.flag);
        var player = player(h, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, sword);
        BladeGameplay.refreshAttributes(player);
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 32, "All families grant 32 effective toughness");
        for (var effect : List.of(MobEffects.HUNGER, MobEffects.BLINDNESS, MobEffects.DARKNESS))
            h.assertTrue(!player.addEffect(new MobEffectInstance(effect, 200)), "Immunity blocks debuff");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        BladeGameplay.refreshAttributes(player);
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0, "No toughness after release");
        h.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200)), "Immunity ends on release");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryHealthDoesNotStackOrHeal(GameTestHelper h) {
        var base = new ItemStack(ThirteenBlade.SWORD.get());
        var advanced = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
        BladeData.edit(base, data -> data.putInt("Kills", 1300));
        BladeData.edit(advanced, data -> data.putInt("Kills", 1300));
        var player = player(h, ItemStack.EMPTY);
        player.getInventory().items.set(10, base);
        BladeGameplay.refreshAttributes(player);
        h.assertTrue(player.getMaxHealth() == 40 && player.getHealth() == 20, "Base gives capped health without healing");
        player.getInventory().items.set(11, advanced);
        BladeGameplay.refreshAttributes(player);
        h.assertTrue(player.getMaxHealth() == 220, "Strongest inventory sword wins");
        player.setHealth(100);
        player.getInventory().clearContent();
        BladeGameplay.refreshAttributes(player);
        h.assertTrue(player.getMaxHealth() == 20 && player.getHealth() == 20, "Removal clamps current health");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandKillsAndMainhandPriority(GameTestHelper h) {
        var off = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
        var main = new ItemStack(ThirteenBlade.SWORD.get());
        var player = player(h, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        BladeGameplay.toggleAbsorption(player);
        kill(h, player, EntityType.SPIDER.create(h.getLevel()));
        h.assertTrue(BladeData.kills(off) == 1 && SoulPower.SPIDER.known(off), "Offhand receives kills and souls");
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        kill(h, player, EntityType.ZOMBIE.create(h.getLevel()));
        h.assertTrue(BladeData.kills(off) == 1 && BladeData.kills(main) == 1, "Mainhand wins and unarmed kill grants no soul");
        h.assertTrue(!SoulPower.ZOMBIE.known(main), "Normal kill cannot absorb");
        BladeEffects.release(player);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void meleeWeaknessExcludesProjectiles(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.SWORD.get());
        BladeData.unlock(sword, SoulPower.VINDICATOR.flag);
        var player = player(h, sword);
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.hurt(player.damageSources().playerAttack(player), 1);
        h.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "Melee applies weakness");
        var other = EntityType.ZOMBIE.create(h.getLevel());
        other.hurt(player.damageSources().arrow(EntityType.ARROW.create(h.getLevel()), player), 1);
        h.assertTrue(!other.hasEffect(MobEffects.WEAKNESS), "Arrows cannot apply sword weakness");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 680)
    public static void armedStateSurvivesThirtySeconds(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.SWORD.get());
        var player = player(h, sword);
        BladeGameplay.toggleAbsorption(player);
        h.runAfterDelay(640, () -> {
            h.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) == 0, "Waiting is free");
            kill(h, player, EntityType.SPIDER.create(h.getLevel()));
            h.assertTrue(SoulPower.SPIDER.known(sword), "No thirty second timeout");
            h.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) > 0, "Trigger consumes cooldown");
            BladeEffects.release(player);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void flightGraceAndCreativePreservation(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
        BladeData.unlock(sword, SoulPower.DRAGON.flag);
        var player = player(h, sword);
        BladeEffects.refresh(player);
        h.assertTrue(player.getAbilities().mayfly, "Dragon grants flight");
        player.getAbilities().flying = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        h.assertTrue(player.getAbilities().mayfly, "Flight lingers");
        h.runAfterDelay(101, () -> {
            BladeEffects.refresh(player);
            h.assertTrue(!player.getAbilities().mayfly && !player.getAbilities().flying, "Revoke survival flight after grace");
            player.setGameMode(GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            BladeEffects.refresh(player);
            BladeEffects.release(player);
            h.assertTrue(player.getAbilities().mayfly, "Keep creative flight");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void theftFiltersCapsAndPersists(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.SWORD.get());
        var player = player(h, sword);
        var mob = EntityType.HUSK.create(h.getLevel());
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1));
        mob.addEffect(new MobEffectInstance(MobEffects.POISON, 80));
        BladeGameplay.toggleAbsorption(player);
        kill(h, player, mob);
        h.assertTrue(BladeData.stolenEffects(sword, System.currentTimeMillis()).size() == 2, "Only beneficial lasting effects are stolen");
        h.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).isInfiniteDuration(), "Finite target buff becomes infinite");
        BladeData.capture(sword, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 100), System.currentTimeMillis());
        var restored = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag) sword.save(h.getLevel().registryAccess()));
        h.assertTrue(BladeData.stolenEffects(restored, System.currentTimeMillis()).get(MobEffects.MOVEMENT_SPEED).amplifier() == 2, "Save preserves capped level III");
        h.assertTrue(!BladeData.capture(sword, new MobEffectInstance(MobEffects.HEAL, 1), System.currentTimeMillis()), "Reject instant healing");
        BladeEffects.release(player);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void effectOwnershipAndExternalPotion(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.SWORD.get());
        BladeData.capture(sword, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1), System.currentTimeMillis());
        var player = player(h, sword);
        BladeEffects.refresh(player);
        var saved = MobEffectInstance.load((CompoundTag) player.getEffect(MobEffects.MOVEMENT_SPEED).save());
        h.assertTrue(BladeEffects.owned(saved), "Ownership survives save");
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        h.assertTrue(!BladeEffects.owned(player.getEffect(MobEffects.MOVEMENT_SPEED)), "Real weaker potion takes ownership");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        h.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 200, "Real potion survives release");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void absorptionCannotRefillBySwitchingOrMilk(GameTestHelper h) {
        var sword = new ItemStack(ThirteenBlade.SWORD.get());
        BladeData.unlock(sword, SoulPower.CREEPER.flag);
        var player = player(h, sword);
        BladeEffects.refresh(player);
        h.assertTrue(player.getAbsorptionAmount() == 4, "Absorption I grants four HP");
        player.setAbsorptionAmount(2);
        for (int i = 0; i < 100; i++) BladeEffects.refresh(player);
        h.assertTrue(player.getAbsorptionAmount() == 2, "Ticks cannot refill");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        h.assertTrue(player.getEffect(MobEffects.ABSORPTION).getDuration() == 100, "Effect gets five second grace");
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        BladeEffects.refresh(player);
        h.assertTrue(player.getAbsorptionAmount() == 2, "Switching cannot refill");
        player.removeAllEffects();
        BladeEffects.refresh(player);
        h.assertTrue(player.getAbsorptionAmount() == 2, "Milk cannot refill");
        BladeEffects.replenishShield(player, sword);
        h.assertTrue(player.getAbsorptionAmount() == 4, "New reward refills");
        BladeEffects.release(player);
        h.succeed();
    }
}
