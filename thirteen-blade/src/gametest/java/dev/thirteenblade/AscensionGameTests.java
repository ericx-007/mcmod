package dev.thirteenblade;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class AscensionGameTests implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void lethalDragonHitUnlocksFlight(TestContext context) throws Exception {
        var sword = new ItemStack(ThirteenBlade.DRAGON_SWORD);
        var player = SoulGameTests.player(context, sword);
        var dragon = EntityType.ENDER_DRAGON.create(context.getWorld());
        dragon.getPhaseManager().setPhase(net.minecraft.entity.boss.dragon.phase.PhaseType.HOLDING_PATTERN);
        SoulGameTests.arm(player);
        dragon.damagePart(dragon.head, player.getDamageSources().playerAttack(player), 10000);
        context.assertTrue(SoulPower.DRAGON.known(sword) && player.getAbilities().allowFlying, "Lethal dragon part hit must grant flight");
        context.assertTrue(BladeData.kills(sword) == 1, "Dragon counts once");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void upgradePreservesAllSwordDataAndUnlocksStoredGrowth(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        BladeData.write(sword).putInt("Kills", 1300);
        BladeData.write(sword).putLong("CooldownUntil", System.currentTimeMillis() + 30000);
        sword.setCustomName(Text.literal("My ancient blade"));
        sword.addEnchantment(net.minecraft.enchantment.Enchantments.SHARPNESS, 3);
        BladeData.unlock(sword, SoulPower.DRAGON.flag);
        BladeData.saveShield(sword, 1);
        context.assertTrue(BladeData.level(sword) == 10, "Base blade caps at ten while retaining every kill");
        var player = SoulGameTests.player(context, sword);
        var inventory = new CraftingInventory(player.playerScreenHandler, 2, 2);
        inventory.setStack(0, sword);
        inventory.setStack(3, new ItemStack(Items.DRAGON_EGG));
        var recipe = new DragonUpgradeRecipe(ThirteenBlade.id("test_upgrade"), CraftingRecipeCategory.EQUIPMENT);
        context.assertTrue(recipe.matches(inventory, context.getWorld()), "Base sword and egg must match in any two slots");
        ItemStack upgraded = recipe.craft(inventory, context.getWorld().getRegistryManager());
        context.assertTrue(upgraded.isOf(ThirteenBlade.DRAGON_SWORD) && BladeData.level(upgraded) == 100, "Ascension unlocks all saved kills");
        context.assertTrue(upgraded.getNbt().equals(sword.getNbt()), "Name, enchants, identity, cooldown and powers must survive exactly");
        BladeData.addKill(upgraded);
        context.assertTrue(BladeData.kills(sword) == 1300, "Craft preview must not mutate the ingredient NBT");
        inventory.setStack(1, new ItemStack(Items.DIRT));
        context.assertTrue(!recipe.matches(inventory, context.getWorld()), "Unrelated ingredients must invalidate the upgrade");
        inventory.setStack(1, ItemStack.EMPTY);
        inventory.setStack(0, upgraded);
        context.assertTrue(!recipe.matches(inventory, context.getWorld()), "An advanced sword cannot be upgraded again");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void eachSpeciesUnlocksRequestedPowerAndTwoToughness(TestContext context) throws Exception {
        EntityType<?>[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
                EntityType.BLAZE, EntityType.WITHER, EntityType.PILLAGER, EntityType.PHANTOM, EntityType.WITCH,
                EntityType.SLIME, EntityType.VINDICATOR, EntityType.GUARDIAN, EntityType.WARDEN, EntityType.SHULKER};
        for (EntityType<?> type : types) {
            ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
            var player = SoulGameTests.player(context, sword);
            MobEntity victim = (MobEntity) type.create(context.getWorld());
            SoulPower power = SoulPower.of(victim);
            SoulGameTests.arm(player);
            context.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) == 0, "Arming must never consume cooldown");
            SoulGameTests.kill(context, player, victim);
            context.assertTrue(power.known(sword), "Expected soul for " + type);
            context.assertTrue(BladeData.soulCount(sword) == 1 && player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS) == 2, "Each first family grants two toughness");
            context.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) > 0, "Successful kill starts cooldown");
            if (power.effect != null) {
                var effect = player.getStatusEffect(power.effect);
                context.assertTrue(effect != null && effect.isInfinite() && effect.getAmplifier() == power.amplifier, "Wrong effect for " + type);
            }
            BladeEffects.release(player);
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void familyVariantsAndLegacySpiderSoulsDoNotDuplicateToughness(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        BladeData.unlock(sword, BladeData.SLOW_FALL);
        context.assertTrue(BladeData.soulCount(sword) == 1 && SoulPower.SPIDER.known(sword), "Legacy spider migrates to one family");
        context.assertTrue(!BladeData.discover(sword, EntityType.CAVE_SPIDER.create(context.getWorld())), "Legacy cave spider must not duplicate discovery");
        context.assertTrue(BladeData.discover(sword, EntityType.ZOMBIE.create(context.getWorld())), "First zombie counts");
        context.assertTrue(!BladeData.discover(sword, EntityType.HUSK.create(context.getWorld())), "Husk shares zombie family");
        context.assertTrue(BladeData.discover(sword, EntityType.GHAST.create(context.getWorld())), "Unlisted hostile species still grants toughness");
        context.assertTrue(!BladeData.discover(sword, EntityType.COW.create(context.getWorld())), "Passive species must not grant toughness");
        var player = SoulGameTests.player(context, sword);
        BladeEffects.refresh(player);
        context.assertTrue(player.hasStatusEffect(StatusEffects.INVISIBILITY) && !player.hasStatusEffect(StatusEffects.SLOW_FALLING), "Legacy spider power becomes invisibility");
        BladeEffects.release(player);
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void toughnessAboveTwentyAndImmunityRemainEffective(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.DRAGON_SWORD);
        for (SoulPower power : SoulPower.values()) BladeData.unlock(sword, power.flag);
        var player = SoulGameTests.player(context, ItemStack.EMPTY);
        player.setStackInHand(Hand.OFF_HAND, sword);
        BladeGameplay.refreshAttributes(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS) == 32, "All sixteen families must grant 32 effective toughness");
        for (var effect : new net.minecraft.entity.effect.StatusEffect[]{StatusEffects.HUNGER, StatusEffects.BLINDNESS, StatusEffects.DARKNESS})
            context.assertTrue(!player.addStatusEffect(new StatusEffectInstance(effect, 200)), "Unlocked immunity must block " + effect);
        player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        BladeGameplay.refreshAttributes(player);
        context.assertTrue(player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS) == 0, "Toughness requires holding a sword");
        context.assertTrue(player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 200)), "Releasing sword ends immunity");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void successfulMeleeHitAppliesWeaknessButArrowsDoNot(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        BladeData.unlock(sword, SoulPower.VINDICATOR.flag);
        var player = SoulGameTests.player(context, sword);
        var zombie = EntityType.ZOMBIE.create(context.getWorld());
        zombie.damage(player.getDamageSources().playerAttack(player), 1);
        context.assertTrue(zombie.hasStatusEffect(StatusEffects.WEAKNESS), "Successful sword melee must inflict Weakness");
        var other = EntityType.ZOMBIE.create(context.getWorld());
        var arrow = EntityType.ARROW.create(context.getWorld());
        other.damage(player.getDamageSources().arrow(arrow, player), 1);
        context.assertTrue(!other.hasStatusEffect(StatusEffects.WEAKNESS), "Projectile must not inherit sword contact debuff");
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 680)
    public void absorptionHasNoThirtySecondExpiry(TestContext context) throws Exception {
        ItemStack sword = new ItemStack(ThirteenBlade.SWORD);
        var player = SoulGameTests.player(context, sword);
        SoulGameTests.arm(player);
        context.waitAndRun(640, () -> {
            context.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) == 0, "Waiting must not start cooldown");
            SoulGameTests.kill(context, player, EntityType.SPIDER.create(context.getWorld()));
            context.assertTrue(SoulPower.SPIDER.known(sword), "Armed skill must survive beyond thirty seconds");
            context.assertTrue(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) > 0, "Only triggering starts cooldown");
            BladeEffects.release(player);
            context.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 140)
    public void dragonFlightRevokesAfterGraceWithoutTouchingCreative(TestContext context) {
        ItemStack sword = new ItemStack(ThirteenBlade.DRAGON_SWORD);
        BladeData.unlock(sword, SoulPower.DRAGON.flag);
        var player = SoulGameTests.player(context, sword);
        BladeEffects.refresh(player);
        context.assertTrue(player.getAbilities().allowFlying, "Dragon soul grants creative-style flight");
        player.getAbilities().flying = true;
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        BladeEffects.refresh(player);
        context.assertTrue(player.getAbilities().allowFlying, "Flight remains during grace");
        context.waitAndRun(101, () -> {
            BladeEffects.refresh(player);
            context.assertTrue(!player.getAbilities().allowFlying && !player.getAbilities().flying, "Survival flight must be revoked after grace");
            player.changeGameMode(net.minecraft.world.GameMode.CREATIVE);
            player.setStackInHand(Hand.MAIN_HAND, sword);
            BladeEffects.refresh(player);
            BladeEffects.release(player);
            context.assertTrue(player.getAbilities().allowFlying, "Never revoke creative mode flight");
            context.complete();
        });
    }
}
