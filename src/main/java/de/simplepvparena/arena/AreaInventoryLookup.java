package de.simplepvparena.arena;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AreaInventoryLookup(Outcome outcome, Optional<ItemStack[]> contents, List<ItemStack> looseItems,
                                  String fileName) {

    public enum Outcome {
        FOUND,
        MISSING,
        BROKEN
    }

    public AreaInventoryLookup {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(contents, "contents");
        looseItems = List.copyOf(looseItems);
        Objects.requireNonNull(fileName, "fileName");
    }

    public static AreaInventoryLookup found(ItemStack[] contents, List<ItemStack> looseItems, String fileName) {
        return new AreaInventoryLookup(Outcome.FOUND, Optional.of(contents.clone()), looseItems, fileName);
    }

    public static AreaInventoryLookup missing(String fileName) {
        return new AreaInventoryLookup(Outcome.MISSING, Optional.empty(), List.of(), fileName);
    }

    public static AreaInventoryLookup broken(String fileName) {
        return new AreaInventoryLookup(Outcome.BROKEN, Optional.empty(), List.of(), fileName);
    }
}
