package de.simplepvparena.particle;

import java.util.Locale;

public final class ParticleNames {

    private static final char NAME_SEPARATOR = '_';

    private static final char NAMESPACE_SEPARATOR = ':';

    private ParticleNames() {
    }

    public static String normalize(String particleName, String fallbackName) {
        if (particleName == null) {
            return fallbackName;
        }
        String withoutNamespace = particleName.substring(particleName.lastIndexOf(NAMESPACE_SEPARATOR) + 1);
        String normalized = withoutNamespace.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', NAME_SEPARATOR)
                .replace(' ', NAME_SEPARATOR);
        return normalized.isEmpty() ? fallbackName : normalized;
    }
}
