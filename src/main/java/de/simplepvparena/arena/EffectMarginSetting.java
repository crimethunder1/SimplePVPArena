package de.simplepvparena.arena;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntConsumer;

public final class EffectMarginSetting {

    public static final int LARGE_STEP = 8;
    public static final int SMALL_STEP = 1;

    private final ArenaRegistry arenaRegistry;
    private final IntConsumer marginApplier;

    public EffectMarginSetting(ArenaRegistry arenaRegistry, IntConsumer marginApplier) {
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.marginApplier = Objects.requireNonNull(marginApplier, "marginApplier");
    }

    public int getMargin() {
        return arenaRegistry.getEffectMargin();
    }

    public Optional<EffectArea> findEffectArea() {
        return arenaRegistry.findEffectArea();
    }

    public boolean canGetSmaller() {
        return getMargin() > EffectArea.MINIMUM_MARGIN;
    }

    public boolean canGetLarger() {
        return getMargin() < EffectArea.MAXIMUM_MARGIN;
    }

    public int findSmallerMargin(int step) {
        return Math.max(EffectArea.MINIMUM_MARGIN, getMargin() - step);
    }

    public int findLargerMargin(int step) {
        return Math.min(EffectArea.MAXIMUM_MARGIN, getMargin() + step);
    }

    public void apply(int margin) {
        marginApplier.accept(margin);
    }

    public static int findStep(boolean shiftClick) {
        return shiftClick ? SMALL_STEP : LARGE_STEP;
    }
}
