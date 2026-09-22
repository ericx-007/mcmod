package dev.thirteenblade;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;

/** Families deliberately share one discovery: variants cannot farm extra toughness. */
public enum SoulPower {
    ZOMBIE("HungerWard", "hunger", null, 0),
    SKELETON("NightSight", "night", StatusEffects.NIGHT_VISION, 0),
    SPIDER("SpiderVeil", "invisibility", StatusEffects.INVISIBILITY, 0),
    CREEPER("CreeperShield", "shield", StatusEffects.ABSORPTION, 0),
    BLAZE("BlazeWard", "fire_resistance", StatusEffects.FIRE_RESISTANCE, 0),
    WITHER("WitherVitality", "regeneration", StatusEffects.REGENERATION, 0),
    DRAGON("DragonFlight", "flight", null, 0),
    PILLAGER("RaidHero", "hero", StatusEffects.HERO_OF_THE_VILLAGE, 4),
    PHANTOM("PhantomSpeed", "speed", StatusEffects.SPEED, 0),
    WITCH("WitchHaste", "haste", StatusEffects.HASTE, 0),
    SLIME("SlimeLeap", "jump", StatusEffects.JUMP_BOOST, 0),
    VINDICATOR("WeakeningEdge", "weakness", null, 0),
    GUARDIAN("GuardianBreath", "water_breathing", StatusEffects.WATER_BREATHING, 0),
    WARDEN("DeepSight", "darkness_ward", null, 0),
    SHULKER("ShulkerSatiation", "saturation", StatusEffects.SATURATION, 0),
    BREEZE("BreezeGuard", "resistance", StatusEffects.RESISTANCE, 0);

    public final String flag;
    public final String translation;
    public final StatusEffect effect;
    public final int amplifier;

    SoulPower(String flag, String translation, StatusEffect effect, int amplifier) {
        this.flag = flag;
        this.translation = "power.thirteenblade." + translation;
        this.effect = effect;
        this.amplifier = amplifier;
    }

    public boolean known(net.minecraft.item.ItemStack sword) {
        // 0.3 spider souls migrate from slow falling to invisibility without losing progression.
        return BladeData.has(sword, flag) || (this == SPIDER && BladeData.has(sword, BladeData.SLOW_FALL));
    }

    public static SoulPower of(LivingEntity mob) {
        if (mob instanceof ZombieEntity) return ZOMBIE;
        if (mob instanceof AbstractSkeletonEntity) return SKELETON;
        if (mob instanceof SpiderEntity) return SPIDER;
        if (mob instanceof CreeperEntity) return CREEPER;
        if (mob instanceof BlazeEntity) return BLAZE;
        if (mob instanceof WitherEntity) return WITHER;
        if (mob instanceof EnderDragonEntity) return DRAGON;
        if (mob instanceof PillagerEntity) return PILLAGER;
        if (mob instanceof PhantomEntity) return PHANTOM;
        if (mob instanceof WitchEntity) return WITCH;
        if (mob instanceof SlimeEntity) return SLIME;
        if (mob instanceof VindicatorEntity) return VINDICATOR;
        if (mob instanceof GuardianEntity) return GUARDIAN;
        if (mob instanceof WardenEntity) return WARDEN;
        if (mob instanceof ShulkerEntity) return SHULKER;
        // Breeze exists only in the 1.21.1 NeoForge edition.
        return null;
    }
}
