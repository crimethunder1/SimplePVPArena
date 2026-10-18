package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaCleaningVisualEffect;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class PluginLiveConfiguration implements LiveConfiguration {

    private final MatchService matchService;
    private final StartGateDuration startGateDuration;
    private final FighterColours fighterColours;
    private final ArenaCleaningService arenaCleaningService;
    private final ArenaCleaningVisualEffect arenaCleaningVisualEffect;
    private final ArenaBaselineService arenaBaselineService;
    private final ArenaSetupService arenaSetupService;
    private final ArenaSafetyZone arenaSafetyZone;
    private final ArenaRegistry arenaRegistry;
    private final BorderParticleService borderParticleService;
    private final BorderWallPreview borderWallPreview;
    private final BorderVisualEffect borderVisualEffect;
    private final TeamSizeSetting teamSizeSetting;
    private final TeamRegistry teamRegistry;
    private final BooleanSupplier tournamentInProgress;
    private final Consumer<String> adminNotifier;
    private final Consumer<String> problemReporter;
    private final Consumer<String> languageApplier;

    public PluginLiveConfiguration(MatchService matchService,
                                   StartGateDuration startGateDuration,
                                   FighterColours fighterColours,
                                   ArenaCleaningService arenaCleaningService,
                                   ArenaCleaningVisualEffect arenaCleaningVisualEffect,
                                   ArenaBaselineService arenaBaselineService,
                                   ArenaSetupService arenaSetupService,
                                   ArenaSafetyZone arenaSafetyZone,
                                   ArenaRegistry arenaRegistry,
                                   BorderParticleService borderParticleService,
                                   BorderWallPreview borderWallPreview,
                                   BorderVisualEffect borderVisualEffect,
                                   TeamSizeSetting teamSizeSetting,
                                   TeamRegistry teamRegistry,
                                   BooleanSupplier tournamentInProgress,
                                   Consumer<String> adminNotifier,
                                   Consumer<String> problemReporter,
                                   Consumer<String> languageApplier) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.startGateDuration = Objects.requireNonNull(startGateDuration, "startGateDuration");
        this.fighterColours = Objects.requireNonNull(fighterColours, "fighterColours");
        this.arenaCleaningService = Objects.requireNonNull(arenaCleaningService, "arenaCleaningService");
        this.arenaCleaningVisualEffect = Objects.requireNonNull(arenaCleaningVisualEffect,
                "arenaCleaningVisualEffect");
        this.arenaBaselineService = Objects.requireNonNull(arenaBaselineService, "arenaBaselineService");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.arenaSafetyZone = Objects.requireNonNull(arenaSafetyZone, "arenaSafetyZone");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.borderParticleService = Objects.requireNonNull(borderParticleService, "borderParticleService");
        this.borderWallPreview = Objects.requireNonNull(borderWallPreview, "borderWallPreview");
        this.borderVisualEffect = Objects.requireNonNull(borderVisualEffect, "borderVisualEffect");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
        this.problemReporter = Objects.requireNonNull(problemReporter, "problemReporter");
        this.languageApplier = Objects.requireNonNull(languageApplier, "languageApplier");
    }

    @Override
    public void setLanguage(String language) {
        languageApplier.accept(language);
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
    public void setTeamSize(int teamSize) {
        int currentTeamSize = teamSizeSetting.getTeamSize();
        if (teamSize == currentTeamSize) {
            return;
        }
        Optional<String> rejection = TeamSizeSetting.findChangeRejection(currentTeamSize, teamSize,
                tournamentInProgress.getAsBoolean(), teamRegistry);
        if (rejection.isPresent()) {
            String warning = rejection.get() + ". The team size " + teamSize + " of " + PluginSettings.TEAM_SIZE_PATH
                    + " in config.yml was not taken over, the team size stays "
                    + TeamSizeSetting.describeMatchUp(currentTeamSize);
            problemReporter.accept(warning);
            adminNotifier.accept(warning);
            return;
        }
        teamSizeSetting.setTeamSize(teamSize);
    }

    @Override
    public void setTeamSizeAtStart(int teamSize) {
        if (TeamSizeSetting.isAllowed(teamSize)) {
            teamSizeSetting.setTeamSize(teamSize);
        }
        int appliedTeamSize = teamSizeSetting.getTeamSize();
        TeamSizeSetting.describeTeamsLargerThan(appliedTeamSize, teamRegistry).ifPresent(largerTeams -> {
            String warning = largerTeams + ". The team size " + appliedTeamSize + " of "
                    + PluginSettings.TEAM_SIZE_PATH + " in config.yml applies anyway since the start, these teams "
                    + "keep their players and every online player of them fights";
            problemReporter.accept(warning);
            adminNotifier.accept(warning);
        });
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
    public void setArenaBaselineDepthBelowFloor(int depthBelowFloor) {
        arenaBaselineService.setDepthBelowFloor(depthBelowFloor);
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
    public void setArenaEffectMargin(int margin) {
        arenaRegistry.setEffectMargin(margin);
        arenaRegistry.findTribuneOutsideOfTheEffectArea().ifPresent(warning -> problemReporter.accept(warning
                + ". Nothing was dropped, set the tribune spawn again closer to the fight area or raise "
                + EffectArea.MARGIN_CONFIGURATION_PATH + " in config.yml"));
    }

    @Override
    public void setTeleportOnJoin(boolean teleportOnJoin) {
        arenaRegistry.setTeleportOnJoin(teleportOnJoin);
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
}
