package dev.thirteenblade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server-owned balance. Clients receive these values when joining. */
public final class BalanceConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public int killsPerLevel = 13;
    public double damagePerLevel = 2;
    public double healthPerLevel = 2;
    public int absorptionCooldownSeconds = 60;
    public double eliteSpawnChance = 0.05;
    public double eliteHealthMultiplier = 1.5;
    public int eliteMaxEffects = 2;
    public int eliteMaxEffectLevel = 2;
    public int maxStolenEffectLevel = 3;
    public int stolenEffectDurationSeconds = -1;

    public BalanceConfig validated() {
        killsPerLevel = Math.max(1, Math.min(10000, killsPerLevel));
        damagePerLevel = finiteClamp(damagePerLevel, 0, 20, 2);
        healthPerLevel = finiteClamp(healthPerLevel, 0, 20, 2);
        absorptionCooldownSeconds = Math.max(1, Math.min(3600, absorptionCooldownSeconds));
        eliteSpawnChance = finiteClamp(eliteSpawnChance, 0, 1, 0.05);
        eliteHealthMultiplier = finiteClamp(eliteHealthMultiplier, 1, 10, 1.5);
        eliteMaxEffects = Math.max(1, Math.min(6, eliteMaxEffects));
        eliteMaxEffectLevel = Math.max(1, Math.min(3, eliteMaxEffectLevel));
        maxStolenEffectLevel = Math.max(1, Math.min(5, maxStolenEffectLevel));
        stolenEffectDurationSeconds = stolenEffectDurationSeconds < 0 ? -1
                : Math.max(1, Math.min(86400, stolenEffectDurationSeconds));
        return this;
    }

    private static double finiteClamp(double n, double min, double max, double fallback) {
        return Double.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
    }

    public static BalanceConfig load(Path path) {
        try {
            if (Files.exists(path)) {
                BalanceConfig result = GSON.fromJson(Files.readString(path), BalanceConfig.class);
                if (result != null) return result.validated();
                throw new IOException("Configuration must be a JSON object");
            }
            BalanceConfig defaults = new BalanceConfig();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(defaults), StandardCharsets.UTF_8);
            return defaults;
        } catch (IOException | RuntimeException e) {
            ThirteenBlade.LOGGER.warn("Cannot read thirteenblade.json; using defaults: {}", e.getClass().getSimpleName());
            return new BalanceConfig();
        }
    }
}
