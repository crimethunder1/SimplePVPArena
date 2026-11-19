package de.simplepvparena.registration;

import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record RegistrationStation(String worldName, int blockX, int blockY, int blockZ) {

    public RegistrationStation {
        Objects.requireNonNull(worldName, "worldName");
    }

    public static RegistrationStation of(Block block) {
        return new RegistrationStation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
