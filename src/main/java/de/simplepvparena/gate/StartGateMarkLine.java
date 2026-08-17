package de.simplepvparena.gate;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record StartGateMarkLine(long encodedPosition, Material material) {

    public static final String SEPARATOR = " ";

    public StartGateMarkLine {
        Objects.requireNonNull(material, "material");
    }

    public static String format(long encodedPosition, Material material) {
        return encodedPosition + SEPARATOR + material.name();
    }

    public String format() {
        return format(encodedPosition, material);
    }

    public static Optional<StartGateMarkLine> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String[] values = line.trim().split("\\s+");
        if (values.length != 2) {
            return Optional.empty();
        }
        long encodedPosition;
        try {
            encodedPosition = Long.parseLong(values[0]);
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
        Material material = Material.getMaterial(values[1].toUpperCase(Locale.ROOT));
        if (material == null) {
            return Optional.empty();
        }
        return Optional.of(new StartGateMarkLine(encodedPosition, material));
    }

    public int blockX() {
        return BlockPositionCodec.decodeX(encodedPosition);
    }

    public int blockY() {
        return BlockPositionCodec.decodeY(encodedPosition);
    }

    public int blockZ() {
        return BlockPositionCodec.decodeZ(encodedPosition);
    }
}
