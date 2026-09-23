package dev.thirteenblade;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;

/** Only species and effects present in vanilla 1.12.2; other hostile mobs still award discoveries. */
public enum SoulPower {
    ZOMBIE("HungerWard", "hunger", null), SKELETON("NightSight", "night", MobEffects.NIGHT_VISION),
    SPIDER("SpiderVeil", "invisibility", MobEffects.INVISIBILITY), CREEPER("CreeperShield", "shield", MobEffects.ABSORPTION),
    BLAZE("BlazeWard", "fire_resistance", MobEffects.FIRE_RESISTANCE), WITHER("WitherVitality", "regeneration", MobEffects.REGENERATION),
    DRAGON("DragonFlight", "flight", null), WITCH("WitchHaste", "haste", MobEffects.HASTE),
    SLIME("SlimeLeap", "jump", MobEffects.JUMP_BOOST), VINDICATOR("WeakeningEdge", "weakness", null),
    GUARDIAN("GuardianBreath", "water_breathing", MobEffects.WATER_BREATHING), SHULKER("ShulkerSatiation", "saturation", MobEffects.SATURATION);

    public final String flag, translation;
    public final Potion effect;
    SoulPower(String flag, String translation, Potion effect) { this.flag = flag; this.translation = "power.thirteenblade." + translation; this.effect = effect; }
    public boolean known(net.minecraft.item.ItemStack stack) { return BladeData.has(stack, flag) || (this == SPIDER && BladeData.has(stack, "SlowFall")); }
    public static SoulPower of(EntityLivingBase mob) {
        if (mob instanceof EntityZombie) return ZOMBIE;
        if (mob instanceof AbstractSkeleton) return SKELETON;
        if (mob instanceof EntitySpider) return SPIDER;
        if (mob instanceof EntityCreeper) return CREEPER;
        if (mob instanceof EntityBlaze) return BLAZE;
        if (mob instanceof EntityWither) return WITHER;
        if (mob instanceof EntityDragon) return DRAGON;
        if (mob instanceof EntityWitch) return WITCH;
        if (mob instanceof EntitySlime) return SLIME;
        if (mob instanceof EntityVindicator) return VINDICATOR;
        if (mob instanceof EntityGuardian) return GUARDIAN;
        if (mob instanceof EntityShulker) return SHULKER;
        return null;
    }
}
