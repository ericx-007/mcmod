package dev.thirteenblade;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.boss.*;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.*;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class EliteMobs {
    private static final String CHECKED = "thirteenblade.spawn_checked", CANDIDATE = "thirteenblade.elite_candidate", ELITE = "thirteenblade.elite";
    private static final UUID HEALTH = UUID.fromString("03b27fe1-4b61-4b40-a4df-f58e98f4f501");
    private static final List<Potion> POOL = Arrays.asList(MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.RESISTANCE, MobEffects.REGENERATION, MobEffects.FIRE_RESISTANCE, MobEffects.ABSORPTION);
    @SubscribeEvent public void check(LivingSpawnEvent.CheckSpawn event) {
        if (!event.isSpawner() && !event.getWorld().isRemote && !event.getEntityLiving().getEntityData().getBoolean(CHECKED))
            event.getEntityLiving().getEntityData().setBoolean(CANDIDATE, true);
    }
    @SubscribeEvent public void join(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving)event.getEntity();
        if (!mob.getEntityData().getBoolean(CANDIDATE)) return;
        mob.getEntityData().removeTag(CANDIDATE);
        if (mob.getEntityData().getBoolean(CHECKED)) return;
        mob.getEntityData().setBoolean(CHECKED, true);
        if (eligible(mob) && mob.getRNG().nextDouble() < ThirteenBlade.balance.eliteSpawnChance) empower(mob);
    }
    public static boolean eligible(EntityLiving mob) {
        return mob.isCreatureType(EnumCreatureType.MONSTER, false) && !mob.isChild() && !(mob instanceof EntityWither) && !(mob instanceof EntityDragon)
                && mob.isNonBoss() && !mob.getEntityData().getBoolean(ELITE);
    }
    public static void empower(EntityLiving mob) {
        mob.getEntityData().setBoolean(ELITE, true); mob.setGlowing(true);
        IAttributeInstance health = mob.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH) == null) {
            health.applyModifier(new AttributeModifier(HEALTH, "Thirteen Blade elite", ThirteenBlade.balance.eliteHealthMultiplier - 1, 2));
            mob.setHealth(mob.getMaxHealth());
        }
        List<Potion> choices = new ArrayList<>(POOL); choices.removeIf(p -> !mob.isPotionApplicable(new PotionEffect(p, 200)));
        int count = Math.min(choices.size(), 1 + mob.getRNG().nextInt(ThirteenBlade.balance.eliteMaxEffects));
        for (int i = 0; i < count; i++) {
            Potion type = choices.remove(mob.getRNG().nextInt(choices.size()));
            int amp = type == MobEffects.FIRE_RESISTANCE ? 0 : mob.getRNG().nextInt(ThirteenBlade.balance.eliteMaxEffectLevel);
            mob.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amp));
        }
    }
}
