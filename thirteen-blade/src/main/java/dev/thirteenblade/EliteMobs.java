package dev.thirteenblade;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;

/** Enhance a small fraction of new natural monsters; loading a saved entity never rerolls it. */
public final class EliteMobs {
    public static final String ELITE_TAG = "thirteenblade.elite";
    private static final String CANDIDATE_TAG = "thirteenblade.elite_candidate";
    private static final String CHECKED_TAG = "thirteenblade.spawn_checked";
    private static final UUID HEALTH_ID = UUID.fromString("dd9069d9-78c2-42ab-84d3-f3f9f828b680");
    private static final List<StatusEffect> POOL = List.of(StatusEffects.SPEED, StatusEffects.STRENGTH,
            StatusEffects.RESISTANCE, StatusEffects.REGENERATION, StatusEffects.FIRE_RESISTANCE, StatusEffects.ABSORPTION);

    private EliteMobs() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob && mob.getCommandTags().contains(CANDIDATE_TAG)) {
                mob.getCommandTags().remove(CANDIDATE_TAG);
                if (eligible(mob) && mob.getRandom().nextDouble() < ThirteenBlade.balance.eliteSpawnChance)
                    empower(mob);
            }
        });
    }

    public static void markSpawn(MobEntity mob, SpawnReason reason) {
        if (mob.getType().getSpawnGroup() != SpawnGroup.MONSTER) return;
        if ((reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION)
                && !mob.getCommandTags().contains(CHECKED_TAG)) {
            mob.addCommandTag(CHECKED_TAG);
            mob.addCommandTag(CANDIDATE_TAG);
        }
    }

    public static boolean eligible(MobEntity mob) {
        return mob.getType().getSpawnGroup() == SpawnGroup.MONSTER && !mob.isBaby()
                && mob.getType() != EntityType.WITHER && mob.getType() != EntityType.ENDER_DRAGON
                && mob.getType() != EntityType.WARDEN && !mob.getCommandTags().contains(ELITE_TAG);
    }

    private static void empower(MobEntity mob) {
        mob.addCommandTag(ELITE_TAG);
        // An outline identifies elites without using custom names, which can prevent natural despawning.
        mob.setGlowing(true);
        var health = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH_ID) == null) {
            health.addPersistentModifier(new EntityAttributeModifier(HEALTH_ID, "Thirteen Blade elite vitality",
                    ThirteenBlade.balance.eliteHealthMultiplier - 1, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        List<StatusEffect> choices = new ArrayList<>(POOL);
        // Undead reject Regeneration. Select only effects that actually work on this species.
        choices.removeIf(effect -> !mob.canHaveStatusEffect(new StatusEffectInstance(effect, -1)));
        int count = Math.min(choices.size(), 1 + mob.getRandom().nextInt(ThirteenBlade.balance.eliteMaxEffects));
        for (int i = 0; i < count; i++) {
            StatusEffect effect = choices.remove(mob.getRandom().nextInt(choices.size()));
            int amplifier = effect == StatusEffects.FIRE_RESISTANCE ? 0
                    : mob.getRandom().nextInt(ThirteenBlade.balance.eliteMaxEffectLevel);
            mob.addStatusEffect(new StatusEffectInstance(effect, -1, amplifier, false, true, true));
        }
    }
}
