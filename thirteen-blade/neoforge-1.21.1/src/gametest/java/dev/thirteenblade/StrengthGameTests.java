package dev.thirteenblade;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thirteenblade")
@PrefixGameTestTemplate(false)
public final class StrengthGameTests {
    @GameTest(template = "empty")
    public static void levelUpHealsBothHandsToActualCarriedMaximum(GameTestHelper h) {
        for (var item : List.of(ThirteenBlade.SWORD.get(), ThirteenBlade.DRAGON_SWORD.get())) {
            for (var hand : InteractionHand.values()) {
                var sword = new ItemStack(item);
                BladeData.edit(sword, data -> data.putInt("Kills", 12));
                var player = BladeGameTests.player(h, ItemStack.EMPTY);
                player.setItemInHand(hand, sword);
                var stronger = new ItemStack(ThirteenBlade.DRAGON_SWORD.get());
                BladeData.edit(stronger, data -> data.putInt("Kills", 1300));
                player.getInventory().items.set(10, stronger);
                BladeGameplay.refreshAttributes(player);
                player.setHealth(3);
                BladeGameTests.kill(h, player, EntityType.ZOMBIE.create(h.getLevel()));
                h.assertTrue(BladeData.level(sword) == 1 && player.getMaxHealth() == 220 && player.getHealth() == 220,
                        "Level-up heals to actual maximum including stronger carried sword");
                player.setHealth(3);
                BladeGameTests.kill(h, player, EntityType.ZOMBIE.create(h.getLevel()));
                BladeGameplay.refreshAttributes(player);
                h.assertTrue(player.getHealth() == 3, "Ordinary kill and attribute refresh must not heal");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cappedBaseStopsHealingWhileAdvancedKeepsHealing(GameTestHelper h) {
        for (var item : List.of(ThirteenBlade.SWORD.get(), ThirteenBlade.DRAGON_SWORD.get())) {
            var sword = new ItemStack(item);
            BladeData.edit(sword, data -> data.putInt("Kills", 129));
            var player = BladeGameTests.player(h, sword);
            BladeGameplay.refreshAttributes(player);
            player.setHealth(3);
            BladeGameTests.kill(h, player, EntityType.ZOMBIE.create(h.getLevel()));
            h.assertTrue(player.getHealth() == 40, "Reaching level ten heals");
            BladeData.edit(sword, data -> data.putInt("Kills", 142));
            player.setHealth(3);
            BladeGameTests.kill(h, player, EntityType.ZOMBIE.create(h.getLevel()));
            h.assertTrue(player.getHealth() == (item == ThirteenBlade.SWORD.get() ? 3 : 42), "Only advanced blade levels and heals past ten");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void intrinsicEnchantsMigrateAndPreserveOtherData(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var looting = registry.getOrThrow(Enchantments.LOOTING);
        var binding = registry.getOrThrow(Enchantments.BINDING_CURSE);
        var sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        for (var item : List.of(ThirteenBlade.SWORD.get(), ThirteenBlade.DRAGON_SWORD.get())) {
            var sword = new ItemStack(item);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Old sword"));
            sword.enchant(sharpness, 4);
            BladeData.edit(sword, data -> data.putInt("Kills", 130));
            var player = BladeGameTests.player(h, sword);
            item.inventoryTick(sword, h.getLevel(), player, 0, true);
            h.assertTrue(EnchantmentHelper.getTagEnchantmentLevel(looting, sword) == 3
                    && EnchantmentHelper.getTagEnchantmentLevel(binding, sword) == 1, "Old and new swords gain intrinsic enchants");
            var snapshot = sword.getComponentsPatch();
            for (int i = 0; i < 10; i++) item.inventoryTick(sword, h.getLevel(), player, 0, true);
            h.assertTrue(snapshot.equals(sword.getComponentsPatch()), "Repeated ticks keep identical components");
            h.assertTrue(BladeData.kills(sword) == 130 && sword.getHoverName().getString().equals("Old sword")
                    && EnchantmentHelper.getTagEnchantmentLevel(sharpness, sword) == 4, "Keep name, growth and other enchants");
            sword.enchant(looting, 5);
            BladeEnchantments.ensure(sword, h.getLevel().registryAccess());
            h.assertTrue(EnchantmentHelper.getTagEnchantmentLevel(looting, sword) == 5, "Keep stronger custom Looting");
            var crafted = new ItemStack(item);
            item.onCraftedBy(crafted, h.getLevel(), player);
            h.assertTrue(EnchantmentHelper.getTagEnchantmentLevel(binding, crafted) == 1, "Crafted sword includes curse");
        }
        h.succeed();
    }
}
