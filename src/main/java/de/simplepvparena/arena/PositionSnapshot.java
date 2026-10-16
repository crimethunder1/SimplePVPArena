package de.simplepvparena.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record PositionSnapshot(String worldName, double x, double y, double z, float yaw, float pitch) {

    public PositionSnapshot {
        Objects.requireNonNull(worldName, "worldName");
    }

    public static PositionSnapshot fromLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location must have a world");
        }
        return new PositionSnapshot(world.getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public Optional<Location> toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }

    public String describe() {
        return worldName + " " + String.format(Locale.ROOT, "%.1f/%.1f/%.1f", x, y, z);
    }
}
