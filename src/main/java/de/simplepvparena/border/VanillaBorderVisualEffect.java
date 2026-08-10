package de.simplepvparena.border;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public final class VanillaBorderVisualEffect implements BorderVisualEffect {

    public static final Particle BORDER_BLOCK_PARTICLE = Particle.END_ROD;
    public static final Particle COLOURED_BORDER_BLOCK_PARTICLE = Particle.DUST;
    public static final Particle CONNECTION_PARTICLE = Particle.ELECTRIC_SPARK;

    private static final float COLOURED_PARTICLE_SIZE = 1.0F;
    private static final Particle.DustOptions ISOLATED_COLUMN_DUST =
            new Particle.DustOptions(Color.fromRGB(255, 60, 60), COLOURED_PARTICLE_SIZE);
    private static final Particle.DustOptions CONNECTED_COLUMNS_DUST =
            new Particle.DustOptions(Color.fromRGB(60, 255, 90), COLOURED_PARTICLE_SIZE);

    private static final int STREAM_PARTICLE_COUNT = 5;
    private static final double STREAM_STEP_HEIGHT = 0.5D;
    private static final double STREAM_RISE_SPEED = 0.02D;
    private static final int DIRECTED_PARTICLE_COUNT = 0;
    private static final int SINGLE_PARTICLE_COUNT = 1;
    private static final double NO_OFFSET = 0.0D;
    private static final double NO_SPEED = 0.0D;

    @Override
    public void showAt(Player player, Location borderBlockCenter, BorderConnectivity connectivity) {
        Particle.DustOptions columnColour = colourOf(connectivity);
        for (int step = 0; step < STREAM_PARTICLE_COUNT; step++) {
            Location streamPoint = borderBlockCenter.clone().add(NO_OFFSET, step * STREAM_STEP_HEIGHT, NO_OFFSET);
            if (columnColour == null) {
                player.spawnParticle(BORDER_BLOCK_PARTICLE, streamPoint, DIRECTED_PARTICLE_COUNT,
                        NO_OFFSET, 1.0D, NO_OFFSET, STREAM_RISE_SPEED);
            } else {
                player.spawnParticle(COLOURED_BORDER_BLOCK_PARTICLE, streamPoint, SINGLE_PARTICLE_COUNT,
                        NO_OFFSET, NO_OFFSET, NO_OFFSET, NO_SPEED, columnColour);
            }
        }
    }

    @Override
    public void showColumnConnection(Player player, Location pointBetweenBorderColumns) {
        player.spawnParticle(COLOURED_BORDER_BLOCK_PARTICLE, pointBetweenBorderColumns, SINGLE_PARTICLE_COUNT,
                NO_OFFSET, NO_OFFSET, NO_OFFSET, NO_SPEED, CONNECTED_COLUMNS_DUST);
    }

    @Override
    public void showConnection(Player player, Location pointBetweenBorderBlocks) {
        player.spawnParticle(CONNECTION_PARTICLE, pointBetweenBorderBlocks, SINGLE_PARTICLE_COUNT,
                NO_OFFSET, NO_OFFSET, NO_OFFSET, NO_OFFSET);
    }

    static Particle.DustOptions colourOf(BorderConnectivity connectivity) {
        return switch (connectivity) {
            case ISOLATED -> ISOLATED_COLUMN_DUST;
            case NORMAL, WELL_CONNECTED -> null;
        };
    }
}
