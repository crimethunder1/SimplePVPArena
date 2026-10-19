package de.simplepvparena.gui;

import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitEquipmentSlot;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitSaveResult;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.LibraryKit;
import de.simplepvparena.kit.SerializedKit;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.KitMenuMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

final class KitAdminMenu implements AdminMenu {

    private static final int KIT_MENU_SIZE = 54;

    private static final int KITS_PREVIOUS_PAGE_SLOT = 45;
    private static final int KITS_INFO_SLOT = 47;
    private static final int KITS_BACK_SLOT = 49;
    private static final int KITS_CREATE_SLOT = 51;
    private static final int KITS_NEXT_PAGE_SLOT = 53;

    private final AdminMenuNavigation navigation;
    private final KitLibrary kitLibrary;
    private final KitSaveService kitSaveService;
    private final ChatInputService chatInputService;

    KitAdminMenu(AdminMenuNavigation navigation, KitLibrary kitLibrary, KitSaveService kitSaveService,
                 ChatInputService chatInputService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
        this.kitSaveService = Objects.requireNonNull(kitSaveService, "kitSaveService");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(KitMenuMessage.TITLE, Placeholder.of("page",
                MenuPagination.of(kitLibrary.getKitCount(), holder.getPageIndex()).getPageNumber()));
    }

    @Override
    public int size() {
        return KIT_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        List<String> kitNames = kitLibrary.getKitNames();
        MenuPagination pagination = MenuPagination.of(kitNames.size(), holder.getPageIndex());
        for (int slot = 0; slot < pagination.getItemCountOnPage(); slot++) {
            int kitSlot = slot;
            kitLibrary.findKit(kitNames.get(pagination.getFirstItemIndex() + slot))
                    .ifPresent(kit -> canvas.set(kitSlot, buildKitItem(kit)));
        }
        canvas.set(KITS_INFO_SLOT, MenuItems.info(Messages.text(KitMenuMessage.INFO_TITLE), List.of(
                Messages.plural(KitMenuMessage.KIT_COUNT, kitNames.size()),
                kitLibrary.getDefaultKitName()
                        .map(defaultKitName -> Messages.text(KitMenuMessage.DEFAULT_KIT,
                                Placeholder.of("kit", defaultKitName)))
                        .orElseGet(() -> Messages.text(KitMenuMessage.NO_DEFAULT_KIT)),
                Messages.text(MenuMessage.PAGE_OF_PAGES, Placeholder.of("page", pagination.getPageNumber()),
                        Placeholder.of("pageCount", pagination.getPageCount())),
                Messages.text(KitMenuMessage.INFO_HINT))));
        canvas.set(KITS_CREATE_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL,
                Messages.text(KitMenuMessage.CREATE_TITLE), buildCreationLore()));
        if (pagination.hasPreviousPage()) {
            canvas.set(KITS_PREVIOUS_PAGE_SLOT, MenuItems.previousPage(pagination));
        }
        if (pagination.hasNextPage()) {
            canvas.set(KITS_NEXT_PAGE_SLOT, MenuItems.nextPage(pagination));
        }
        canvas.set(KITS_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MAIN_MENU));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        List<String> kitNames = kitLibrary.getKitNames();
        MenuPagination pagination = MenuPagination.of(kitNames.size(), holder.getPageIndex());
        switch (slot) {
            case KITS_PREVIOUS_PAGE_SLOT -> {
                if (pagination.hasPreviousPage()) {
                    navigation.open(player, AdminMenuHolder.kit(pagination.getPreviousPageIndex()));
                }
            }
            case KITS_NEXT_PAGE_SLOT -> {
                if (pagination.hasNextPage()) {
                    navigation.open(player, AdminMenuHolder.kit(pagination.getNextPageIndex()));
                }
            }
            case KITS_CREATE_SLOT -> askForKitName(player, pagination.pageIndex());
            case KITS_INFO_SLOT -> { }
            case KITS_BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> openKitActions(player, kitNames, pagination, slot);
        }
    }

    static void reportSave(Player player, KitSaveResult result) {
        if (result.isSaved()) {
            TournamentMessages.sendSuccessReport(player, result.describe());
            return;
        }
        TournamentMessages.sendErrorReport(player, result.describe());
    }

    static List<String> describeItemCounts(LibraryKit kit) {
        return List.of(
                Messages.plural(KitMenuMessage.WORN_ITEM_COUNT, kit.wornItemCount(),
                        Placeholder.of("slotCount", KitEquipmentSlot.values().length)),
                Messages.plural(KitMenuMessage.INVENTORY_ITEM_COUNT, kit.inventoryItemCount(),
                        Placeholder.of("slotCount", SerializedKit.INVENTORY_SLOT_COUNT)));
    }

    static List<String> describeInventoryCapture() {
        return Messages.lines(KitMenuMessage.INVENTORY_CAPTURE);
    }

    private MenuTile buildKitItem(LibraryKit kit) {
        List<String> lore = new ArrayList<>(describeItemCounts(kit));
        if (kitLibrary.isDefaultKit(kit.name())) {
            lore.add(Messages.text(KitMenuMessage.KIT_DEFAULT_KIT));
        }
        lore.add(Messages.text(KitMenuMessage.KIT_LORE));
        return MenuItems.create(Material.CHEST, kit.name(), lore);
    }

    private List<String> buildCreationLore() {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.text(KitMenuMessage.CREATE_ASKS_FOR_NAME));
        lore.addAll(describeInventoryCapture());
        lore.add(Messages.text(KitMenuMessage.CREATE_FIRST_KIT));
        lore.add(Messages.text(CommonMessage.MENU_CLOSES_TO_TYPE));
        return lore;
    }

    private void openKitActions(Player player, List<String> kitNames, MenuPagination pagination, int slot) {
        OptionalInt kitIndex = pagination.findItemIndex(slot);
        if (kitIndex.isEmpty()) {
            return;
        }
        navigation.open(player, AdminMenuHolder.kitActions(kitNames.get(kitIndex.getAsInt()),
                pagination.pageIndex()));
    }

    private void askForKitName(Player player, int pageIndex) {
        navigation.close(player);
        chatInputService.await(player, Messages.text(KitMenuMessage.NAME_INPUT_DESCRIPTION),
                Messages.lines(KitMenuMessage.NAME_INPUT_PROMPT, Placeholder.of("rule", KitLibrary.describeNameRule())),
                line -> handleKitName(player, pageIndex, line));
    }

    private void handleKitName(Player player, int pageIndex, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        reportSave(player, kitSaveService.createFromInventory(player, line.trim()));
        navigation.open(player, AdminMenuHolder.kit(pageIndex));
    }
}
