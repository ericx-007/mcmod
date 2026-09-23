package dev.thirteenblade;

/** Pure progression rules, deliberately independent of Minecraft state. */
public final class Progression {
    private Progression() {}

    public static int level(int kills, BalanceConfig config) {
        return Math.max(0, kills) / config.killsPerLevel;
    }

    public static int nextKill(int kills) {
        return kills == Integer.MAX_VALUE ? kills : Math.max(0, kills) + 1;
    }

    public static int remaining(int kills, BalanceConfig config) {
        return config.killsPerLevel - Math.max(0, kills) % config.killsPerLevel;
    }
}
