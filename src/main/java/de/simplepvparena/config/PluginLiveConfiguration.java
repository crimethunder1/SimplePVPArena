package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaCleaningVisualEffect;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.kit.KitConfigurationLoader;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.TeamColourSettings;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;
import java.util.function.Consumer;

public final class PluginLiveConfiguration implements LiveConfiguration {

    private final KitService kitService;
    private final KitConfigurationLoader kitConfigurationLoader;
    private final MatchService matchService;
    private final StartGateDuration startGateDuration;
    private final FighterColours fighterColours;
    private final ArenaCleaningService arenaCleaningService;
    private final ArenaCleaningVisualEffect arenaCleaningVisualEffect;
    private final ArenaBaselineService arenaBaselineService;
    private final ArenaSetupService arenaSetupService;
    private final ArenaSafetyZone arenaSafetyZone;
    private final BorderParticleService borderParticleService;
    private final BorderWallPreview borderWallPreview;
    private final BorderVisualEffect borderVisualEffect;
    private final Consumer<String> problemReporter;

    public PluginLiveConfiguration(KitService kitService,
                                   KitConfigurationLoader kitConfigurationLoader,
                                   MatchService matchService,
                                   StartGateDuration startGateDuration,
                                   FighterColours fighterColours,
                                   ArenaCleaningService arenaCleaningService,
                                   ArenaCleaningVisualEffect arenaCleaningVisualEffect,
                                   ArenaBaselineService arenaBaselineService,
                                   ArenaSetupService arenaSetupService,
                                   ArenaSafetyZone arenaSafetyZone,
                                   BorderParticleService borderParticleService,
                                   BorderWallPreview borderWallPreview,
                                   BorderVisualEffect borderVisualEffect,
                                   Consumer<String> problemReporter) {
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.kitConfigurationLoader = Objects.requireNonNull(kitConfigurationLoader, "kitConfigurationLoader");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.startGateDuration = Objects.requireNonNull(startGateDuration, "startGateDuration");
        this.fighterColours = Objects.requireNonNull(fighterColours, "fighterColours");
        this.arenaCleaningService = Objects.requireNonNull(arenaCleaningService, "arenaCleaningService");
        this.arenaCleaningVisualEffect = Objects.requireNonNull(arenaCleaningVisualEffect,
                "arenaCleaningVisualEffect");
        this.arenaBaselineService = Objects.requireNonNull(arenaBaselineService, "arenaBaselineService");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.arenaSafetyZone = Objects.requireNonNull(arenaSafetyZone, "arenaSafetyZone");
        this.borderParticleService = Objects.requireNonNull(borderParticleService, "borderParticleService");
        this.borderWallPreview = Objects.requireNonNull(borderWallPreview, "borderWallPreview");
        this.borderVisualEffect = Objects.requireNonNull(borderVisualEffect, "borderVisualEffect");
        this.problemReporter = Objects.requireNonNull(problemReporter, "problemReporter");
    }

    @Override
    public void setRoundResetDelayTicks(long roundResetDelayTicks) {
        matchService.setRoundResetDelayTicks(roundResetDelayTicks);
    }

    @Override
    public void setRoundTimeoutSettings(RoundTimeoutSettings roundTimeoutSettings) {
        matchService.setRoundTimeoutSettings(roundTimeoutSettings);
    }

    @Override
    public void setSuddenDeathDisabledInFinal(boolean suddenDeathDisabledInFinal) {
        matchService.setSuddenDeathDisabledInFinal(suddenDeathDisabledInFinal);
    }

    @Override
    public void setStartGateDurationSeconds(int durationSeconds) {
        startGateDuration.setSeconds(durationSeconds);
    }

    @Override
    public void setTeamColourSettings(TeamColourSettings teamColourSettings) {
        fighterColours.applySettings(teamColourSettings);
    }

    @Override
    public void setArenaCleaningBlocksPerTick(int blocksPerTick) {
        arenaCleaningService.setBlocksPerTick(blocksPerTick);
    }

    @Override
    public void setArenaBaselineBlocksPerTick(int blocksPerTick) {
        arenaBaselineService.setBlocksPerTick(blocksPerTick);
    }

    @Override
    public void setArenaCleaningParticleSettings(ArenaCleaningParticleSettings settings) {
        arenaCleaningVisualEffect.applyParticleSettings(settings, problemReporter);
        arenaCleaningService.setAnimationDurationTicks(settings.animationDurationTicks());
    }

    @Override
    public void setMaximumArenaCellCount(int maximumArenaCellCount) {
        arenaSetupService.setMaximumArenaCellCount(maximumArenaCellCount);
    }

    @Override
    public void setSafetyZoneRadius(int radius) {
        arenaSafetyZone.setRadius(radius);
    }

    @Override
    public void setBorderParticlePeriodTicks(long periodTicks) {
        borderParticleService.setPeriodTicks(periodTicks);
    }

    @Override
    public void setBorderParticleViewRadius(int viewRadius) {
        borderParticleService.setViewRadius(viewRadius);
    }

    @Override
    public void setBorderParticleMaximumBlocksPerPlayer(int maximumBlocksPerPlayer) {
        borderParticleService.setMaximumBlocksPerPlayer(maximumBlocksPerPlayer);
    }

    @Override
    public void setBorderParticleMaximumLinesPerPlayer(int maximumLinesPerPlayer) {
        borderParticleService.setMaximumLinesPerPlayer(maximumLinesPerPlayer);
    }

    @Override
    public void setBorderWallPreviewDurationTicks(long durationTicks) {
        borderWallPreview.setDurationTicks(durationTicks);
    }

    @Override
    public void setConfirmedBorderParticleSettings(ConfirmedBorderParticleSettings settings) {
        borderVisualEffect.applyConfirmedParticleSettings(settings, problemReporter);
    }

    @Override
    public void reloadKit(ConfigurationSection kitSection) {
        kitService.setKitDefinition(kitConfigurationLoader.load(kitSection));
    }
}
