package de.simplepvparena.gui;

import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.LibraryKit;
import de.simplepvparena.message.MenuMessage;
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
        return "Kit " + holder.getSubject().orElseThrow();
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
                .orElse(List.of("This kit does not exist any more"))));
        canvas.set(KIT_DEFAULT_SLOT, buildDefaultItem(kitName));
        canvas.set(KIT_REPLACE_SLOT, MenuItems.create(Material.CHEST, "Replace with your inventory",
                buildReplacementLore(kitName, "Asks for a confirmation first")));
        canvas.set(KIT_DELETE_SLOT, kitLibrary.findDeletionRejection(kitName)
                .map(rejection -> MenuItems.unavailable("Delete the kit", rejection))
                .orElseGet(() -> MenuItems.create(MenuItems.REFUSE_MATERIAL, "Delete the kit",
                        "Removes the kit " + kitName, "Asks for a confirmation first")));
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
                        "The items the kit holds now are gone afterwards");
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
                return List.of(
                        "Kit " + holder.getSubject().orElseThrow() + " is deleted",
                        "Its items are gone afterwards");
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
                ? "Default kit: every fighter is given it when a round starts"
                : "Not the default kit, no fighter is given it");
        return lore;
    }

    private MenuTile buildDefaultItem(String kitName) {
        Optional<String> rejection = kitLibrary.findDefaultRejection(kitName);
        if (rejection.isPresent()) {
            return MenuItems.unavailable(kitLibrary.isDefaultKit(kitName)
                    ? "This is the default kit"
                    : "Use as the default kit", rejection.get());
        }
        return MenuItems.create(Material.NETHER_STAR, "Use as the default kit",
                "Every fighter is given it from the next round on",
                "A running round keeps the kit its fighters were given");
    }

    private List<String> buildReplacementLore(String kitName, String closingLine) {
        List<String> lore = new ArrayList<>();
        lore.add("Your inventory becomes the kit " + kitName);
        lore.addAll(KitAdminMenu.describeInventoryCapture());
        if (kitLibrary.isDefaultKit(kitName) && matchRunning.getAsBoolean()) {
            lore.add("A match is running: its fighters keep their items,");
            lore.add("the new kit is handed out from the next round on");
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
        TournamentMessages.sendSuccess(player, "Kit " + kitName + " is the default kit now, every fighter is "
                + "given it from the next round on");
        navigation.refresh(player, holder);
    }

    private void askForReplacementConfirmation(Player player, AdminMenuHolder holder, String kitName) {
        if (kitLibrary.findKit(kitName).isEmpty()) {
            TournamentMessages.sendError(player, "Unknown kit " + kitName);
            return;
        }
        if (kitSaveService.holdsNoItem(player)) {
            TournamentMessages.sendError(player, KitSaveService.EMPTY_INVENTORY_REPLACEMENT_MESSAGE);
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
        TournamentMessages.sendSuccess(player, "Kit " + kitName + " deleted");
    }
}
