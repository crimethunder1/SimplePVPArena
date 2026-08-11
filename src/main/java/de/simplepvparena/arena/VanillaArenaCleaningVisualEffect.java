package de.simplepvparena.arena;

import de.simplepvparena.particle.ParticleTypes;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public final class VanillaArenaCleaningVisualEffect implements ArenaCleaningVisualEffect {

    public static final Particle DEFAULT_PARTICLE = Particle.DUST;

    private static final int DIRECTED_PARTICLE_COUNT = 0;

    private static final int SINGLE_PARTICLE_COUNT = 1;

    private static final double NO_OFFSET = 0.0D;
    private static final double UPWARDS = 1.0D;
    private static final double NO_SPEED = 0.0D;

    private static final double RISE_SPEED = 0.2D;

    private static final float PARTICLE_SIZE = 1.0F;

    private static final double CELL_CENTRE = 0.5D;

    private final IntUnaryOperator randomIndex;

    private ArenaCleaningParticleSettings settings = ArenaCleaningParticleSettings.defaults();
    private Particle particle = DEFAULT_PARTICLE;

    private Map<ArenaCleaningPhase, Particle.DustOptions> phaseColours;

    public VanillaArenaCleaningVisualEffect() {
        this(bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    VanillaArenaCleaningVisualEffect(IntUnaryOperator randomIndex) {
        this.randomIndex = Objects.requireNonNull(randomIndex, "randomIndex");
        this.phaseColours = coloursOf(DEFAULT_PARTICLE);
    }

    @Override
    public void showFloodedLayer(World world, ArenaShape shape, ArenaCleaningPhase phase, int layerY) {
        if (!settings.isShown()) {
            return;
        }
        long[] columns = ArenaCleaningColumnSample.pick(shape, settings.columnsPerTick(), randomIndex);
        double dotY = layerY + CELL_CENTRE;
        Particle.DustOptions colour = phaseColours.get(phase);
        for (long column : columns) {
            spawnDotAt(world, HorizontalCellCodec.decodeX(column) + CELL_CENTRE, dotY,
                    HorizontalCellCodec.decodeZ(column) + CELL_CENTRE, colour);
        }
    }

    @Override
    public void applyParticleSettings(ArenaCleaningParticleSettings settings,
                                      Consumer<String> problemReporter) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.particle = resolveParticle(settings.particleName(), problemReporter);
        this.phaseColours = coloursOf(particle);
    }

    ArenaCleaningParticleSettings getSettings() {
        return settings;
    }

    Particle getParticle() {
        return particle;
    }

    Optional<Particle.DustOptions> findColour(ArenaCleaningPhase phase) {
        return Optional.ofNullable(phaseColours.get(phase));
    }

    private void spawnDotAt(World world, double dotX, double dotY, double dotZ,
                            Particle.DustOptions colour) {
        if (colour == null) {
            world.spawnParticle(particle, dotX, dotY, dotZ, DIRECTED_PARTICLE_COUNT,
                    NO_OFFSET, UPWARDS, NO_OFFSET, RISE_SPEED);
            return;
        }
        world.spawnParticle(particle, dotX, dotY, dotZ, SINGLE_PARTICLE_COUNT,
                NO_OFFSET, NO_OFFSET, NO_OFFSET, NO_SPEED, colour);
    }

    private static Map<ArenaCleaningPhase, Particle.DustOptions> coloursOf(Particle particle) {
        if (!ParticleTypes.carriesColour(particle)) {
            return Map.of();
        }
        Map<ArenaCleaningPhase, Particle.DustOptions> colours = new EnumMap<>(ArenaCleaningPhase.class);
        for (ArenaCleaningPhase phase : ArenaCleaningPhase.values()) {
            colours.put(phase, new Particle.DustOptions(
                    Color.fromRGB(phase.red(), phase.green(), phase.blue()), PARTICLE_SIZE));
        }
        return colours;
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
        if (!isSupported(particle)) {
            problemReporter.accept(ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH + " is set to "
                    + particle.name() + ", which needs extra data the cleaning cannot supply. Use "
                    + DEFAULT_PARTICLE.name() + " for the two coloured phases or a particle without "
                    + "extra data for a colourless animation. Falling back to " + DEFAULT_PARTICLE.name());
            return DEFAULT_PARTICLE;
        }
        return particle;
    }

    static boolean isSupported(Particle particle) {
        return ParticleTypes.carriesColour(particle) || ParticleTypes.carriesNoData(particle);
    }
}
