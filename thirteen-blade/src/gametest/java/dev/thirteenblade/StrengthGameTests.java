package dev.thirteenblade;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class StrengthGameTests implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void levelUpHealsBothHandsToActualCarriedMaximum(TestContext context) {
        for (var item : new ThirteenBladeItem[]{ThirteenBlade.SWORD, ThirteenBlade.DRAGON_SWORD}) {
            for (var hand : Hand.values()) {
                var sword = new ItemStack(item);
                BladeData.write(sword).putInt("Kills", 12);
                var player = SoulGameTests.player(context, ItemStack.EMPTY);
                player.setStackInHand(hand, sword);
                var stronger = new ItemStack(ThirteenBlade.DRAGON_SWORD);
                BladeData.write(stronger).putInt("Kills", 1300);
                player.getInventory().setStack(10, stronger);
                BladeGameplay.refreshAttributes(player);
                player.setHealth(3);
                SoulGameTests.kill(context, player, EntityType.ZOMBIE.create(context.getWorld()));
                context.assertTrue(BladeData.level(sword) == 1 && player.getMaxHealth() == 220 && player.getHealth() == 220,
                        "Level-up heals to actual maximum including stronger carried sword");
                player.setHealth(3);
                SoulGameTests.kill(context, player, EntityType.ZOMBIE.create(context.getWorld()));
                BladeGameplay.refreshAttributes(player);
                context.assertTrue(player.getHealth() == 3, "Ordinary kill and attribute refresh must not heal");
            }
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cappedBaseStopsHealingWhileAdvancedKeepsHealing(TestContext context) {
        for (var item : new ThirteenBladeItem[]{ThirteenBlade.SWORD, ThirteenBlade.DRAGON_SWORD}) {
            var sword = new ItemStack(item);
            BladeData.write(sword).putInt("Kills", 129);
            var player = SoulGameTests.player(context, sword);
            BladeGameplay.refreshAttributes(player);
            player.setHealth(3);
            SoulGameTests.kill(context, player, EntityType.ZOMBIE.create(context.getWorld()));
            context.assertTrue(player.getHealth() == 40, "Reaching level ten heals");
            BladeData.write(sword).putInt("Kills", 142);
            player.setHealth(3);
            SoulGameTests.kill(context, player, EntityType.ZOMBIE.create(context.getWorld()));
            context.assertTrue(player.getHealth() == (item == ThirteenBlade.SWORD ? 3 : 42), "Only advanced blade levels and heals past ten");
        }
        context.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void intrinsicEnchantsMigrateAndPreserveOtherData(TestContext context) {
        for (var item : new ThirteenBladeItem[]{ThirteenBlade.SWORD, ThirteenBlade.DRAGON_SWORD}) {
            var sword = new ItemStack(item);
            sword.setCustomName(Text.literal("Old sword"));
            sword.addEnchantment(Enchantments.SHARPNESS, 4);
            BladeData.write(sword).putInt("Kills", 130);
            var player = SoulGameTests.player(context, sword);
            item.inventoryTick(sword, context.getWorld(), player, 0, true);
            context.assertTrue(EnchantmentHelper.getLevel(Enchantments.LOOTING, sword) == 3
                    && EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, sword) == 1, "Old and new stacks gain intrinsic enchantments");
            var snapshot = sword.getNbt().copy();
            for (int i = 0; i < 10; i++) item.inventoryTick(sword, context.getWorld(), player, 0, true);
            context.assertTrue(snapshot.equals(sword.getNbt()), "Repeated ticks do not rewrite or duplicate data");
            context.assertTrue(BladeData.kills(sword) == 130 && sword.getName().getString().equals("Old sword")
                    && EnchantmentHelper.getLevel(Enchantments.SHARPNESS, sword) == 4, "Keep name, growth and additional enchants");
            var enchantments = EnchantmentHelper.get(sword);
            enchantments.put(Enchantments.LOOTING, 5);
            EnchantmentHelper.set(enchantments, sword);
            BladeEnchantments.ensure(sword);
            context.assertTrue(EnchantmentHelper.getLevel(Enchantments.LOOTING, sword) == 5, "Do not downgrade stronger custom Looting");
            context.assertTrue(EnchantmentHelper.getLevel(Enchantments.LOOTING, item.getDefaultStack()) == 3, "Creative default includes Looting");
            var crafted = new ItemStack(item);
            item.onCraft(crafted, context.getWorld(), player);
            context.assertTrue(EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, crafted) == 1, "Crafted sword includes curse");
        }
        context.complete();
    }
}
