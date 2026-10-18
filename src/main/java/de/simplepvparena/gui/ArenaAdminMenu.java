package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaDebugCellLimit;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.input.BlockItemRequest;
import de.simplepvparena.input.BlockItemRequestResult;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class ArenaAdminMenu implements AdminMenu {

    private static final int ARENA_CALCULATE_SLOT = 10;
    private static final int ARENA_DIAGNOSE_SLOT = 11;
    private static final int ARENA_FIRST_TEAM_SPAWN_SLOT = 12;
    private static final int ARENA_SECOND_TEAM_SPAWN_SLOT = 13;
    private static final int ARENA_TRIBUNE_SPAWN_SLOT = 14;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int ARENA_HEIGHT_MARKER_SLOT = 19;
    private static final int ARENA_START_GATE_SLOT = 20;
    private static final Material START_GATE_TILE_MATERIAL = Material.IRON_BARS;

    private final AdminMenuNavigation navigation;
    private final ArenaSetupService arenaSetupService;
    private final ArenaHeightMarkerService heightMarkerService;
    private final StartGateItemService gateItemService;
    private final ChatInputService chatInputService;

    ArenaAdminMenu(AdminMenuNavigation navigation, ArenaSetupService arenaSetupService,
                   ArenaHeightMarkerService heightMarkerService, StartGateItemService gateItemService,
                   ChatInputService chatInputService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.heightMarkerService = Objects.requireNonNull(heightMarkerService, "heightMarkerService");
        this.gateItemService = Objects.requireNonNull(gateItemService, "gateItemService");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Arena";
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Arena", arenaSetupService.describeArena()));
        canvas.set(ARENA_CALCULATE_SLOT, MenuItems.create(Material.SPYGLASS, "Calculate the fight area",
                "Runs the calculation for the position you stand on",
                "Stand inside the closed border wall before you click",
                "The menu closes so you can see the recognized wall"));
        canvas.set(ARENA_DIAGNOSE_SLOT, MenuItems.create(Material.REDSTONE_TORCH,
                "Diagnose with a higher cell limit",
                "One calculation with a limit you type, up to "
                        + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT + " cells",
                "Shows how far a fill through a gap really gets",
                "config.yml stays untouched"));
        canvas.set(ARENA_HEIGHT_MARKER_SLOT, MenuItems.create(ArenaHeightMarkerService.MARKER_MATERIAL,
                "Mark the height of the fight area", buildHeightMarkerLore()));
        canvas.set(ARENA_START_GATE_SLOT, MenuItems.create(START_GATE_TILE_MATERIAL,
                "Give start gate blocks", buildStartGateLore()));
        canvas.set(ARENA_FIRST_TEAM_SPAWN_SLOT, buildSpawnItem(Material.BLUE_BED, ArenaSpawnType.FIRST_TEAM));
        canvas.set(ARENA_SECOND_TEAM_SPAWN_SLOT, buildSpawnItem(Material.RED_BED, ArenaSpawnType.SECOND_TEAM));
        canvas.set(ARENA_TRIBUNE_SPAWN_SLOT, buildSpawnItem(Material.YELLOW_BED, ArenaSpawnType.TRIBUNE));
        canvas.set(ARENA_CLEAN_SLOT, MenuItems.create(Material.TNT, "Clean the arena", buildCleaningTileLore()));
        canvas.set(ARENA_RESET_SLOT, MenuItems.create(MenuItems.DESTRUCTIVE_MATERIAL, "Reset the arena",
                "Drops the fight area and all three spawns",
                "The blocks and the border blocks stay untouched",
                "Asks for a confirmation first"));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MAIN_MENU));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case ARENA_CALCULATE_SLOT -> {
                navigation.close(player);
                arenaSetupService.calculateShape(player);
            }
            case ARENA_DIAGNOSE_SLOT -> askForDebugCellLimitLine(player);
            case ARENA_HEIGHT_MARKER_SLOT -> handOutHeightMarker(player, holder);
            case ARENA_START_GATE_SLOT -> askForStartGateItemLine(player);
            case ARENA_FIRST_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.FIRST_TEAM);
            case ARENA_SECOND_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.SECOND_TEAM);
            case ARENA_TRIBUNE_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.TRIBUNE);
            case ARENA_CLEAN_SLOT -> askForCleaningConfirmation(player);
            case ARENA_RESET_SLOT -> askForResetConfirmation(player);
            case AdminMenuLayout.BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    AdminMenuConfirmation cleanConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return describeCleanConfirmation();
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                startCleaning(player);
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.arena();
            }
        };
    }

    AdminMenuConfirmation resetConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return describeResetConfirmation();
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                clearConfiguration(player);
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.arena();
            }
        };
    }

    private List<String> describeCleanConfirmation() {
        return buildCleaningLore("The change cannot be undone",
                "The border wall around it stays untouched");
    }

    private List<String> describeResetConfirmation() {
        return List.of(
                "The fight area and all three spawns are dropped",
                "The blocks of the arena and the border blocks stay untouched",
                "No tournament can be started until it is calculated again");
    }

    private void startCleaning(Player player) {
        arenaSetupService.startCleaning(player);
    }

    private void clearConfiguration(Player player) {
        arenaSetupService.clearConfiguration(player);
    }

    private void setSpawn(Player player, AdminMenuHolder holder, ArenaSpawnType spawnType) {
        arenaSetupService.setSpawn(player, spawnType);
        navigation.refresh(player, holder);
    }

    private void handOutHeightMarker(Player player, AdminMenuHolder holder) {
        Map<Integer, ItemStack> itemsThatDidNotFit = player.getInventory()
                .addItem(heightMarkerService.createMarkerItem());
        if (!itemsThatDidNotFit.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Your inventory is full, the height marker could not be handed out");
            return;
        }
        TournamentMessages.sendSuccess(player, "You received the height marker of the fight area"
                + TournamentMessages.LINE_BREAK + "Place a block with it at the lowest point of the area, "
                + "the item switches to the highest one by itself"
                + TournamentMessages.LINE_BREAK + "The placed block disappears again, only its height is kept");
        navigation.refresh(player, holder);
    }

    private void askForStartGateItemLine(Player player) {
        navigation.close(player);
        chatInputService.await(player, "the start gate blocks", List.of(
                        "Type: " + BlockItemRequest.USAGE + " in chat",
                        "Example: barrier 32",
                        "The amount has to be between " + BlockItemRequest.MINIMUM_AMOUNT
                                + " and " + BlockItemRequest.MAXIMUM_AMOUNT),
                line -> handleStartGateItemLine(player, line));
    }

    private void handleStartGateItemLine(Player player, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        BlockItemRequestResult requestResult = BlockItemRequest.parseLine(line,
                ArenaAdminMenu::isPlaceableBlock);
        if (!requestResult.isAccepted()) {
            TournamentMessages.sendError(player, requestResult.getRejectionMessage().orElseThrow());
            navigation.open(player, AdminMenuHolder.arena());
            return;
        }
        BlockItemRequest request = requestResult.getRequest().orElseThrow();
        Map<Integer, ItemStack> itemsThatDidNotFit = player.getInventory()
                .addItem(gateItemService.createGateItem(request));
        if (!itemsThatDidNotFit.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Your inventory is full, not every gate block could be handed out");
        }
        TournamentMessages.sendSuccess(player, "You received " + request.amount() + " "
                + request.material().name() + " start gate blocks"
                + TournamentMessages.LINE_BREAK
                + "Build the box around a team spawn out of whatever your map is made of and close it "
                + "with these blocks"
                + TournamentMessages.LINE_BREAK
                + "Every one of them you place is part of the gate right away, breaking one takes it "
                + "out again"
                + TournamentMessages.LINE_BREAK
                + "Every round start puts the whole gate back and takes it apart again from the top");
    }

    private void askForCleaningConfirmation(Player player) {
        Optional<String> rejection = arenaSetupService.findCleaningRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.CLEAN_ARENA, null, 0));
    }

    private void askForResetConfirmation(Player player) {
        Optional<String> rejection = arenaSetupService.findResetRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.RESET_ARENA, null, 0));
    }

    private List<String> buildHeightMarkerLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Hands you the item that marks the lowest and the");
        lore.add("highest point of the fight area");
        lore.add("Place a block with it, only its height is kept");
        lore.add("and the block disappears again");
        lore.add("The next calculation takes them over as the floor");
        lore.add("and the ceiling of the fight area, also below the");
        lore.add("border wall, and uses both marks up");
        lore.addAll(heightMarkerService.describeMarkers());
        return lore;
    }

    private List<String> buildStartGateLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Asks for a material and an amount in the chat and");
        lore.add("hands you that many start gate blocks");
        lore.add("Build a box around each team spawn and close it");
        lore.add("with them: every placed one is part of the gate,");
        lore.add("breaking one takes it out again");
        lore.add("Every round start puts the whole gate back and takes");
        lore.add("it apart again from the top, with a countdown");
        lore.add("A gate nobody placed is skipped, the round still starts");
        lore.addAll(gateItemService.describeGate());
        return lore;
    }

    private List<String> buildCleaningTileLore() {
        if (arenaSetupService.getShape().isEmpty()) {
            return List.of("No fight area is calculated, there is nothing to clean");
        }
        return buildCleaningLore("Asks for a confirmation first");
    }

    private List<String> buildCleaningLore(String... closingLines) {
        List<String> lore = new ArrayList<>(arenaSetupService.describeCleaningEffect());
        lore.add(arenaSetupService.countCleaningBlocks() + " blocks are affected");
        lore.addAll(List.of(closingLines));
        return lore;
    }

    private MenuTile buildSpawnItem(Material material, ArenaSpawnType spawnType) {
        return MenuItems.create(material, "Set the " + spawnType.getDisplayName(),
                "Sets it to the position you stand on",
                "Stand " + spawnType.getPlacementHint(),
                "A position on the wrong side of the fight area",
                "is refused once the area is calculated");
    }

    private static boolean isPlaceableBlock(Material material) {
        return material.isBlock() && material.isItem() && !material.isAir();
    }

    private void askForDebugCellLimitLine(Player player) {
        navigation.close(player);
        chatInputService.await(player, "the diagnosis cell limit", List.of(
                        "Type: " + ArenaDebugCellLimit.USAGE + " in chat",
                        "A number between " + ArenaDebugCellLimit.MINIMUM_CELL_COUNT + " and "
                                + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT,
                        "Stand inside the border wall, the calculation runs from where you stand",
                        "config.yml is not changed by it"),
                line -> handleDebugCellLimitLine(player, line));
    }

    private void handleDebugCellLimitLine(Player player, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        ArenaDebugCellLimit debugCellLimit = ArenaDebugCellLimit.parseLine(line);
        if (!debugCellLimit.isAccepted()) {
            TournamentMessages.sendError(player, debugCellLimit.getRejectionMessage().orElseThrow());
            navigation.open(player, AdminMenuHolder.arena());
            return;
        }
        arenaSetupService.calculateShape(player, debugCellLimit.getCellCount());
    }
}
