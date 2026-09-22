package dev.thirteenblade;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ThirteenBladeItem extends SwordItem {
    public ThirteenBladeItem(boolean advanced) {
        super(Tiers.NETHERITE, new Properties().stacksTo(1).fireResistant().rarity(advanced ? Rarity.EPIC : Rarity.RARE)
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, advanced ? 7 : 1, -2.4f))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return true; }
    @Override public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {}
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) { return true; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return false; }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) BladeData.ensureIdentity(stack);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        BalanceConfig config = ThirteenBlade.balance;
        lines.add(Component.translatable("tooltip.thirteenblade.unbreakable").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.thirteenblade.level", BladeData.level(stack),
                BladeData.kills(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.thirteenblade.bonus", number(BladeData.baseAttack(stack) + BladeData.damageBonus(stack)),
                number(BladeData.damageBonus(stack))).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.thirteenblade.carried_health", number(BladeData.healthBonus(stack)))
                .withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable(BladeData.advanced(stack) ? "tooltip.thirteenblade.advanced" : "tooltip.thirteenblade.base_cap").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.thirteenblade.toughness", number(BladeData.toughnessBonus(stack)), BladeData.soulCount(stack)).withStyle(ChatFormatting.GRAY));
        int remaining = Progression.remaining(BladeData.kills(stack), config);
        lines.add(Component.translatable(!BladeData.advanced(stack) && BladeData.level(stack) >= 10 ? "tooltip.thirteenblade.capped" : "tooltip.thirteenblade.next", remaining)
                .withStyle(ChatFormatting.DARK_AQUA));
        for (SoulPower power : SoulPower.values())
            if (power.known(stack)) lines.add(Component.translatable(power.translation).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (var effect : BladeData.stolenEffects(stack, System.currentTimeMillis()).values()) {
            Component duration = effect.expiresAt() < 0 ? Component.translatable("tooltip.thirteenblade.infinite")
                    : Component.translatable("tooltip.thirteenblade.seconds", Math.max(1, (effect.expiresAt() - System.currentTimeMillis() + 999) / 1000));
            lines.add(Component.translatable("tooltip.thirteenblade.stolen", effect.effect().value().getDisplayName(), effect.amplifier() + 1, duration)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        lines.add(Component.translatable("tooltip.thirteenblade.keys").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static String number(double value) {
        return value == (long) value ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
