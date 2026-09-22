package dev.thirteenblade;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/** Families deliberately share one discovery: variants cannot farm extra toughness. */
public enum SoulPower {
    ZOMBIE("HungerWard", "hunger", null, 0),
    SKELETON("NightSight", "night", MobEffects.NIGHT_VISION, 0),
    SPIDER("SpiderVeil", "invisibility", MobEffects.INVISIBILITY, 0),
    CREEPER("CreeperShield", "shield", MobEffects.ABSORPTION, 0),
    BLAZE("BlazeWard", "fire_resistance", MobEffects.FIRE_RESISTANCE, 0),
    WITHER("WitherVitality", "regeneration", MobEffects.REGENERATION, 0),
    DRAGON("DragonFlight", "flight", null, 0),
    PILLAGER("RaidHero", "hero", MobEffects.HERO_OF_THE_VILLAGE, 4),
    PHANTOM("PhantomSpeed", "speed", MobEffects.MOVEMENT_SPEED, 0),
    WITCH("WitchHaste", "haste", MobEffects.DIG_SPEED, 0),
    SLIME("SlimeLeap", "jump", MobEffects.JUMP, 0),
    VINDICATOR("WeakeningEdge", "weakness", null, 0),
    GUARDIAN("GuardianBreath", "water_breathing", MobEffects.WATER_BREATHING, 0),
    WARDEN("DeepSight", "darkness_ward", null, 0),
    SHULKER("ShulkerSatiation", "saturation", MobEffects.SATURATION, 0),
    BREEZE("BreezeGuard", "resistance", MobEffects.DAMAGE_RESISTANCE, 0);

    public final String flag;
    public final String translation;
    public final net.minecraft.core.Holder<MobEffect> effect;
    public final int amplifier;

    SoulPower(String flag, String translation, net.minecraft.core.Holder<MobEffect> effect, int amplifier) {
        this.flag = flag;
        this.translation = "power.thirteenblade." + translation;
        this.effect = effect;
        this.amplifier = amplifier;
    }

    public boolean known(net.minecraft.world.item.ItemStack sword) {
        // 0.3 spider souls migrate from slow falling to invisibility without losing progression.
        return BladeData.has(sword, flag) || (this == SPIDER && BladeData.has(sword, BladeData.SLOW_FALL));
    }

    public static SoulPower of(LivingEntity mob) {
        if (mob instanceof Zombie) return ZOMBIE;
        if (mob instanceof AbstractSkeleton) return SKELETON;
        if (mob instanceof Spider) return SPIDER;
        if (mob instanceof Creeper) return CREEPER;
        if (mob instanceof Blaze) return BLAZE;
        if (mob instanceof WitherBoss) return WITHER;
        if (mob instanceof EnderDragon) return DRAGON;
        if (mob instanceof Pillager) return PILLAGER;
        if (mob instanceof Phantom) return PHANTOM;
        if (mob instanceof Witch) return WITCH;
        if (mob instanceof Slime) return SLIME;
        if (mob instanceof Vindicator) return VINDICATOR;
        if (mob instanceof Guardian) return GUARDIAN;
        if (mob instanceof Warden) return WARDEN;
        if (mob instanceof Shulker) return SHULKER;
        if (mob instanceof Breeze) return BREEZE;
        return null;
    }
}
