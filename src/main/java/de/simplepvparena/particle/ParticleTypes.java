package de.simplepvparena.particle;

import org.bukkit.Particle;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public final class ParticleTypes {

    private ParticleTypes() {
    }

    public static Optional<Particle> byName(String particleName) {
        if (particleName == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Particle.valueOf(particleName));
        } catch (IllegalArgumentException unknownParticle) {
            return Optional.empty();
        }
    }

    public static boolean carriesNoData(Particle particle) {
        return particle.getDataType() == Void.class;
    }

    public static boolean carriesColour(Particle particle) {
        return particle.getDataType() == Particle.DustOptions.class;
    }
}
