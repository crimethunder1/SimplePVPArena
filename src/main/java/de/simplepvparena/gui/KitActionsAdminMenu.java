package de.simplepvparena.gui;

import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.LibraryKit;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.KitActionsMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

final class KitActionsAdminMenu implements AdminMenu {

    private static final int KIT_DEFAULT_SLOT = 11;
    private static final int KIT_REPLACE_SLOT = 13;
    private static final int KIT_DELETE_SLOT = 15;

    private final AdminMenuNavigation navigation;
    private final KitLibrary kitLibrary;
    private final KitSaveService kitSaveService;
    private final BooleanSupplier matchRunning;

    KitActionsAdminMenu(AdminMenuNavigation navigation, KitLibrary kitLibrary, KitSaveService kitSaveService,
                        BooleanSupplier matchRunning) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
        this.kitSaveService = Objects.requireNonNull(kitSaveService, "kitSaveService");
        this.matchRunning = Objects.requireNonNull(matchRunning, "matchRunning");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(KitActionsMessage.TITLE, Placeholder.of("kit", holder.getSubject().orElseThrow()));
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        String kitName = holder.getSubject().orElseThrow();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(kitName, kitLibrary.findKit(kitName)
                .map(this::buildInfoLore)
                .orElseGet(() -> List.of(Messages.text(KitActionsMessage.INFO_KIT_GONE)))));
        canvas.set(KIT_DEFAULT_SLOT, buildDefaultItem(kitName));
        canvas.set(KIT_REPLACE_SLOT, MenuItems.create(Material.CHEST, Messages.text(KitActionsMessage.REPLACE_TITLE),
                buildReplacementLore(kitName, Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION))));
        canvas.set(KIT_DELETE_SLOT, kitLibrary.findDeletionRejection(kitName)
                .map(rejection -> MenuItems.unavailable(Messages.text(KitActionsMessage.DELETE_TITLE), rejection))
                .orElseGet(() -> MenuItems.create(MenuItems.REFUSE_MATERIAL,
                        Messages.text(KitActionsMessage.DELETE_TITLE),
                        Messages.text(KitActionsMessage.DELETE_LORE, Placeholder.of("kit", kitName)),
                        Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION))));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_KIT_LIST));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        String kitName = holder.getSubject().orElseThrow();
        switch (slot) {
            case KIT_DEFAULT_SLOT -> useAsDefaultKit(player, holder, kitName);
            case KIT_REPLACE_SLOT -> askForReplacementConfirmation(player, holder, kitName);
            case KIT_DELETE_SLOT -> askForDeletionConfirmation(player, holder, kitName);
            case AdminMenuLayout.BACK_SLOT -> navigation.open(player, AdminMenuHolder.kit(holder.getPageIndex()));
            default -> { }
        }
    }

    AdminMenuConfirmation replaceConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return buildReplacementLore(holder.getSubject().orElseThrow(),
                        Messages.text(KitActionsMessage.REPLACE_CONFIRMATION_ITEMS_GONE));
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                KitAdminMenu.reportSave(player,
                        kitSaveService.replaceFromInventory(player, holder.getSubject().orElseThrow()));
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.kitActions(holder.getSubject().orElseThrow(), holder.getPageIndex());
            }
        };
    }

    AdminMenuConfirmation deleteConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return Messages.lines(KitActionsMessage.DELETE_CONFIRMATION,
                        Placeholder.of("kit", holder.getSubject().orElseThrow()));
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                deleteKit(player, holder.getSubject().orElseThrow());
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.kit(holder.getPageIndex());
            }
        };
    }

    private List<String> buildInfoLore(LibraryKit kit) {
        List<String> lore = new ArrayList<>(KitAdminMenu.describeItemCounts(kit));
        lore.add(kitLibrary.isDefaultKit(kit.name())
                ? Messages.text(KitActionsMessage.INFO_DEFAULT_KIT)
                : Messages.text(KitActionsMessage.INFO_NOT_DEFAULT_KIT));
        return lore;
    }

    private MenuTile buildDefaultItem(String kitName) {
        Optional<String> rejection = kitLibrary.findDefaultRejection(kitName);
        if (rejection.isPresent()) {
            return MenuItems.unavailable(kitLibrary.isDefaultKit(kitName)
                    ? Messages.text(KitActionsMessage.DEFAULT_ALREADY_DEFAULT_TITLE)
                    : Messages.text(KitActionsMessage.DEFAULT_TITLE), rejection.get());
        }
        return MenuItems.create(Material.NETHER_STAR, Messages.text(KitActionsMessage.DEFAULT_TITLE),
                Messages.lines(KitActionsMessage.DEFAULT_LORE));
    }

    private List<String> buildReplacementLore(String kitName, String closingLine) {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.text(KitActionsMessage.REPLACEMENT_INVENTORY_BECOMES_KIT, Placeholder.of("kit", kitName)));
        lore.addAll(KitAdminMenu.describeInventoryCapture());
        if (kitLibrary.isDefaultKit(kitName) && matchRunning.getAsBoolean()) {
            lore.addAll(Messages.lines(KitActionsMessage.REPLACEMENT_MATCH_RUNNING));
        }
        lore.add(closingLine);
        return lore;
    }

    private void useAsDefaultKit(Player player, AdminMenuHolder holder, String kitName) {
        Optional<String> rejection = kitLibrary.findDefaultRejection(kitName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        kitLibrary.setDefaultKit(kitName);
        TournamentMessages.sendSuccess(player, Messages.text(KitActionsMessage.DEFAULT_KIT_SET,
                Placeholder.of("kit", kitName)));
        navigation.refresh(player, holder);
    }

    private void askForReplacementConfirmation(Player player, AdminMenuHolder holder, String kitName) {
        if (kitLibrary.findKit(kitName).isEmpty()) {
            TournamentMessages.sendError(player, KitLibrary.describeUnknownKit(kitName));
            return;
        }
        if (kitSaveService.holdsNoItem(player)) {
            TournamentMessages.sendError(player, KitSaveService.describeEmptyInventoryReplacement());
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.REPLACE_KIT, kitName,
                holder.getPageIndex()));
    }

    private void askForDeletionConfirmation(Player player, AdminMenuHolder holder, String kitName) {
        Optional<String> rejection = kitLibrary.findDeletionRejection(kitName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.DELETE_KIT, kitName,
                holder.getPageIndex()));
    }

    private void deleteKit(Player player, String kitName) {
        Optional<String> rejection = kitLibrary.findDeletionRejection(kitName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        kitLibrary.deleteKit(kitName);
        TournamentMessages.sendSuccess(player, Messages.text(KitActionsMessage.KIT_DELETED,
                Placeholder.of("kit", kitName)));
    }
}
