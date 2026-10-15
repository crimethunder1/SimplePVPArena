package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.TeamColourSettings;
import org.bukkit.configuration.ConfigurationSection;

public interface LiveConfiguration {

    void setRoundResetDelayTicks(long roundResetDelayTicks);

    void setRoundTimeoutSettings(RoundTimeoutSettings roundTimeoutSettings);

    void setSuddenDeathDisabledInFinal(boolean suddenDeathDisabledInFinal);

    void setStartGateDurationSeconds(int durationSeconds);

    void setTeamColourSettings(TeamColourSettings teamColourSettings);

    void setArenaCleaningBlocksPerTick(int blocksPerTick);

    void setArenaBaselineBlocksPerTick(int blocksPerTick);

    void setArenaBaselineDepthBelowFloor(int depthBelowFloor);

    void setArenaCleaningParticleSettings(ArenaCleaningParticleSettings settings);

    void setMaximumArenaCellCount(int maximumArenaCellCount);

    void setSafetyZoneRadius(int radius);

    void setBorderParticlePeriodTicks(long periodTicks);

    void setBorderParticleViewRadius(int viewRadius);

    void setBorderParticleMaximumBlocksPerPlayer(int maximumBlocksPerPlayer);

    void setBorderParticleMaximumLinesPerPlayer(int maximumLinesPerPlayer);

    void setBorderWallPreviewDurationTicks(long durationTicks);

    void setConfirmedBorderParticleSettings(ConfirmedBorderParticleSettings settings);

    void reloadKit(ConfigurationSection kitSection);
}
