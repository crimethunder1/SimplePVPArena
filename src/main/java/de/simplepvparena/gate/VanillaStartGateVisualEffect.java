package de.simplepvparena.gate;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Particle;
import org.bukkit.World;

public final class VanillaStartGateVisualEffect implements StartGateVisualEffect {

    public static final Particle GATE_PARTICLE = Particle.POOF;

    private static final int DOTS_PER_BLOCK = 6;

    private static final double BLOCK_CENTRE = 0.5D;

    private static final double SPREAD = 0.25D;

    private static final double DRIFT_SPEED = 0.02D;

    @Override
    public void showRemovedLayer(World world, long[] encodedPositions) {
        for (long encodedPosition : encodedPositions) {
            world.spawnParticle(GATE_PARTICLE,
                    BlockPositionCodec.decodeX(encodedPosition) + BLOCK_CENTRE,
                    BlockPositionCodec.decodeY(encodedPosition) + BLOCK_CENTRE,
                    BlockPositionCodec.decodeZ(encodedPosition) + BLOCK_CENTRE,
                    DOTS_PER_BLOCK, SPREAD, SPREAD, SPREAD, DRIFT_SPEED);
        }
    }
}
