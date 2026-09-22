package dev.thirteenblade;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;

public final class CarryGameTests implements FabricGameTest {
    private static ItemStack sword(int kills) {
        var sword = new ItemStack(ThirteenBlade.DRAGON_SWORD);
        BladeData.write(sword).putInt("Kills", kills);
        return sword;
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void offhandReceivesMeleeGrowthAndAbsorption(TestContext context) throws Exception {
        var sword = sword(142);
        var player = SoulGameTests.player(context, ItemStack.EMPTY);
        player.setStackInHand(Hand.OFF_HAND, sword);
        BladeGameplay.refreshAttributes(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == 21, "Offhand must add growth to main-hand attacks");
        SoulGameTests.arm(player);
        var skeleton = EntityType.SKELETON.create(context.getWorld());
        skeleton.setHealth(0.1f);
        player.attack(skeleton);
        context.assertTrue(BladeData.kills(sword) == 143 && BladeData.level(sword) == 11, "Offhand melee kill must grow beyond level ten");
        context.assertTrue(player.getMaxHealth() == 42 && player.getHealth() == 20, "Level eleven grants vitality without healing");
        context.assertTrue(player.getStatusEffect(StatusEffects.NIGHT_VISION).isInfinite(), "Offhand activation must absorb the skeleton power");
        BladeData.unlock(sword, BladeData.HUNGER_WARD);
        context.assertTrue(!player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200)), "Offhand hunger ward must reject Hunger");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void backpackGrantsOnlyVitalityAndRemovingSwordClampsHealth(TestContext context) {
        var sword = sword(1300);
        BladeData.unlock(sword, BladeData.NIGHT_SIGHT);
        var player = SoulGameTests.player(context, ItemStack.EMPTY);
        player.getInventory().setStack(20, sword);
        BladeGameplay.refreshAttributes(player);
        BladeEffects.refresh(player);
        context.assertTrue(player.getMaxHealth() == 220 && player.getHealth() == 20, "Backpack sword must grant uncapped vitality without healing");
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) == 1, "Backpack alone must not grant combat damage");
        context.assertTrue(!player.hasStatusEffect(StatusEffects.NIGHT_VISION), "Backpack alone must not activate powers");
        SoulGameTests.kill(context, player, EntityType.ZOMBIE.create(context.getWorld()));
        context.assertTrue(BladeData.kills(sword) == 1300, "Backpack sword must not gain unrelated kills");
        player.setHealth(220);
        player.getInventory().setStack(20, ItemStack.EMPTY);
        BladeGameplay.refreshAttributes(player);
        context.assertTrue(player.getMaxHealth() == 20 && player.getHealth() == 20, "Removing the last carried sword must revoke vitality");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void strongestInventorySwordSuppliesHealthAndMainHandHasCombatPriority(TestContext context) {
        var main = sword(13);
        var offhand = sword(39);
        var backpack = sword(65);
        var player = SoulGameTests.player(context, main);
        player.setStackInHand(Hand.OFF_HAND, offhand);
        player.getInventory().setStack(20, backpack);
        for (int i = 0; i < 20; i++) BladeGameplay.refreshAttributes(player);
        context.assertTrue(player.getMaxHealth() == 30, "Only the strongest sword's vitality applies");
        SoulGameTests.kill(context, player, EntityType.ZOMBIE.create(context.getWorld()));
        context.assertTrue(BladeData.kills(main) == 14 && BladeData.kills(offhand) == 39, "Only the main-hand sword receives the kill");
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        player.getInventory().setStack(20, ItemStack.EMPTY);
        BladeGameplay.refreshAttributes(player);
        context.assertTrue(player.getMaxHealth() == 26 && BladeInventory.activeSword(player) == offhand, "Offhand takes over without stacking");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void transferringTheSameSwordBetweenHandsKeepsArmedSkill(TestContext context) throws Exception {
        var sword = sword(0);
        var player = SoulGameTests.player(context, sword);
        SoulGameTests.arm(player);
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        player.setStackInHand(Hand.OFF_HAND, sword);
        SoulGameTests.kill(context, player, EntityType.SPIDER.create(context.getWorld()));
        context.assertTrue(SoulPower.SPIDER.known(sword), "Moving the same sword between hands must keep the prepared skill");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 140)
    public void effectsExpireFiveSecondsAfterStowingWhileVitalityRemains(TestContext context) {
        var sword = sword(26);
        BladeData.unlock(sword, BladeData.NIGHT_SIGHT);
        BladeData.unlock(sword, BladeData.CREEPER_SHIELD);
        var player = SoulGameTests.player(context, sword);
        BladeEffects.refresh(player);
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        player.getInventory().setStack(20, sword);
        BladeGameplay.refreshAttributes(player);
        BladeEffects.refresh(player);
        context.assertTrue(player.getStatusEffect(StatusEffects.NIGHT_VISION).getDuration() == 100, "Infinite effect becomes a five-second effect");
        context.assertTrue(player.getMaxHealth() == 24, "Stowing must retain inventory vitality");
        context.waitAndRun(50, () -> {
            BladeEffects.refresh(player);
            context.assertTrue(player.hasStatusEffect(StatusEffects.NIGHT_VISION), "Effect must survive midway through grace");
            player.damage(player.getDamageSources().generic(), 2);
            context.assertTrue(player.getAbsorptionAmount() == 2, "Lingering shield can still take damage");
        });
        context.waitAndRun(101, () -> {
            BladeEffects.refresh(player);
            context.assertTrue(!player.hasStatusEffect(StatusEffects.NIGHT_VISION) && !player.hasStatusEffect(StatusEffects.ABSORPTION), "Refresh must not extend the original grace deadline");
            context.assertTrue(player.getAbsorptionAmount() == 0 && BladeData.shield(sword, 0) == 2, "Expired shield must remember only the unspent hearts");
            BladeEffects.release(player);
            context.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void reequippingDuringGraceRestoresInfiniteDurationWithoutRefillingShield(TestContext context) {
        var sword = sword(0);
        BladeData.unlock(sword, BladeData.CREEPER_SHIELD);
        var player = SoulGameTests.player(context, sword);
        BladeEffects.refresh(player);
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        player.damage(player.getDamageSources().generic(), 2);
        player.setStackInHand(Hand.OFF_HAND, sword);
        BladeEffects.refresh(player);
        context.assertTrue(player.getStatusEffect(StatusEffects.ABSORPTION).isInfinite(), "Re-equipping must restore infinite duration");
        context.assertTrue(player.getAbsorptionAmount() == 2, "Damage taken during grace must not be refunded");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void differentSwordDoesNotTakeOwnershipOfLingeringShield(TestContext context) {
        var first = sword(0);
        BladeData.unlock(first, BladeData.CREEPER_SHIELD);
        var second = sword(0);
        BladeData.unlock(second, BladeData.NIGHT_SIGHT);
        var player = SoulGameTests.player(context, first);
        BladeEffects.refresh(player);
        player.setStackInHand(Hand.MAIN_HAND, second);
        BladeEffects.refresh(player);
        player.damage(player.getDamageSources().generic(), 2);
        BladeEffects.refresh(player);
        context.assertTrue(player.getStatusEffect(StatusEffects.NIGHT_VISION).isInfinite(), "New sword's own power must activate");
        context.assertTrue(player.getStatusEffect(StatusEffects.ABSORPTION).getDuration() == 100, "Old sword's unrelated shield must linger");
        context.assertTrue(BladeData.shield(first, 0) == 2 && !BladeData.read(second).contains("ShieldRemaining"), "Lingering damage must belong to the original sword");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void graceDoesNotExtendShortBuffsOrStripExternalPotions(TestContext context) {
        var sword = sword(0);
        long now = System.currentTimeMillis();
        BladeData.capture(sword, new StatusEffectInstance(StatusEffects.SPEED, 40), now);
        BladeData.write(sword).getList("StolenEffects", 10).getCompound(0).putLong("ExpiresAt", now + 2000);
        var player = SoulGameTests.player(context, sword);
        BladeEffects.refresh(player);
        int remaining = player.getStatusEffect(StatusEffects.SPEED).getDuration();
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        context.assertTrue(player.getStatusEffect(StatusEffects.SPEED).getDuration() <= remaining, "Grace must never extend a shorter effect");
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1));
        BladeEffects.release(player);
        context.assertTrue(!BladeEffects.owned(player.getStatusEffect(StatusEffects.SPEED)) && player.getStatusEffect(StatusEffects.SPEED).getDuration() == 600, "Ordinary potion must survive ownership cleanup");
        context.complete();
    }
}
