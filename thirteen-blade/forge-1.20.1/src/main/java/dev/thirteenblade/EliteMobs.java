package dev.thirteenblade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;

public final class EliteMobs {
    public static final String ELITE_TAG = "thirteenblade.elite";
    private static final String CANDIDATE_TAG = "thirteenblade.elite_candidate", CHECKED_TAG = "thirteenblade.spawn_checked";
    private static final java.util.UUID HEALTH_ID = java.util.UUID.fromString("03b27fe1-4b61-4b40-a4df-f58e98f4f501");
    private static final List<MobEffect> POOL = List.of(MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST,
            MobEffects.DAMAGE_RESISTANCE, MobEffects.REGENERATION, MobEffects.FIRE_RESISTANCE, MobEffects.ABSORPTION);
    private EliteMobs() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((FinalizeSpawn event) -> markSpawn(event.getEntity(), event.getSpawnType()));
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) processSpawn(mob);
        });
    }
    public static void markSpawn(Mob mob, MobSpawnType reason) {
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if ((reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) && !mob.getTags().contains(CHECKED_TAG)) {
            mob.addTag(CHECKED_TAG); mob.addTag(CANDIDATE_TAG);
        }
    }
    public static void processSpawn(Mob mob) {
        if (mob.getTags().remove(CANDIDATE_TAG) && eligible(mob)
                && mob.getRandom().nextDouble() < ThirteenBlade.balance.eliteSpawnChance) empower(mob);
    }
    public static boolean eligible(Mob mob) {
        return mob.getType().getCategory() == MobCategory.MONSTER && !mob.isBaby()
                && mob.getType() != EntityType.WITHER && mob.getType() != EntityType.ENDER_DRAGON
                && mob.getType() != EntityType.WARDEN && !mob.getTags().contains(ELITE_TAG);
    }
    private static void empower(Mob mob) {
        mob.addTag(ELITE_TAG);
        mob.setGlowingTag(true);
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH_ID) == null) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_ID, "Thirteen Blade elite", ThirteenBlade.balance.eliteHealthMultiplier - 1,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        List<MobEffect> choices = new ArrayList<>(POOL);
        choices.removeIf(effect -> !mob.canBeAffected(new MobEffectInstance(effect, -1)));
        int count = Math.min(choices.size(), 1 + mob.getRandom().nextInt(ThirteenBlade.balance.eliteMaxEffects));
        for (int i = 0; i < count; i++) {
            var effect = choices.remove(mob.getRandom().nextInt(choices.size()));
            int amp = effect == MobEffects.FIRE_RESISTANCE ? 0 : mob.getRandom().nextInt(ThirteenBlade.balance.eliteMaxEffectLevel);
            mob.addEffect(new MobEffectInstance(effect, -1, amp, false, true, true));
        }
    }
}
