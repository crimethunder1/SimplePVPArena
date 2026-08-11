package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.match.RoundTimeoutSettings;
import org.bukkit.configuration.ConfigurationSection;

public interface LiveConfiguration {

    void setRoundResetDelayTicks(long roundResetDelayTicks);

    void setRoundTimeoutSettings(RoundTimeoutSettings roundTimeoutSettings);

    void setArenaCleaningBlocksPerTick(int blocksPerTick);

    void setArenaBaselineBlocksPerTick(int blocksPerTick);

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
