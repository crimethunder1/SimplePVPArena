package de.simplepvparena.match;

public record RoundTimeoutSettings(int roundTimeoutSeconds,
                                   int suddenDeathWarningSeconds,
                                   double suddenDeathBaseDamage,
                                   double suddenDeathDamageIncreasePerSecond) {

    public static final int DEFAULT_ROUND_TIMEOUT_SECONDS = 300;
    public static final int DEFAULT_SUDDEN_DEATH_WARNING_SECONDS = 45;
    public static final double DEFAULT_SUDDEN_DEATH_BASE_DAMAGE = 2.0D;
    public static final double DEFAULT_SUDDEN_DEATH_DAMAGE_INCREASE_PER_SECOND = 1.0D;

    public RoundTimeoutSettings {
        if (roundTimeoutSeconds < 1) {
            throw new IllegalArgumentException("roundTimeoutSeconds must be positive");
        }
        suddenDeathWarningSeconds = Math.max(0, Math.min(suddenDeathWarningSeconds, roundTimeoutSeconds));
        suddenDeathBaseDamage = Math.max(0.0D, suddenDeathBaseDamage);
        suddenDeathDamageIncreasePerSecond = Math.max(0.0D, suddenDeathDamageIncreasePerSecond);
    }

    public static RoundTimeoutSettings defaults() {
        return new RoundTimeoutSettings(DEFAULT_ROUND_TIMEOUT_SECONDS,
                DEFAULT_SUDDEN_DEATH_WARNING_SECONDS,
                DEFAULT_SUDDEN_DEATH_BASE_DAMAGE,
                DEFAULT_SUDDEN_DEATH_DAMAGE_INCREASE_PER_SECOND);
    }

    public double damageForSuddenDeathSecond(int suddenDeathSecond) {
        if (suddenDeathSecond < 1) {
            return 0.0D;
        }
        return suddenDeathBaseDamage + suddenDeathDamageIncreasePerSecond * (suddenDeathSecond - 1);
    }
}
