package de.simplepvparena.gui;

import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.LibraryKit;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.tournament.KitChoiceService;
import de.simplepvparena.tournament.PendingKitChoice;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

final class KitChoiceAdminMenu implements AdminMenu {

    private static final int KIT_CHOICE_MENU_SIZE = 54;

    private static final int KIT_CHOICE_PREVIOUS_PAGE_SLOT = 45;
    private static final int KIT_CHOICE_INFO_SLOT = 49;
    private static final int KIT_CHOICE_NEXT_PAGE_SLOT = 53;

    private final AdminMenuNavigation navigation;
    private final KitLibrary kitLibrary;
    private final KitChoiceService kitChoiceService;

    KitChoiceAdminMenu(AdminMenuNavigation navigation, KitLibrary kitLibrary, KitChoiceService kitChoiceService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
        this.kitChoiceService = Objects.requireNonNull(kitChoiceService, "kitChoiceService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Choose the kit for " + holder.getSubject().orElseThrow();
    }

    @Override
    public int size() {
        return KIT_CHOICE_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        String matchId = holder.getSubject().orElseThrow();
        List<String> kitNames = kitLibrary.getKitNames();
        MenuPagination pagination = MenuPagination.of(kitNames.size(), holder.getPageIndex());
        for (int slot = 0; slot < pagination.getItemCountOnPage(); slot++) {
            int kitSlot = slot;
            kitLibrary.findKit(kitNames.get(pagination.getFirstItemIndex() + slot))
                    .ifPresent(kit -> canvas.set(kitSlot, buildKitItem(kit)));
        }
        canvas.set(KIT_CHOICE_INFO_SLOT, MenuItems.info("Kit choice", findChoiceOfMenu(matchId)
                .map(this::buildInfoLore)
                .orElseGet(() -> List.of("Match " + matchId, describeOverChoice(matchId)))));
        if (pagination.hasPreviousPage()) {
            canvas.set(KIT_CHOICE_PREVIOUS_PAGE_SLOT, MenuItems.create(MenuItems.PAGE_MATERIAL, "Previous page",
                    "Page " + (pagination.getPreviousPageIndex() + 1) + " of " + pagination.getPageCount()));
        }
        if (pagination.hasNextPage()) {
            canvas.set(KIT_CHOICE_NEXT_PAGE_SLOT, MenuItems.create(MenuItems.PAGE_MATERIAL, "Next page",
                    "Page " + (pagination.getNextPageIndex() + 1) + " of " + pagination.getPageCount()));
        }
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        String matchId = holder.getSubject().orElseThrow();
        List<String> kitNames = kitLibrary.getKitNames();
        MenuPagination pagination = MenuPagination.of(kitNames.size(), holder.getPageIndex());
        switch (slot) {
            case KIT_CHOICE_PREVIOUS_PAGE_SLOT -> {
                if (pagination.hasPreviousPage()) {
                    navigation.open(player, AdminMenuHolder.kitChoice(matchId, pagination.getPreviousPageIndex()));
                }
            }
            case KIT_CHOICE_NEXT_PAGE_SLOT -> {
                if (pagination.hasNextPage()) {
                    navigation.open(player, AdminMenuHolder.kitChoice(matchId, pagination.getNextPageIndex()));
                }
            }
            case KIT_CHOICE_INFO_SLOT -> { }
            default -> chooseKit(player, matchId, kitNames, pagination, slot);
        }
    }

    static String describeOverChoice(String matchId) {
        return "The kit choice for match " + matchId + " is over";
    }

    private MenuTile buildKitItem(LibraryKit kit) {
        List<String> lore = new ArrayList<>(KitAdminMenu.describeItemCounts(kit));
        if (kitLibrary.isDefaultKit(kit.name())) {
            lore.add("Default kit");
        }
        lore.add("Click to use it for this match");
        return MenuItems.create(Material.CHEST, kit.name(), lore);
    }

    private List<String> buildInfoLore(PendingKitChoice choice) {
        return List.of(
                "Match " + choice.matchId() + ": " + choice.teamsDescription(),
                "Default kit: " + choice.defaultKitName(),
                "Time left: " + kitChoiceService.getSecondsLeft() + " seconds",
                "The default kit is used when nobody chooses in time");
    }

    private Optional<PendingKitChoice> findChoiceOfMenu(String matchId) {
        return kitChoiceService.getPending().filter(choice -> choice.matchId().equals(matchId));
    }

    private void chooseKit(Player player, String matchId, List<String> kitNames, MenuPagination pagination,
                           int slot) {
        OptionalInt kitIndex = pagination.findItemIndex(slot);
        if (kitIndex.isEmpty()) {
            return;
        }
        boolean chosen = findChoiceOfMenu(matchId).isPresent()
                && kitChoiceService.choose(kitNames.get(kitIndex.getAsInt()), player.getName());
        navigation.close(player);
        if (!chosen) {
            TournamentMessages.sendWarning(player, describeOverChoice(matchId));
        }
    }
}
