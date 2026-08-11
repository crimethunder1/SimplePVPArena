package de.simplepvparena.arena;

import de.simplepvparena.particle.ParticleTypes;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public final class VanillaArenaCleaningVisualEffect implements ArenaCleaningVisualEffect {

    public static final Particle DEFAULT_PARTICLE = Particle.WITCH;

    private static final int DIRECTED_PARTICLE_COUNT = 0;

    private static final double NO_OFFSET = 0.0D;
    private static final double UPWARDS = 1.0D;

    private static final double RISE_SPEED = 0.2D;

    private static final double CELL_CENTRE = 0.5D;

    private static final double FLOOR_FACE = 1.0D;

    private final IntUnaryOperator randomIndex;

    private ArenaCleaningParticleSettings settings = ArenaCleaningParticleSettings.defaults();
    private Particle particle = DEFAULT_PARTICLE;

    public VanillaArenaCleaningVisualEffect() {
        this(bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    VanillaArenaCleaningVisualEffect(IntUnaryOperator randomIndex) {
        this.randomIndex = Objects.requireNonNull(randomIndex, "randomIndex");
    }

    @Override
    public void showCleanedColumns(World world, ArenaShape shape, long[] cleanedBlockBatch) {
        if (!settings.isShown()) {
            return;
        }
        long[] columns = ArenaCleaningColumnSample.pick(cleanedBlockBatch, settings.columnsPerTick(),
                randomIndex);
        double bottomY = shape.getFloorY() + FLOOR_FACE;
        int dotCount = settings.dotCount();
        for (long column : columns) {
            double columnX = HorizontalCellCodec.decodeX(column) + CELL_CENTRE;
            double columnZ = HorizontalCellCodec.decodeZ(column) + CELL_CENTRE;
            for (int dot = 0; dot < dotCount; dot++) {
                world.spawnParticle(particle, columnX, bottomY + dot * ArenaCleaningParticleSettings.DOT_DISTANCE,
                        columnZ, DIRECTED_PARTICLE_COUNT, NO_OFFSET, UPWARDS, NO_OFFSET, RISE_SPEED);
            }
        }
    }

    @Override
    public void applyParticleSettings(ArenaCleaningParticleSettings settings,
                                      Consumer<String> problemReporter) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.particle = resolveParticle(settings.particleName(), problemReporter);
    }

    ArenaCleaningParticleSettings getSettings() {
        return settings;
    }

    Particle getParticle() {
        return particle;
    }

    private static Particle resolveParticle(String particleName, Consumer<String> problemReporter) {
        Optional<Particle> namedParticle = ParticleTypes.byName(particleName);
        if (namedParticle.isEmpty()) {
            problemReporter.accept(ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH + " is set to "
                    + particleName + ", which is no particle of this server version. Falling back to "
                    + DEFAULT_PARTICLE.name());
            return DEFAULT_PARTICLE;
        }
        Particle particle = namedParticle.get();
        if (!ParticleTypes.carriesNoData(particle)) {
            problemReporter.accept(ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH + " is set to "
                    + particle.name() + ", which needs extra data the cleaning cannot supply. Use "
                    + DEFAULT_PARTICLE.name() + " or another particle without extra data. Falling back to "
                    + DEFAULT_PARTICLE.name());
            return DEFAULT_PARTICLE;
        }
        return particle;
    }
}
