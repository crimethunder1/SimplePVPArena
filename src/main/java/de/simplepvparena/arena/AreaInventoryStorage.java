package de.simplepvparena.arena;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@NullMarked
public interface AreaInventoryStorage {

    void save(UUID playerUniqueId, ItemStack[] contents, List<ItemStack> looseItems, AreaPlayerCondition condition)
            throws IOException;

    AreaInventoryLookup find(UUID playerUniqueId);

    void delete(UUID playerUniqueId) throws IOException;
}
