package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.tournament.AutoMatchSettings;
import de.simplepvparena.visual.CustomVisualSettings;

public interface LiveConfiguration {

    void setLanguage(String language);

    void setRoundResetDelayTicks(long roundResetDelayTicks);

    void setRoundTimeoutSettings(RoundTimeoutSettings roundTimeoutSettings);

    void setSuddenDeathDisabledInFinal(boolean suddenDeathDisabledInFinal);

    void setStartGateDurationSeconds(int durationSeconds);

    void setCountdownSeconds(int countdownSeconds);

    void setReconnectSeconds(int reconnectSeconds);

    void setTeamColourSettings(TeamColourSettings teamColourSettings);

    void setTeamSize(int teamSize);

    void setTeamSizeAtStart(int teamSize);

    void setAutoMatchSettings(AutoMatchSettings autoMatchSettings);

    void setArenaCleaningBlocksPerTick(int blocksPerTick);

    void setArenaBaselineBlocksPerTick(int blocksPerTick);

    void setArenaBaselineDepthBelowFloor(int depthBelowFloor);

    void setArenaCleaningParticleSettings(ArenaCleaningParticleSettings settings);

    void setMaximumArenaCellCount(int maximumArenaCellCount);

    void setSafetyZoneRadius(int radius);

    void setArenaEffectMargin(int margin);

    void setTeleportOnJoin(boolean teleportOnJoin);

    void setBorderParticlePeriodTicks(long periodTicks);

    void setBorderParticleViewRadius(int viewRadius);

    void setBorderParticleMaximumBlocksPerPlayer(int maximumBlocksPerPlayer);

    void setBorderParticleMaximumLinesPerPlayer(int maximumLinesPerPlayer);

    void setBorderWallPreviewDurationTicks(long durationTicks);

    void setConfirmedBorderParticleSettings(ConfirmedBorderParticleSettings settings);

    void setConfirmedBorderParticleEnabled(boolean enabled);

    void setAreaInventoryEnabled(boolean enabled);

    void setCustomVisualSettings(CustomVisualSettings settings);
}
