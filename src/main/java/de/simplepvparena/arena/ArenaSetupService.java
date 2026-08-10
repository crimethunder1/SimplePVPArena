package de.simplepvparena.arena;

import de.simplepvparena.border.BorderWallOutline;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public final class ArenaSetupService {

    private final ArenaRegistry arenaRegistry;
    private final MatchService matchService;
    private final SpectatorBorderService spectatorBorderService;
    private final BorderWallPreview borderWallPreview;
    private final ArenaGapGuide arenaGapGuide;
    private final ArenaCleaningService arenaCleaningService;
    private final ArenaBorderConfirmation borderConfirmation;
    private final int maximumArenaCellCount;
    private final Runnable arenaConfigurationSaver;

    public ArenaSetupService(ArenaRegistry arenaRegistry,
                             MatchService matchService,
                             SpectatorBorderService spectatorBorderService,
                             BorderWallPreview borderWallPreview,
                             ArenaGapGuide arenaGapGuide,
                             ArenaCleaningService arenaCleaningService,
                             ArenaBorderConfirmation borderConfirmation,
                             int maximumArenaCellCount,
                             Runnable arenaConfigurationSaver) {
        this.arenaRegistry = arenaRegistry;
        this.matchService = matchService;
        this.spectatorBorderService = spectatorBorderService;
        this.borderWallPreview = borderWallPreview;
        this.arenaGapGuide = arenaGapGuide;
        this.arenaCleaningService = arenaCleaningService;
        this.borderConfirmation = borderConfirmation;
        this.maximumArenaCellCount = maximumArenaCellCount;
        this.arenaConfigurationSaver = arenaConfigurationSaver;
    }

    public boolean isArenaReady() {
        return arenaRegistry.isArenaFullyConfigured();
    }

    public Optional<ArenaShape> getShape() {
        return arenaRegistry.getArena().getShape();
    }

    public void calculateShape(Player admin) {
        calculateShape(admin, maximumArenaCellCount, false);
    }

    public void calculateShape(Player admin, int debugCellLimit) {
        calculateShape(admin, debugCellLimit, true);
    }

    private void calculateShape(Player admin, int cellLimit, boolean isDiagnosisRun) {
        if (matchService.hasActiveMatch()) {
            admin.sendMessage("A match is running in the arena, it can only be calculated between matches");
            return;
        }
        if (isDiagnosisRun) {
            admin.sendMessage("Calculating once with a limit of " + cellLimit + " cells instead of the "
                    + maximumArenaCellCount + " of " + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH
                    + ", config.yml is not changed by it");
        }
        Location seedLocation = admin.getLocation();
        World world = seedLocation.getWorld();
        BorderColumnReader borderColumns = spectatorBorderService.createColumnReader(world);
        ArenaShapeCalculation calculation = ArenaShapeFloodFill.calculate(world.getName(),
                seedLocation.getBlockX(), seedLocation.getBlockY(), seedLocation.getBlockZ(),
                cellLimit, borderColumns);
        if (!calculation.isSuccessful()) {
            admin.sendMessage(calculation.getRejectionMessage().orElseThrow());
            guideToGap(admin, world, seedLocation.getBlockY(), calculation);
            return;
        }
        arenaGapGuide.stopFor(admin);
        ArenaShape shape = calculation.getShape().orElseThrow();
        Arena arena = arenaRegistry.getArena();
        arena.setShape(shape);
        borderConfirmation.confirmCalculation();
        arenaConfigurationSaver.run();
        admin.sendMessage("The fight area was calculated from the border blocks: " + shape.describe());
        reportDiagnosisLimit(admin, shape, cellLimit);
        previewRecognizedWall(admin, world, shape, borderColumns);
        arena.findSpawnPlacementWarnings().forEach(admin::sendMessage);
        reportMissingConfiguration(admin, arena);
    }

    private void guideToGap(Player admin, World world, int seedY, ArenaShapeCalculation calculation) {
        if (calculation.getStatus() != ArenaShapeCalculation.Status.CELL_LIMIT_REACHED) {
            return;
        }
        Optional<ArenaFillDiagnostics> diagnostics = calculation.getDiagnostics()
                .filter(ArenaFillDiagnostics::hasSpread);
        if (diagnostics.isEmpty()) {
            return;
        }
        ArenaGapTarget target = ArenaGapTarget.fromDiagnostics(world.getName(), seedY, diagnostics.get());
        boolean compassHandedOut = arenaGapGuide.guideTo(admin, world, target);
        admin.sendMessage((compassHandedOut
                ? "You received a compass that points to " + target.describe()
                : "Your inventory is full, the compass towards " + target.describe()
                        + " could not be handed over")
                + ", and a trail of flame particles points the same way while you walk. That is the "
                + "column the fill reached first of the ones it was stopped on, not a hole anybody "
                + "found - start looking around there. The trail ends when you get there, and the next "
                + "calculation replaces both with its own target");
    }

    private void reportDiagnosisLimit(Player admin, ArenaShape shape, int cellLimit) {
        if (cellLimit <= maximumArenaCellCount || shape.getCellCount() <= maximumArenaCellCount) {
            return;
        }
        admin.sendMessage("The wall is closed, the area is only larger than "
                + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH + ". Raise it to at least "
                + shape.getCellCount() + " in config.yml, otherwise the next calculation without a limit "
                + "is rejected again");
    }

    private void previewRecognizedWall(Player player, World world, ArenaShape shape,
                                       BorderColumnReader borderColumns) {
        BorderWallOutline outline = BorderWallOutline.around(shape, borderColumns);
        if (!borderWallPreview.show(player, world, outline)) {
            return;
        }
        player.sendMessage("The recognized wall is shown for a few seconds: " + outline.describe());
    }

    public void setSpawn(Player admin, ArenaSpawnType spawnType) {
        Arena arena = arenaRegistry.getArena();
        PositionSnapshot position = PositionSnapshot.fromLocation(admin.getLocation());
        switch (spawnType) {
            case FIRST_TEAM -> arena.setFirstTeamSpawn(position);
            case SECOND_TEAM -> arena.setSecondTeamSpawn(position);
            case TRIBUNE -> arena.setTribuneSpawn(position);
        }
        arenaConfigurationSaver.run();
        admin.sendMessage("Set the " + spawnType.getDisplayName() + " of the arena to " + position.describe());
        arena.findSpawnPlacementWarnings().forEach(admin::sendMessage);
        reportMissingConfiguration(admin, arena);
    }

    public void clearConfiguration(CommandSender sender) {
        arenaRegistry.clearArenaConfiguration();
        arenaConfigurationSaver.run();
        sender.sendMessage("The arena configuration was removed, its blocks and border blocks stay untouched");
    }

    public Optional<String> findCleaningRejection() {
        if (arenaRegistry.getArena().getShape().isEmpty()) {
            return Optional.of("The fight area is not calculated, there is nothing to clean. Use "
                    + Arena.CALCULATE_ACTION + " while standing inside the border wall first");
        }
        if (matchService.hasActiveMatch()) {
            return Optional.of("A match is running in the arena, the arena can only be cleaned between matches");
        }
        if (arenaCleaningService.isCleaning()) {
            return Optional.of("The arena is already being cleaned");
        }
        return Optional.empty();
    }

    public long countCleaningBlocks() {
        return getShape().map(ArenaCleaningService::countBlocks).orElse(0L);
    }

    public boolean startCleaning(CommandSender sender) {
        Optional<String> rejection = findCleaningRejection();
        if (rejection.isPresent()) {
            sender.sendMessage(rejection.get());
            return false;
        }
        ArenaShape shape = getShape().orElseThrow();
        long blockCount = ArenaCleaningService.countBlocks(shape);
        boolean cleaningStarted = arenaCleaningService.startCleaning(shape,
                report -> sender.sendMessage("The arena is clean, " + report.describe()));
        if (!cleaningStarted) {
            sender.sendMessage("The arena cleaning could not be started");
            return false;
        }
        sender.sendMessage("Cleaning the arena, " + blockCount + " blocks are processed in batches of "
                + ArenaCleaningService.DEFAULT_BLOCKS_PER_TICK + " per tick");
        return true;
    }

    public List<String> describeArena() {
        Arena arena = arenaRegistry.getArena();
        return List.of(
                "Arena " + (arena.isFullyConfigured() ? "ready" : "incomplete"),
                "  fight area: " + arena.getShape().map(ArenaShape::describe).orElse("not calculated"),
                "  first team spawn: "
                        + arena.getConfiguredFirstTeamSpawn().map(PositionSnapshot::describe).orElse("not configured"),
                "  second team spawn: "
                        + arena.getConfiguredSecondTeamSpawn().map(PositionSnapshot::describe).orElse("not configured"),
                "  tribune spawn: "
                        + arena.getTribuneSpawn().map(PositionSnapshot::describe).orElse("not configured"),
                "  running match: " + matchService.getActiveMatch().map(Match::describe).orElse("none"));
    }

    private void reportMissingConfiguration(CommandSender sender, Arena arena) {
        List<String> missingParts = arena.getMissingConfiguration();
        if (!missingParts.isEmpty()) {
            sender.sendMessage("The arena still misses: " + String.join(", ", missingParts));
        }
    }
}
