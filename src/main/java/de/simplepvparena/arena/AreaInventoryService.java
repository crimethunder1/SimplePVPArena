package de.simplepvparena.arena;

import de.simplepvparena.message.AreaInventoryMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
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

public final class AreaInventoryService {

    public static final String ENABLED_CONFIGURATION_PATH = "arena.area-inventory.enabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String DEPOSITED_KEY_NAME = "area_inventory_deposited";
    public static final long RECONCILE_PERIOD_TICKS = 20L;
    public static final long WARNING_INTERVAL_MILLIS = 60_000L;

    private static final byte DEPOSITED_VALUE = 1;

    private final Plugin plugin;
    private final NamespacedKey depositedKey;
    private final Supplier<Optional<EffectArea>> effectAreaSource;
    private final AreaInventoryStorage storage;
    private final Predicate<Player> kitWorn;
    private final Consumer<String> warningReporter;
    private final LongSupplier clock;
    private final Map<UUID, Long> lastWarningMillisByPlayer = new HashMap<>();

    private boolean enabled = DEFAULT_ENABLED;
    private BukkitTask reconcileTask;

    public AreaInventoryService(Plugin plugin, Supplier<Optional<EffectArea>> effectAreaSource,
                                AreaInventoryStorage storage, Predicate<Player> kitWorn,
                                Consumer<String> warningReporter, LongSupplier clock) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.depositedKey = new NamespacedKey(plugin, DEPOSITED_KEY_NAME);
        this.effectAreaSource = Objects.requireNonNull(effectAreaSource, "effectAreaSource");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.kitWorn = Objects.requireNonNull(kitWorn, "kitWorn");
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

    public void reconcile(Player player) {
        reconcile(player, player.getLocation());
    }

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
        ItemStack[] contents = player.getInventory().getContents();
        try {
            storage.save(player.getUniqueId(), contents);
        } catch (IOException | RuntimeException writeFailure) {
            warnAtMostOncePerInterval(player, Messages.text(AreaInventoryMessage.NOT_WRITTEN,
                    Placeholder.of("player", player.getName()),
                    Placeholder.of("reason", describeFailure(writeFailure))));
            return;
        }
        player.getPersistentDataContainer().set(depositedKey, PersistentDataType.BYTE, DEPOSITED_VALUE);
        lastWarningMillisByPlayer.remove(player.getUniqueId());
        try {
            player.getInventory().clear();
        } catch (RuntimeException clearFailure) {
            warningReporter.accept(Messages.text(AreaInventoryMessage.NOT_CLEARED,
                    Placeholder.of("player", player.getName()),
                    Placeholder.of("reason", describeFailure(clearFailure))));
            return;
        }
        TournamentMessages.sendInfo(player, Messages.text(AreaInventoryMessage.KEPT_SAFE));
    }

    private void giveBack(Player player, boolean informPlayer) {
        AreaInventoryLookup lookup = storage.find(player.getUniqueId());
        switch (lookup.outcome()) {
            case FOUND -> restore(player, lookup.contents().orElseThrow(), lookup.fileName(), informPlayer);
            case MISSING -> abandon(player, AreaInventoryMessage.MISSING, lookup.fileName(), informPlayer);
            case BROKEN -> abandon(player, AreaInventoryMessage.UNREADABLE, lookup.fileName(), informPlayer);
        }
    }

    private void restore(Player player, ItemStack[] contents, String fileName, boolean informPlayer) {
        try {
            PlayerInventory inventory = player.getInventory();
            inventory.clear();
            inventory.setContents(contents);
        } catch (RuntimeException restoreFailure) {
            warnAtMostOncePerInterval(player, Messages.text(AreaInventoryMessage.NOT_RESTORED,
                    Placeholder.of("player", player.getName()),
                    Placeholder.of("reason", describeFailure(restoreFailure))));
            return;
        }
        removeMarker(player);
        if (savePlayerData(player, fileName)) {
            deleteFile(player, fileName);
        }
        if (informPlayer) {
            TournamentMessages.sendSuccess(player, Messages.text(AreaInventoryMessage.BACK));
        }
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
                    Placeholder.of("reason", describeFailure(saveFailure))));
            return false;
        }
    }

    private void deleteFile(Player player, String fileName) {
        try {
            storage.delete(player.getUniqueId());
        } catch (IOException deleteFailure) {
            warningReporter.accept(Messages.text(AreaInventoryMessage.FILE_NOT_DELETED,
                    Placeholder.of("player", player.getName()), Placeholder.of("file", fileName),
                    Placeholder.of("reason", describeFailure(deleteFailure))));
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

    private static String describeFailure(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
