package de.simplepvparena.arena;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public record AreaInventoryLookup(Outcome outcome, Optional<ItemStack[]> contents, String fileName) {

    public enum Outcome {
        FOUND,
        MISSING,
        BROKEN
    }

    public AreaInventoryLookup {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(contents, "contents");
        Objects.requireNonNull(fileName, "fileName");
    }

    public static AreaInventoryLookup found(ItemStack[] contents, String fileName) {
        return new AreaInventoryLookup(Outcome.FOUND, Optional.of(contents.clone()), fileName);
    }

    public static AreaInventoryLookup missing(String fileName) {
        return new AreaInventoryLookup(Outcome.MISSING, Optional.empty(), fileName);
    }

    public static AreaInventoryLookup broken(String fileName) {
        return new AreaInventoryLookup(Outcome.BROKEN, Optional.empty(), fileName);
    }
}
