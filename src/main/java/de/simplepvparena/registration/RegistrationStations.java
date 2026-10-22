package de.simplepvparena.registration;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RegistrationStations {

    private final Set<RegistrationStation> stations = new LinkedHashSet<>();

    public boolean isStation(Location location) {
        if (location == null) {
            return false;
        }
        World world = location.getWorld();
        return world != null && stations.contains(new RegistrationStation(world.getName(), location.getBlockX(),
                location.getBlockY(), location.getBlockZ()));
    }

    public boolean isStation(RegistrationStation station) {
        return stations.contains(station);
    }

    public List<RegistrationStation> stations() {
        return List.copyOf(stations);
    }

    public boolean add(RegistrationStation station) {
        return stations.add(Objects.requireNonNull(station, "station"));
    }

    public boolean remove(RegistrationStation station) {
        return stations.remove(station);
    }

    public void clear() {
        stations.clear();
    }
}
