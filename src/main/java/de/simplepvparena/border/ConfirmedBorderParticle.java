package de.simplepvparena.border;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class ConfirmedBorderParticle {

    public static final Particle DEFAULT_PARTICLE = Particle.END_ROD;

    private static final float PARTICLE_SIZE = 1.0F;
    private static final double RISE_SPEED = 0.01D;
    private static final int DIRECTED_PARTICLE_COUNT = 0;
    private static final int SINGLE_PARTICLE_COUNT = 1;
    private static final double NO_OFFSET = 0.0D;
    private static final double UPWARDS = 1.0D;
    private static final double NO_SPEED = 0.0D;

    private final Particle particle;

    private final Particle.DustOptions colour;

    private final double stepHeight;
    private final int particleCount;

    private ConfirmedBorderParticle(Particle particle, Particle.DustOptions colour, double stepHeight,
                                    int particleCount) {
        this.particle = Objects.requireNonNull(particle, "particle");
        this.colour = colour;
        this.stepHeight = stepHeight;
        this.particleCount = particleCount;
    }

    public static ConfirmedBorderParticle defaults() {
        return from(ConfirmedBorderParticleSettings.defaults(), problem -> {
        });
    }

    public static ConfirmedBorderParticle from(ConfirmedBorderParticleSettings settings,
                                               Consumer<String> problemReporter) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(problemReporter, "problemReporter");
        Particle particle = resolveParticle(settings.particleName(), problemReporter);
        Particle.DustOptions colour = carriesColour(particle)
                ? new Particle.DustOptions(Color.fromRGB(settings.red(), settings.green(), settings.blue()),
                        PARTICLE_SIZE)
                : null;
        return new ConfirmedBorderParticle(particle, colour, settings.streamStepHeight(),
                settings.drawnParticleCount());
    }

    public void spawnStreamAt(Player player, Location borderBlockCenter) {
        for (int step = 0; step < particleCount; step++) {
            Location streamPoint = borderBlockCenter.clone().add(NO_OFFSET, step * stepHeight, NO_OFFSET);
            spawnDotAt(player, streamPoint);
        }
    }

    public Particle getParticle() {
        return particle;
    }

    public Optional<Particle.DustOptions> getColour() {
        return Optional.ofNullable(colour);
    }

    public double getStepHeight() {
        return stepHeight;
    }

    public int getParticleCount() {
        return particleCount;
    }

    private static Particle resolveParticle(String particleName, Consumer<String> problemReporter) {
        Optional<Particle> namedParticle = byName(particleName);
        if (namedParticle.isEmpty()) {
            problemReporter.accept(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH + " is set to "
                    + particleName + ", which is no particle of this server version. Falling back to "
                    + DEFAULT_PARTICLE.name());
            return DEFAULT_PARTICLE;
        }
        Particle particle = namedParticle.get();
        if (!isSupported(particle)) {
            problemReporter.accept(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH + " is set to "
                    + particle.name() + ", which needs extra data the border cannot supply. Use "
                    + DEFAULT_PARTICLE.name() + " or another particle without extra data, or "
                    + Particle.DUST.name() + " for a coloured one. Falling back to " + DEFAULT_PARTICLE.name());
            return DEFAULT_PARTICLE;
        }
        return particle;
    }

    static Optional<Particle> byName(String particleName) {
        if (particleName == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Particle.valueOf(particleName));
        } catch (IllegalArgumentException unknownParticle) {
            return Optional.empty();
        }
    }

    static boolean isSupported(Particle particle) {
        return carriesNoData(particle) || carriesColour(particle);
    }

    static boolean carriesColour(Particle particle) {
        return particle.getDataType() == Particle.DustOptions.class;
    }

    static boolean carriesNoData(Particle particle) {
        return particle.getDataType() == Void.class;
    }

    private void spawnDotAt(Player player, Location streamPoint) {
        if (colour == null) {
            player.spawnParticle(particle, streamPoint, DIRECTED_PARTICLE_COUNT,
                    NO_OFFSET, UPWARDS, NO_OFFSET, RISE_SPEED);
            return;
        }
        player.spawnParticle(particle, streamPoint, SINGLE_PARTICLE_COUNT,
                NO_OFFSET, NO_OFFSET, NO_OFFSET, NO_SPEED, colour);
    }
}
