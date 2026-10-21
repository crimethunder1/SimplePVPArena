package de.simplepvparena.match;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.VisualMessage;
import de.simplepvparena.visual.CustomVisualSettings;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CustomItemRoundStartEffects implements RoundStartEffects {

    public static final long ANIMATION_DELAY_TICKS = 1L;
    public static final long GIVE_BACK_DELAY_TICKS = 2L;

    private static final int NO_SLOT = -1;

    private final Plugin plugin;
    private final RoundStartEffects vanillaEffects;
    private final Supplier<Optional<ItemStack>> totemItemSource;
    private final FightStartTotems fightStartTotems;
    private final Consumer<String> warningLogger;
    private final Map<UUID, PendingTotem> pendingTotems = new LinkedHashMap<>();

    private boolean preparationFailureReported;

    public CustomItemRoundStartEffects(Plugin plugin, RoundStartEffects vanillaEffects,
                                       Supplier<Optional<ItemStack>> totemItemSource,
                                       FightStartTotems fightStartTotems, Consumer<String> warningLogger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.vanillaEffects = Objects.requireNonNull(vanillaEffects, "vanillaEffects");
        this.totemItemSource = Objects.requireNonNull(totemItemSource, "totemItemSource");
        this.fightStartTotems = Objects.requireNonNull(fightStartTotems, "fightStartTotems");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
    }

    @Override
    public void blind(Player player, int durationTicks) {
        vanillaEffects.blind(player, durationTicks);
    }

    @Override
    public void removeBlindness(Player player) {
        vanillaEffects.removeBlindness(player);
    }

    @Override
    public void playFightStartAnimation(Player player) {
        stopFightStartAnimation(player);
        Optional<ItemStack> totem = totemItemSource.get().flatMap(this::prepareTotem);
        if (totem.isEmpty()) {
            vanillaEffects.playFightStartAnimation(player);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        PendingTotem pendingTotem = new PendingTotem(player, copyOf(inventory.getItemInOffHand()));
        pendingTotems.put(player.getUniqueId(), pendingTotem);
        try {
            inventory.setItemInOffHand(totem.get());
        } catch (RuntimeException failure) {
            abandon(pendingTotem);
            reportPreparationFailure(failure);
            vanillaEffects.playFightStartAnimation(player);
            return;
        }
        pendingTotem.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> playAnimation(pendingTotem), ANIMATION_DELAY_TICKS));
        pendingTotem.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> stop(pendingTotem), GIVE_BACK_DELAY_TICKS));
    }

    @Override
    public void stopFightStartAnimation(Player player) {
        PendingTotem pendingTotem = pendingTotems.get(player.getUniqueId());
        if (pendingTotem != null) {
            stop(pendingTotem);
        }
    }

    @Override
    public void stopFightStartAnimations() {
        for (PendingTotem pendingTotem : List.copyOf(pendingTotems.values())) {
            stop(pendingTotem);
        }
    }

    public boolean isTotem(ItemStack itemStack) {
        return fightStartTotems.isTotem(itemStack);
    }

    public void removeTotems(Player player) {
        removeTotems(player.getInventory());
    }

    private Optional<ItemStack> prepareTotem(ItemStack customItem) {
        try {
            return Optional.of(fightStartTotems.createTotem(customItem));
        } catch (RuntimeException failure) {
            reportPreparationFailure(failure);
            return Optional.empty();
        }
    }

    private void playAnimation(PendingTotem pendingTotem) {
        if (pendingTotems.get(pendingTotem.player.getUniqueId()) != pendingTotem || !pendingTotem.player.isOnline()) {
            return;
        }
        vanillaEffects.playFightStartAnimation(pendingTotem.player);
    }

    private void stop(PendingTotem pendingTotem) {
        pendingTotems.remove(pendingTotem.player.getUniqueId(), pendingTotem);
        for (BukkitTask task : pendingTotem.tasks) {
            task.cancel();
        }
        pendingTotem.tasks.clear();
        giveBack(pendingTotem.player, pendingTotem.offHandItem);
    }

    private void abandon(PendingTotem pendingTotem) {
        pendingTotems.remove(pendingTotem.player.getUniqueId(), pendingTotem);
        PlayerInventory inventory = pendingTotem.player.getInventory();
        removeTotems(inventory);
        if (isNothing(inventory.getItemInOffHand()) && !isNothing(pendingTotem.offHandItem)) {
            inventory.setItemInOffHand(pendingTotem.offHandItem);
        }
    }

    private void giveBack(Player player, ItemStack offHandItem) {
        PlayerInventory inventory = player.getInventory();
        int totemSlot = removeTotems(inventory);
        if (isNothing(offHandItem)) {
            return;
        }
        if (totemSlot != NO_SLOT) {
            inventory.setItem(totemSlot, offHandItem);
            return;
        }
        if (isNothing(inventory.getItemInOffHand())) {
            inventory.setItemInOffHand(offHandItem);
            return;
        }
        for (ItemStack itemThatDidNotFit : inventory.addItem(offHandItem).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), itemThatDidNotFit);
        }
    }

    private int removeTotems(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        int firstTotemSlot = NO_SLOT;
        for (int slot = 0; slot < contents.length; slot++) {
            if (fightStartTotems.isTotem(contents[slot])) {
                if (firstTotemSlot == NO_SLOT) {
                    firstTotemSlot = slot;
                }
                inventory.setItem(slot, null);
            }
        }
        return firstTotemSlot;
    }

    private void reportPreparationFailure(RuntimeException failure) {
        if (preparationFailureReported) {
            return;
        }
        preparationFailureReported = true;
        warningLogger.accept(Messages.text(VisualMessage.TOTEM_NOT_PREPARED,
                Placeholder.of("path", CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH),
                Placeholder.of("reason", failure.getMessage() == null
                        ? failure.getClass().getSimpleName()
                        : failure.getMessage())));
    }

    private static ItemStack copyOf(ItemStack itemStack) {
        return isNothing(itemStack) ? null : itemStack.clone();
    }

    private static boolean isNothing(ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty();
    }

    private static final class PendingTotem {

        private final Player player;
        private final ItemStack offHandItem;
        private final List<BukkitTask> tasks = new ArrayList<>();

        private PendingTotem(Player player, ItemStack offHandItem) {
            this.player = player;
            this.offHandItem = offHandItem;
        }
    }
}
