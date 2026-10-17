package de.simplepvparena.gui;

import de.simplepvparena.kit.KitSaveResult;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

final class KitAdminMenu implements AdminMenu {

    private static final int KIT_SAVE_SLOT = 13;

    private final AdminMenuNavigation navigation;
    private final KitSaveService kitSaveService;
    private final BooleanSupplier matchRunning;

    KitAdminMenu(AdminMenuNavigation navigation, KitSaveService kitSaveService, BooleanSupplier matchRunning) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.kitSaveService = Objects.requireNonNull(kitSaveService, "kitSaveService");
        this.matchRunning = Objects.requireNonNull(matchRunning, "matchRunning");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Kit";
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Kit", buildKitInfoLore()));
        canvas.set(KIT_SAVE_SLOT, MenuItems.create(Material.CHEST, "Save your inventory as the kit",
                buildKitSaveLore("Asks for a confirmation first")));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back("the main menu"));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case KIT_SAVE_SLOT -> navigation.open(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.SAVE_KIT, null, 0));
            case AdminMenuLayout.BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    AdminMenuConfirmation saveConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return describeSaveConfirmation();
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                saveKit(player);
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.kit();
            }
        };
    }

    private List<String> describeSaveConfirmation() {
        return buildKitSaveLore("The kit that stands in config.yml now is gone afterwards");
    }

    private void saveKit(Player player) {
        KitSaveResult result = kitSaveService.saveFromInventory(player);
        if (result.isSaved()) {
            TournamentMessages.sendSuccessReport(player, result.describe());
            return;
        }
        TournamentMessages.sendErrorReport(player, result.describe());
    }

    private List<String> buildKitInfoLore() {
        List<String> lore = new ArrayList<>(kitSaveService.describeKit());
        lore.add("Every fighter is given it when a round starts");
        return lore;
    }

    private List<String> buildKitSaveLore(String... closingLines) {
        List<String> lore = new ArrayList<>();
        lore.add("Takes your helmet, chestplate, leggings, boots,");
        lore.add("your offhand and the 36 slots of your inventory");
        lore.add("Enchantments, names, lore and damage are kept exactly");
        lore.add("An empty slot stays empty in the kit");
        lore.add("The item on your cursor is not taken, it lies in no slot");
        lore.add("It replaces the kit section of config.yml");
        if (matchRunning.getAsBoolean()) {
            lore.add("A match is running: its fighters keep their items,");
            lore.add("the new kit is handed out from the next round on");
        }
        lore.addAll(List.of(closingLines));
        return lore;
    }
}
