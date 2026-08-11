package de.simplepvparena.config;

import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.match.RoundTimeoutSettings;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RecordingLiveConfiguration implements LiveConfiguration {

    static final String ROUND_RESET_DELAY = "roundResetDelayTicks";
    static final String ROUND_TIMEOUT = "roundTimeoutSettings";
    static final String CLEANING_BLOCKS_PER_TICK = "arenaCleaningBlocksPerTick";
    static final String BASELINE_BLOCKS_PER_TICK = "arenaBaselineBlocksPerTick";
    static final String MAXIMUM_CELL_COUNT = "maximumArenaCellCount";
    static final String SAFETY_ZONE_RADIUS = "safetyZoneRadius";
    static final String PARTICLE_PERIOD = "borderParticlePeriodTicks";
    static final String PARTICLE_VIEW_RADIUS = "borderParticleViewRadius";
    static final String PARTICLE_MAXIMUM_BLOCKS = "borderParticleMaximumBlocksPerPlayer";
    static final String PARTICLE_MAXIMUM_LINES = "borderParticleMaximumLinesPerPlayer";
    static final String WALL_PREVIEW_DURATION = "borderWallPreviewDurationTicks";
    static final String CONFIRMED_PARTICLE = "confirmedBorderParticleSettings";
    static final String KIT = "kit";

    static final List<String> EVERY_APPLIED_VALUE = List.of(
            ROUND_RESET_DELAY, ROUND_TIMEOUT, CLEANING_BLOCKS_PER_TICK, BASELINE_BLOCKS_PER_TICK,
            MAXIMUM_CELL_COUNT, SAFETY_ZONE_RADIUS, PARTICLE_PERIOD, PARTICLE_VIEW_RADIUS,
            PARTICLE_MAXIMUM_BLOCKS, PARTICLE_MAXIMUM_LINES, WALL_PREVIEW_DURATION, CONFIRMED_PARTICLE, KIT);

    private final Map<String, Object> appliedValues = new LinkedHashMap<>();
    private final List<String> kitSectionKeys = new ArrayList<>();

    @Override
    public void setRoundResetDelayTicks(long roundResetDelayTicks) {
        appliedValues.put(ROUND_RESET_DELAY, roundResetDelayTicks);
    }

    @Override
    public void setRoundTimeoutSettings(RoundTimeoutSettings roundTimeoutSettings) {
        appliedValues.put(ROUND_TIMEOUT, roundTimeoutSettings);
    }

    @Override
    public void setArenaCleaningBlocksPerTick(int blocksPerTick) {
        appliedValues.put(CLEANING_BLOCKS_PER_TICK, blocksPerTick);
    }

    @Override
    public void setArenaBaselineBlocksPerTick(int blocksPerTick) {
        appliedValues.put(BASELINE_BLOCKS_PER_TICK, blocksPerTick);
    }

    @Override
    public void setMaximumArenaCellCount(int maximumArenaCellCount) {
        appliedValues.put(MAXIMUM_CELL_COUNT, maximumArenaCellCount);
    }

    @Override
    public void setSafetyZoneRadius(int radius) {
        appliedValues.put(SAFETY_ZONE_RADIUS, radius);
    }

    @Override
    public void setBorderParticlePeriodTicks(long periodTicks) {
        appliedValues.put(PARTICLE_PERIOD, periodTicks);
    }

    @Override
    public void setBorderParticleViewRadius(int viewRadius) {
        appliedValues.put(PARTICLE_VIEW_RADIUS, viewRadius);
    }

    @Override
    public void setBorderParticleMaximumBlocksPerPlayer(int maximumBlocksPerPlayer) {
        appliedValues.put(PARTICLE_MAXIMUM_BLOCKS, maximumBlocksPerPlayer);
    }

    @Override
    public void setBorderParticleMaximumLinesPerPlayer(int maximumLinesPerPlayer) {
        appliedValues.put(PARTICLE_MAXIMUM_LINES, maximumLinesPerPlayer);
    }

    @Override
    public void setBorderWallPreviewDurationTicks(long durationTicks) {
        appliedValues.put(WALL_PREVIEW_DURATION, durationTicks);
    }

    @Override
    public void setConfirmedBorderParticleSettings(ConfirmedBorderParticleSettings settings) {
        appliedValues.put(CONFIRMED_PARTICLE, settings);
    }

    @Override
    public void reloadKit(ConfigurationSection kitSection) {
        appliedValues.put(KIT, kitSection == null ? "no kit section" : kitSection.getCurrentPath());
        kitSectionKeys.clear();
        if (kitSection != null) {
            kitSectionKeys.addAll(kitSection.getKeys(false));
        }
    }

    Object valueOf(String name) {
        return appliedValues.get(name);
    }

    long longValueOf(String name) {
        return ((Number) appliedValues.get(name)).longValue();
    }

    int intValueOf(String name) {
        return ((Number) appliedValues.get(name)).intValue();
    }

    RoundTimeoutSettings roundTimeoutSettings() {
        return (RoundTimeoutSettings) appliedValues.get(ROUND_TIMEOUT);
    }

    ConfirmedBorderParticleSettings confirmedBorderParticleSettings() {
        return (ConfirmedBorderParticleSettings) appliedValues.get(CONFIRMED_PARTICLE);
    }

    List<String> kitSectionKeys() {
        return List.copyOf(kitSectionKeys);
    }

    List<String> appliedValueNames() {
        return List.copyOf(appliedValues.keySet());
    }
}
