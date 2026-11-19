package de.simplepvparena.registration;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class RegistrationMachines {

    private final List<RegistrationMachine> machines = new ArrayList<>();

    public List<RegistrationMachine> machines() {
        return List.copyOf(machines);
    }

    public Optional<RegistrationMachine> findAt(String worldName, int x, int y, int z) {
        return machines.stream().filter(machine -> machine.contains(worldName, x, y, z)).findFirst();
    }

    public boolean overlapsAny(RegistrationMachine candidate) {
        return candidate.blocks().stream()
                .anyMatch(block -> findAt(candidate.worldName(), block[0], block[1], block[2]).isPresent());
    }

    public boolean add(RegistrationMachine machine) {
        Objects.requireNonNull(machine, "machine");
        if (overlapsAny(machine)) {
            return false;
        }
        return machines.add(machine);
    }

    public boolean remove(RegistrationMachine machine) {
        return machines.remove(machine);
    }

    public void replaceAll(List<RegistrationMachine> loadedMachines) {
        machines.clear();
        loadedMachines.forEach(this::add);
    }
}
