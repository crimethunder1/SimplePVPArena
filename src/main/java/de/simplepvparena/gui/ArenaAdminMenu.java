package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaDebugCellLimit;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.ConfirmedBorderParticleWriter;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.input.BlockItemRequest;
import de.simplepvparena.input.BlockItemRequestResult;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
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
    private static final int ARENA_BORDER_PARTICLES_SLOT = 21;
    private static final Material START_GATE_TILE_MATERIAL = Material.IRON_BARS;
    private static final Material BORDER_PARTICLES_TILE_MATERIAL = Material.BLAZE_POWDER;

    private final AdminMenuNavigation navigation;
    private final ArenaSetupService arenaSetupService;
    private final ArenaHeightMarkerService heightMarkerService;
    private final StartGateItemService gateItemService;
    private final ChatInputService chatInputService;
    private final BorderParticleService borderParticleService;
    private final ConfirmedBorderParticleWriter confirmedBorderParticleWriter;

    ArenaAdminMenu(AdminMenuNavigation navigation, ArenaSetupService arenaSetupService,
                   ArenaHeightMarkerService heightMarkerService, StartGateItemService gateItemService,
                   ChatInputService chatInputService, BorderParticleService borderParticleService,
                   ConfirmedBorderParticleWriter confirmedBorderParticleWriter) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.heightMarkerService = Objects.requireNonNull(heightMarkerService, "heightMarkerService");
        this.gateItemService = Objects.requireNonNull(gateItemService, "gateItemService");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
        this.borderParticleService = Objects.requireNonNull(borderParticleService, "borderParticleService");
        this.confirmedBorderParticleWriter = Objects.requireNonNull(confirmedBorderParticleWriter,
                "confirmedBorderParticleWriter");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(ArenaMenuMessage.TITLE);
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(Messages.text(ArenaMenuMessage.INFO_TITLE),
                arenaSetupService.describeArena()));
        canvas.set(ARENA_CALCULATE_SLOT, MenuItems.create(Material.SPYGLASS,
                Messages.text(ArenaMenuMessage.CALCULATE_TITLE), Messages.lines(ArenaMenuMessage.CALCULATE_LORE)));
        canvas.set(ARENA_DIAGNOSE_SLOT, MenuItems.create(Material.REDSTONE_TORCH,
                Messages.text(ArenaMenuMessage.DIAGNOSE_TITLE), buildDiagnoseLore()));
        canvas.set(ARENA_HEIGHT_MARKER_SLOT, MenuItems.create(ArenaHeightMarkerService.MARKER_MATERIAL,
                Messages.text(ArenaMenuMessage.HEIGHT_MARKER_TITLE), buildHeightMarkerLore()));
        canvas.set(ARENA_START_GATE_SLOT, MenuItems.create(START_GATE_TILE_MATERIAL,
                Messages.text(ArenaMenuMessage.START_GATE_TITLE), buildStartGateLore()));
        canvas.set(ARENA_BORDER_PARTICLES_SLOT, MenuItems.create(BORDER_PARTICLES_TILE_MATERIAL,
                Messages.text(ArenaMenuMessage.BORDER_PARTICLES_TITLE), buildBorderParticlesLore()));
        canvas.set(ARENA_FIRST_TEAM_SPAWN_SLOT, buildSpawnItem(Material.BLUE_BED, ArenaSpawnType.FIRST_TEAM,
                ArenaMenuMessage.FIRST_TEAM_SPAWN_TITLE));
        canvas.set(ARENA_SECOND_TEAM_SPAWN_SLOT, buildSpawnItem(Material.RED_BED, ArenaSpawnType.SECOND_TEAM,
                ArenaMenuMessage.SECOND_TEAM_SPAWN_TITLE));
        canvas.set(ARENA_TRIBUNE_SPAWN_SLOT, buildSpawnItem(Material.YELLOW_BED, ArenaSpawnType.TRIBUNE,
                ArenaMenuMessage.TRIBUNE_SPAWN_TITLE));
        canvas.set(ARENA_CLEAN_SLOT, MenuItems.create(Material.TNT, Messages.text(ArenaMenuMessage.CLEAN_TITLE),
                buildCleaningTileLore()));
        canvas.set(ARENA_RESET_SLOT, MenuItems.create(MenuItems.DESTRUCTIVE_MATERIAL,
                Messages.text(ArenaMenuMessage.RESET_TITLE), buildResetTileLore()));
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
            case ARENA_BORDER_PARTICLES_SLOT -> switchBorderParticles(player, holder);
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
        return buildCleaningLore(Messages.lines(ArenaMenuMessage.CLEAN_CONFIRMATION));
    }

    private List<String> describeResetConfirmation() {
        return Messages.lines(ArenaMenuMessage.RESET_CONFIRMATION);
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
            TournamentMessages.sendWarning(player, Messages.text(ArenaMenuMessage.HEIGHT_MARKER_INVENTORY_FULL));
            return;
        }
        TournamentMessages.sendSuccess(player, Messages.text(ArenaMenuMessage.HEIGHT_MARKER_HANDED_OUT));
        navigation.refresh(player, holder);
    }

    private void askForStartGateItemLine(Player player) {
        navigation.close(player);
        chatInputService.await(player, Messages.text(ArenaMenuMessage.START_GATE_INPUT_DESCRIPTION),
                Messages.lines(ArenaMenuMessage.START_GATE_INPUT_PROMPT,
                        Placeholder.of("usage", BlockItemRequest.describeUsage()),
                        Placeholder.of("minimum", BlockItemRequest.MINIMUM_AMOUNT),
                        Placeholder.of("maximum", BlockItemRequest.MAXIMUM_AMOUNT)),
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
            TournamentMessages.sendWarning(player, Messages.text(ArenaMenuMessage.START_GATE_INVENTORY_FULL));
        }
        TournamentMessages.sendSuccess(player, Messages.plural(ArenaMenuMessage.START_GATE_HANDED_OUT,
                request.amount(), Placeholder.of("material", request.material().name())));
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

    private List<String> buildDiagnoseLore() {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.plural(ArenaMenuMessage.DIAGNOSE_LIMIT, ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT));
        lore.addAll(Messages.lines(ArenaMenuMessage.DIAGNOSE_LORE));
        return lore;
    }

    private List<String> buildHeightMarkerLore() {
        List<String> lore = new ArrayList<>(Messages.lines(ArenaMenuMessage.HEIGHT_MARKER_LORE));
        lore.addAll(heightMarkerService.describeMarkers());
        return lore;
    }

    private List<String> buildStartGateLore() {
        List<String> lore = new ArrayList<>(Messages.lines(ArenaMenuMessage.START_GATE_LORE));
        lore.addAll(gateItemService.describeGate());
        return lore;
    }

    private List<String> buildBorderParticlesLore() {
        boolean enabled = borderParticleService.isConfirmedParticleEnabled();
        List<String> lore = new ArrayList<>();
        lore.add(Messages.text(enabled
                ? ArenaMenuMessage.BORDER_PARTICLES_STATE_SHOWN
                : ArenaMenuMessage.BORDER_PARTICLES_STATE_HIDDEN));
        lore.addAll(Messages.lines(ArenaMenuMessage.BORDER_PARTICLES_LORE));
        lore.add(Messages.text(enabled
                ? ArenaMenuMessage.BORDER_PARTICLES_CLICK_TO_HIDE
                : ArenaMenuMessage.BORDER_PARTICLES_CLICK_TO_SHOW));
        return lore;
    }

    private void switchBorderParticles(Player player, AdminMenuHolder holder) {
        boolean enabled = !borderParticleService.isConfirmedParticleEnabled();
        borderParticleService.setConfirmedParticleEnabled(enabled);
        try {
            confirmedBorderParticleWriter.write(enabled);
            TournamentMessages.sendSuccess(player, Messages.text(enabled
                    ? ArenaMenuMessage.BORDER_PARTICLES_SWITCHED_TO_SHOWN
                    : ArenaMenuMessage.BORDER_PARTICLES_SWITCHED_TO_HIDDEN));
        } catch (IOException failure) {
            TournamentMessages.sendWarning(player, Messages.text(enabled
                    ? ArenaMenuMessage.BORDER_PARTICLES_SWITCHED_TO_SHOWN_NOT_WRITTEN
                    : ArenaMenuMessage.BORDER_PARTICLES_SWITCHED_TO_HIDDEN_NOT_WRITTEN,
                    Placeholder.of("reason", describeFailure(failure))));
        }
        navigation.refresh(player, holder);
    }

    private static String describeFailure(IOException failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private List<String> buildResetTileLore() {
        List<String> lore = new ArrayList<>(Messages.lines(ArenaMenuMessage.RESET_LORE));
        lore.add(Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION));
        return lore;
    }

    private List<String> buildCleaningTileLore() {
        if (arenaSetupService.getShape().isEmpty()) {
            return List.of(Messages.text(ArenaMenuMessage.CLEAN_WITHOUT_FIGHT_AREA));
        }
        return buildCleaningLore(List.of(Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION)));
    }

    private List<String> buildCleaningLore(List<String> closingLines) {
        List<String> lore = new ArrayList<>(arenaSetupService.describeCleaningEffect());
        lore.add(Messages.plural(ArenaMenuMessage.AFFECTED_BLOCKS, arenaSetupService.countCleaningBlocks()));
        lore.addAll(closingLines);
        return lore;
    }

    private MenuTile buildSpawnItem(Material material, ArenaSpawnType spawnType, MessageKey title) {
        return MenuItems.create(material, Messages.text(title), Messages.lines(ArenaMenuMessage.SPAWN_LORE,
                Placeholder.of("placement", spawnType.getPlacementHint())));
    }

    private static boolean isPlaceableBlock(Material material) {
        return material.isBlock() && material.isItem() && !material.isAir();
    }

    private void askForDebugCellLimitLine(Player player) {
        navigation.close(player);
        chatInputService.await(player, Messages.text(ArenaMenuMessage.DIAGNOSIS_INPUT_DESCRIPTION),
                Messages.lines(ArenaMenuMessage.DIAGNOSIS_INPUT_PROMPT,
                        Placeholder.of("usage", ArenaDebugCellLimit.describeUsage()),
                        Placeholder.of("minimum", ArenaDebugCellLimit.MINIMUM_CELL_COUNT),
                        Placeholder.of("maximum", ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT)),
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
