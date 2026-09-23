package dev.thirteenblade;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;

public final class ThirteenBladeItem extends ItemSword {
    private static final ToolMaterial BASE = EnumHelper.addToolMaterial("THIRTEEN_BLADE", 3, 0, 8, 2, 22);
    private static final ToolMaterial DRAGON = EnumHelper.addToolMaterial("DRAGON_THIRTEEN_BLADE", 3, 0, 8, 8, 22);
    public ThirteenBladeItem(boolean advanced) {
        super(advanced ? DRAGON : BASE);
        String id = advanced ? "dragon_thirteen_blade" : "thirteen_blade";
        setRegistryName(ThirteenBlade.ID, id); setTranslationKey(ThirteenBlade.ID + "." + id);
        setCreativeTab(CreativeTabs.COMBAT); setMaxDamage(0);
    }
    @Override public boolean isDamageable() { return false; }
    @Override public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) { return true; }
    @Override public boolean getIsRepairable(ItemStack target, ItemStack ingredient) { return false; }
    @Override public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) { if (!world.isRemote) BladeEnchantments.ensure(stack); }
    @Override public void onCreated(ItemStack stack, World world, EntityPlayer player) { BladeEnchantments.ensure(stack); }
    @Override public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) { if (isInCreativeTab(tab)) items.add(ThirteenBlade.sword(this == ThirteenBlade.DRAGON_SWORD)); }
    @Override public boolean hasCustomEntity(ItemStack stack) { return true; }
    @Override public Entity createEntity(World world, Entity location, ItemStack stack) {
        EntityItem item = new FireproofItem(world, location.posX, location.posY, location.posZ, stack);
        item.readFromNBT(location.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        return item;
    }
    public static final class FireproofItem extends EntityItem {
        public FireproofItem(World world) { super(world); isImmuneToFire = true; }
        public FireproofItem(World world, double x, double y, double z, ItemStack stack) { super(world, x, y, z, stack); isImmuneToFire = true; }
        @Override public boolean attackEntityFrom(DamageSource source, float amount) { return !source.isFireDamage() && super.attackEntityFrom(source, amount); }
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        lines.add(TextFormatting.LIGHT_PURPLE + tr("tooltip.thirteenblade.unbreakable"));
        lines.add(tr(BladeData.advanced(stack) ? "tooltip.thirteenblade.advanced" : "tooltip.thirteenblade.base_cap"));
        lines.add(tr("tooltip.thirteenblade.level", BladeData.level(stack), BladeData.kills(stack)));
        lines.add(tr("tooltip.thirteenblade.bonus", BladeData.baseAttack(stack) + BladeData.damageBonus(stack), BladeData.damageBonus(stack)));
        lines.add(tr("tooltip.thirteenblade.carried_health", BladeData.healthBonus(stack)));
        lines.add(tr("tooltip.thirteenblade.toughness", BladeData.toughnessBonus(stack), BladeData.soulCount(stack)));
        lines.add(BladeData.level(stack) >= 10 && !BladeData.advanced(stack) ? tr("tooltip.thirteenblade.capped") : tr("tooltip.thirteenblade.next", Progression.remaining(BladeData.kills(stack), ThirteenBlade.balance)));
        for (SoulPower power : SoulPower.values()) if (power.known(stack)) lines.add(TextFormatting.AQUA + tr(power.translation));
        long now = System.currentTimeMillis();
        for (BladeData.StoredEffect effect : BladeData.stolenEffects(stack, now).values()) lines.add(tr("tooltip.thirteenblade.stolen", tr(effect.effect.getName()), effect.amplifier + 1,
                effect.expiresAt < 0 ? tr("tooltip.thirteenblade.infinite") : tr("tooltip.thirteenblade.seconds", (effect.expiresAt - now + 999) / 1000)));
        lines.add(TextFormatting.GRAY + tr("tooltip.thirteenblade.keys"));
    }
    private static String tr(String key, Object... values) { return I18n.translateToLocalFormatted(key, values); }
}
