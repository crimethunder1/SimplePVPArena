package de.simplepvparena.border;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public final class VanillaBorderVisualEffect implements BorderVisualEffect {

    public static final Particle BORDER_BLOCK_PARTICLE = Particle.END_ROD;
    public static final Particle CONNECTION_PARTICLE = Particle.ELECTRIC_SPARK;

    private static final int STREAM_PARTICLE_COUNT = 5;
    private static final double STREAM_STEP_HEIGHT = 0.5D;
    private static final double STREAM_RISE_SPEED = 0.02D;
    private static final int DIRECTED_PARTICLE_COUNT = 0;
    private static final int SINGLE_PARTICLE_COUNT = 1;
    private static final double NO_OFFSET = 0.0D;

    @Override
    public void showAt(Player player, Location borderBlockCenter) {
        for (int step = 0; step < STREAM_PARTICLE_COUNT; step++) {
            Location streamPoint = borderBlockCenter.clone().add(NO_OFFSET, step * STREAM_STEP_HEIGHT, NO_OFFSET);
            player.spawnParticle(BORDER_BLOCK_PARTICLE, streamPoint, DIRECTED_PARTICLE_COUNT,
                    NO_OFFSET, 1.0D, NO_OFFSET, STREAM_RISE_SPEED);
        }
    }

    @Override
    public void showConnection(Player player, Location pointBetweenBorderBlocks) {
        player.spawnParticle(CONNECTION_PARTICLE, pointBetweenBorderBlocks, SINGLE_PARTICLE_COUNT,
                NO_OFFSET, NO_OFFSET, NO_OFFSET, NO_OFFSET);
    }
}
