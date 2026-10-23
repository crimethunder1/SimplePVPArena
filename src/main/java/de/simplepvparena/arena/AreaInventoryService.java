package de.simplepvparena.arena;

import de.simplepvparena.message.AreaInventoryMessage;
import de.simplepvparena.message.FailureDescriptions;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class AreaInventoryService implements EffectAreaPresence {

    public static final String ENABLED_CONFIGURATION_PATH = "arena.area-inventory.enabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String DEPOSITED_KEY_NAME = "area_inventory_deposited";
    public static final long RECONCILE_PERIOD_TICKS = 20L;
    public static final long WARNING_INTERVAL_MILLIS = 60_000L;
    public static final long CHAT_MESSAGE_INTERVAL_MILLIS = 60_000L;

    private static final byte DEPOSITED_VALUE = 1;

    private final Plugin plugin;
    private final NamespacedKey depositedKey;
    private final Supplier<Optional<EffectArea>> effectAreaSource;
    private final AreaInventoryStorage storage;
    private final Predicate<Player> kitWorn;
    private final Predicate<Player> activeFighter;
    private final SpectatorToggleItemGiver spectatorToggleItemGiver;
    private final PlayerPotionEffects potionEffects;
    private final Consumer<String> warningReporter;
    private final LongSupplier clock;
    private final Map<UUID, Long> lastWarningMillisByPlayer = new HashMap<>();
    private final Map<UUID, Long> lastKeptSafeChatMillisByPlayer = new HashMap<>();
    private final Map<UUID, Long> lastBackChatMillisByPlayer = new HashMap<>();

    private boolean enabled = DEFAULT_ENABLED;
    private BukkitTask reconcileTask;

    public AreaInventoryService(Plugin plugin, Supplier<Optional<EffectArea>> effectAreaSource,
                                AreaInventoryStorage storage, Predicate<Player> kitWorn,
                                Predicate<Player> activeFighter, SpectatorToggleItemGiver spectatorToggleItemGiver,
                                PlayerPotionEffects potionEffects, Consumer<String> warningReporter,
                                LongSupplier clock) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.depositedKey = new NamespacedKey(plugin, DEPOSITED_KEY_NAME);
        this.effectAreaSource = Objects.requireNonNull(effectAreaSource, "effectAreaSource");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.kitWorn = Objects.requireNonNull(kitWorn, "kitWorn");
        this.activeFighter = Objects.requireNonNull(activeFighter, "activeFighter");
        this.spectatorToggleItemGiver = Objects.requireNonNull(spectatorToggleItemGiver, "spectatorToggleItemGiver");
        this.potionEffects = Objects.requireNonNull(potionEffects, "potionEffects");
        this.warningReporter = Objects.requireNonNull(warningReporter, "warningReporter");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        reconcileOnlinePlayers();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        if (reconcileTask != null) {
            return;
        }
        reconcileTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::reconcileOnlinePlayers,
                RECONCILE_PERIOD_TICKS, RECONCILE_PERIOD_TICKS);
    }

    public void stop() {
        if (reconcileTask != null) {
            reconcileTask.cancel();
            reconcileTask = null;
        }
    }

    public boolean isRunning() {
        return reconcileTask != null;
    }

    public void reconcileOnlinePlayers() {
        for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
            reconcile(player);
        }
    }

    @Override
    public void reconcile(Player player) {
        reconcile(player, player.getLocation());
    }

    @Override
    public void reconcile(Player player, Location location) {
        if (isWearingKit(player)) {
            return;
        }
        boolean inside = isLockedAt(location);
        boolean deposited = hasDeposited(player);
        if (inside && !deposited) {
            deposit(player);
        } else if (!inside && deposited) {
            giveBack(player, true);
        }
    }

    public void giveEveryInventoryBackQuietly() {
        for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
            if (hasDeposited(player) && !isWearingKit(player)) {
                giveBack(player, false);
            }
        }
    }

    public boolean isLockedAt(Location location) {
        if (!enabled || location == null) {
            return false;
        }
        return effectAreaSource.get().map(effectArea -> effectArea.contains(location)).orElse(false);
    }

    public boolean hasDeposited(Player player) {
        return player.getPersistentDataContainer().has(depositedKey, PersistentDataType.BYTE);
    }

    public boolean isWearingKit(Player player) {
        return kitWorn.test(player);
    }

    private void deposit(Player player) {
        player.closeInventory();
        ItemStack[] contents = player.getInventory().getContents();
        Optional<CraftingInventory> craftingGrid = findCraftingGrid(player);
        List<ItemStack> looseItems = collectLooseItems(player, craftingGrid);
        AreaPlayerCondition condition = new AreaPlayerCondition(potionEffects.read(player), player.getFoodLevel(),
                player.getSaturation(), player.getExhaustion());
        try {
            storage.save(player.getUniqueId(), contents, looseItems, condition);
        } catch (IOException | RuntimeException writeFailure) {
            warnAtMostOncePerInterval(player, Messages.text(AreaInventoryMessage.NOT_WRITTEN,
                    Placeholder.of("player", player.getName()),
                    Placeholder.of("reason", FailureDescriptions.describe(writeFailure))));
            return;
        }
        player.getPersistentDataContainer().set(depositedKey, PersistentDataType.BYTE, DEPOSITED_VALUE);
        lastWarningMillisByPlayer.remove(player.getUniqueId());
        try {
            player.getInventory().clear();
            if (!looseItems.isEmpty()) {
                player.setItemOnCursor(null);
                craftingGrid.ifPresent(grid -> grid.setMatrix(new ItemStack[grid.getMatrix().length]));
            }
            potionEffects.removeAll(player);
            setHunger(player, AreaPlayerCondition.FULL_FOOD_LEVEL, AreaPlayerCondition.FULL_SATURATION,
                    AreaPlayerCondition.NO_EXHAUSTION);
        } catch (RuntimeException clearFailure) {
            warningReporter.accept(Messages.text(AreaInventoryMessage.NOT_CLEARED,
                    Placeholder.of("player", player.getName()),
                    Placeholder.of("reason", FailureDescriptions.describe(clearFailure))));
            return;
        }
        if (!activeFighter.test(player)) {
            spectatorToggleItemGiver.giveSpectatorToggleItemIfMissing(player);
        }
        String keptSafe = Messages.text(AreaInventoryMessage.KEPT_SAFE);
        if (isChatDue(lastKeptSafeChatMillisByPlayer, player)) {
            TournamentMessages.sendInfo(player, keptSafe);
        } else {
            TournamentMessages.sendActionBarInfo(player, keptSafe);
        }
    }

    private static Optional<CraftingInventory> findCraftingGrid(Player player) {
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory.getType() == InventoryType.CRAFTING && topInventory instanceof CraftingInventory grid) {
            return Optional.of(grid);
        }
        return Optional.empty();
    }

    private static List<ItemStack> collectLooseItems(Player player, Optional<CraftingInventory> craftingGrid) {
        List<ItemStack> looseItems = new ArrayList<>();
        addIfPresent(looseItems, player.getItemOnCursor());
        craftingGrid.ifPresent(grid -> {
            for (ItemStack itemStack : grid.getMatrix()) {
                addIfPresent(looseItems, itemStack);
            }
        });
        return looseItems;
    }

    private static void addIfPresent(List<ItemStack> itemStacks, ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            itemStacks.add(itemStack);
        }
    }

    private void giveBack(Player player, boolean informPlayer) {
        AreaInventoryLookup lookup = storage.find(player.getUniqueId());
        switch (lookup.outcome()) {
            case FOUND -> restore(player, lookup.contents().orElseThrow(), lookup.looseItems(), lookup.condition(),
                    lookup.fileName(), informPlayer);
            case MISSING -> abandon(player, AreaInventoryMessage.MISSING, lookup.fileName(), informPlayer);
            case BROKEN -> abandon(player, AreaInventoryMessage.UNREADABLE, lookup.fileName(), informPlayer);
        }
    }

    private void restore(Player player, ItemStack[] contents, List<ItemStack> looseItems,
                         Optional<AreaPlayerCondition> condition, String fileName, boolean informPlayer) {
        try {
            PlayerInventory inventory = player.getInventory();
            inventory.clear();
            inventory.setContents(contents);
            giveLooseItemsBack(player, inventory, looseItems);
            condition.ifPresent(savedCondition -> restoreCondition(player, savedCondition));
        } catch (RuntimeException restoreFailure) {
            warnAtMostOncePerInterval(player, Messages.text(AreaInventoryMessage.NOT_RESTORED,
                    Placeholder.of("player", player.getName()),
                    Placeholder.of("reason", FailureDescriptions.describe(restoreFailure))));
            return;
        }
        removeMarker(player);
        if (savePlayerData(player, fileName)) {
            deleteFile(player, fileName);
        }
        if (!informPlayer) {
            return;
        }
        String back = Messages.text(AreaInventoryMessage.BACK);
        if (isChatDue(lastBackChatMillisByPlayer, player)) {
            TournamentMessages.sendSuccess(player, back);
        } else {
            TournamentMessages.sendActionBarSuccess(player, back);
        }
    }

    private void restoreCondition(Player player, AreaPlayerCondition condition) {
        potionEffects.removeAll(player);
        potionEffects.apply(player, condition.potionEffects().stream()
                .filter(SavedPotionEffect::isRestorable)
                .toList());
        setHunger(player, condition.foodLevel(), condition.saturation(), condition.exhaustion());
    }

    private static void setHunger(Player player, int foodLevel, float saturation, float exhaustion) {
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
    }

    private static void giveLooseItemsBack(Player player, PlayerInventory inventory, List<ItemStack> looseItems) {
        if (looseItems.isEmpty()) {
            return;
        }
        Map<Integer, ItemStack> itemsWithoutRoom = inventory.addItem(looseItems.toArray(new ItemStack[0]));
        for (ItemStack itemWithoutRoom : itemsWithoutRoom.values()) {
            player.getWorld().dropItem(player.getLocation(), itemWithoutRoom);
        }
    }

    private boolean isChatDue(Map<UUID, Long> lastChatMillisByPlayer, Player player) {
        long now = clock.getAsLong();
        Long lastChatMillis = lastChatMillisByPlayer.get(player.getUniqueId());
        if (lastChatMillis != null && now - lastChatMillis < CHAT_MESSAGE_INTERVAL_MILLIS) {
            return false;
        }
        lastChatMillisByPlayer.put(player.getUniqueId(), now);
        return true;
    }

    private void abandon(Player player, AreaInventoryMessage warning, String fileName, boolean informPlayer) {
        removeMarker(player);
        warningReporter.accept(Messages.text(warning, Placeholder.of("player", player.getName()),
                Placeholder.of("file", fileName)));
        if (informPlayer) {
            TournamentMessages.sendWarning(player, Messages.text(AreaInventoryMessage.NOT_GIVEN_BACK));
        }
    }

    private boolean savePlayerData(Player player, String fileName) {
        try {
            player.saveData();
            return true;
        } catch (RuntimeException saveFailure) {
            warningReporter.accept(Messages.text(AreaInventoryMessage.PLAYER_DATA_NOT_SAVED,
                    Placeholder.of("player", player.getName()), Placeholder.of("file", fileName),
                    Placeholder.of("reason", FailureDescriptions.describe(saveFailure))));
            return false;
        }
    }

    private void deleteFile(Player player, String fileName) {
        try {
            storage.delete(player.getUniqueId());
        } catch (IOException deleteFailure) {
            warningReporter.accept(Messages.text(AreaInventoryMessage.FILE_NOT_DELETED,
                    Placeholder.of("player", player.getName()), Placeholder.of("file", fileName),
                    Placeholder.of("reason", FailureDescriptions.describe(deleteFailure))));
        }
    }

    private void removeMarker(Player player) {
        player.getPersistentDataContainer().remove(depositedKey);
        lastWarningMillisByPlayer.remove(player.getUniqueId());
    }

    private void warnAtMostOncePerInterval(Player player, String warning) {
        long now = clock.getAsLong();
        Long lastWarningMillis = lastWarningMillisByPlayer.get(player.getUniqueId());
        if (lastWarningMillis != null && now - lastWarningMillis < WARNING_INTERVAL_MILLIS) {
            return;
        }
        lastWarningMillisByPlayer.put(player.getUniqueId(), now);
        warningReporter.accept(warning);
    }
}
