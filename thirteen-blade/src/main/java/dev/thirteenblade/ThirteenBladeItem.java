package dev.thirteenblade;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ThirteenBladeItem extends SwordItem {
    private static final ToolMaterial MATERIAL = new ToolMaterial() {
        public int getDurability() { return 0; }
        public float getMiningSpeedMultiplier() { return 6; }
        public float getAttackDamage() { return 2; }
        public int getMiningLevel() { return 2; }
        public int getEnchantability() { return 14; }
        public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };

    public ThirteenBladeItem() {
        // Player base (1) + material (2) + sword (3) = 6; 4 - 2.4 = 1.6 attacks/s.
        super(MATERIAL, 3, -2.4f, new Settings().maxCount(1).fireproof().rarity(Rarity.RARE));
    }

    @Override public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) { return true; }
    @Override public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) { return true; }
    @Override public boolean canRepair(ItemStack stack, ItemStack ingredient) { return false; }
    @Override public boolean isEnchantable(ItemStack stack) { return stack.getCount() == 1; }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && !BladeData.read(stack).containsUuid("Identity"))
            BladeData.write(stack).putUuid("Identity", java.util.UUID.randomUUID());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> lines, TooltipContext context) {
        BalanceConfig config = ThirteenBlade.balance;
        lines.add(Text.translatable("tooltip.thirteenblade.unbreakable").formatted(Formatting.AQUA));
        lines.add(Text.translatable("tooltip.thirteenblade.level", BladeData.level(stack),
                BladeData.kills(stack)).formatted(Formatting.LIGHT_PURPLE));
        lines.add(Text.translatable("tooltip.thirteenblade.bonus", number(6 + BladeData.damageBonus(stack)),
                number(BladeData.damageBonus(stack))).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.thirteenblade.carried_health", number(BladeData.healthBonus(stack)))
                .formatted(Formatting.GREEN));
        int remaining = Progression.remaining(BladeData.kills(stack), config);
        lines.add(Text.translatable("tooltip.thirteenblade.next", remaining)
                .formatted(Formatting.DARK_AQUA));
        if (BladeData.has(stack, BladeData.HUNGER_WARD))
            lines.add(Text.translatable("power.thirteenblade.hunger").formatted(Formatting.GREEN));
        if (BladeData.has(stack, BladeData.NIGHT_SIGHT))
            lines.add(Text.translatable("power.thirteenblade.night").formatted(Formatting.BLUE));
        if (BladeData.has(stack, BladeData.SLOW_FALL))
            lines.add(Text.translatable("power.thirteenblade.slow_fall").formatted(Formatting.AQUA));
        if (BladeData.has(stack, BladeData.CREEPER_SHIELD))
            lines.add(Text.translatable("power.thirteenblade.shield").formatted(Formatting.GOLD));
        for (var effect : BladeData.stolenEffects(stack, System.currentTimeMillis()).values()) {
            Text duration = effect.expiresAt() < 0 ? Text.translatable("tooltip.thirteenblade.infinite")
                    : Text.translatable("tooltip.thirteenblade.seconds", Math.max(1, (effect.expiresAt() - System.currentTimeMillis() + 999) / 1000));
            lines.add(Text.translatable("tooltip.thirteenblade.stolen", effect.effect().getName(), effect.amplifier() + 1, duration)
                    .formatted(Formatting.LIGHT_PURPLE));
        }
        lines.add(Text.translatable("tooltip.thirteenblade.keys").formatted(Formatting.DARK_GRAY));
    }

    public static String number(double value) {
        return value == (long) value ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
