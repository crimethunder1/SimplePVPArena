package de.simplepvparena.arena;

import java.util.List;

public record AreaPlayerCondition(List<SavedPotionEffect> potionEffects, int foodLevel, float saturation,
                                  float exhaustion) {

    public static final int FULL_FOOD_LEVEL = 20;
    public static final float FULL_SATURATION = 20.0F;
    public static final float NO_EXHAUSTION = 0.0F;

    public AreaPlayerCondition {
        potionEffects = List.copyOf(potionEffects);
    }
}
