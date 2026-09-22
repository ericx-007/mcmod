package dev.thirteenblade;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionTest {
    @Test void thirteenKillsAreNeededBeforeAnyBonus() {
        BalanceConfig config = new BalanceConfig();
        assertEquals(0, Progression.level(12, config));
        assertEquals(1, Progression.remaining(12, config));
        assertEquals(1, Progression.level(13, config));
        assertEquals(13, Progression.remaining(13, config));
        assertEquals(2, Progression.level(26, config));
    }

    @Test void growthContinuesBeyondTenLevels() {
        BalanceConfig config = new BalanceConfig();
        assertEquals(10, Progression.level(130, config));
        assertEquals(11, Progression.level(143, config));
        assertEquals(100, Progression.level(1300, config));
        assertEquals(7692, Progression.level(100000, config));
        assertEquals(13, Progression.remaining(130, config));
        assertEquals(131, Progression.nextKill(130));
        assertEquals(26, 6 + Progression.level(130, config) * config.damagePerLevel);
        assertEquals(40, 20 + Progression.level(130, config) * config.healthPerLevel);
        assertEquals(206, 6 + Progression.level(1300, config) * config.damagePerLevel);
        assertEquals(220, 20 + Progression.level(1300, config) * config.healthPerLevel);
    }

    @Test void oldConfigurationCannotReintroduceTheRemovedLevelCap() {
        BalanceConfig config = new com.google.gson.Gson().fromJson("{\"maxLevel\":10,\"killsPerLevel\":13}", BalanceConfig.class).validated();
        assertEquals(100, Progression.level(1300, config));
    }

    @Test void importedNegativeOrOverflowedKillDataCannotCreateBonuses() {
        BalanceConfig config = new BalanceConfig();
        assertEquals(0, Progression.level(Integer.MIN_VALUE, config));
        assertEquals(13, Progression.remaining(-1, config));
        assertEquals(1, Progression.nextKill(-100));
        assertEquals(Integer.MAX_VALUE, Progression.nextKill(Integer.MAX_VALUE));
    }

    @Test void invalidBalanceCannotDivideByZeroOrPoisonEntityAttributes() {
        BalanceConfig config = new BalanceConfig();
        config.killsPerLevel = 0;
        config.damagePerLevel = Double.NaN;
        config.healthPerLevel = Double.POSITIVE_INFINITY;
        config.absorptionWindowSeconds = -1;
        config.absorptionCooldownSeconds = 0;
        config.validated();
        assertEquals(1, config.killsPerLevel);
        assertEquals(2, config.damagePerLevel);
        assertEquals(2, config.healthPerLevel);
        assertEquals(1, config.absorptionWindowSeconds);
        assertEquals(1, config.absorptionCooldownSeconds);
        assertEquals(10, Progression.level(10, config));
    }

    @Test void changingBalanceReinterpretsExistingKillsWithoutDestroyingThem() {
        BalanceConfig config = new BalanceConfig();
        int savedKills = 39;
        assertEquals(3, Progression.level(savedKills, config));
        config.killsPerLevel = 20;
        assertEquals(1, Progression.level(savedKills, config));
        assertEquals(1, Progression.remaining(savedKills, config));
    }

    @Test void eliteAndStolenEffectSettingsHaveSafeBoundsAndInfiniteDefault() {
        BalanceConfig config = new BalanceConfig();
        assertEquals(-1, config.stolenEffectDurationSeconds);
        config.eliteSpawnChance = Double.NaN;
        config.eliteHealthMultiplier = Double.POSITIVE_INFINITY;
        config.eliteMaxEffects = Integer.MAX_VALUE;
        config.eliteMaxEffectLevel = -1;
        config.maxStolenEffectLevel = 999;
        config.stolenEffectDurationSeconds = 0;
        config.validated();
        assertEquals(0.05, config.eliteSpawnChance);
        assertEquals(1.5, config.eliteHealthMultiplier);
        assertEquals(6, config.eliteMaxEffects);
        assertEquals(1, config.eliteMaxEffectLevel);
        assertEquals(5, config.maxStolenEffectLevel);
        assertEquals(1, config.stolenEffectDurationSeconds);
        config.eliteSpawnChance = 0;
        config.stolenEffectDurationSeconds = -100;
        config.validated();
        assertEquals(0, config.eliteSpawnChance);
        assertEquals(-1, config.stolenEffectDurationSeconds);
    }
}
