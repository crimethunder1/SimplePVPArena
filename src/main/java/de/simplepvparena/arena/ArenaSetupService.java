package de.simplepvparena.arena;

import de.simplepvparena.border.BorderWallOutline;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ArenaSetupService {

    private final ActiveArenaState activeArena;
    private final MatchService matchService;
    private final SpectatorBorderService spectatorBorderService;
    private final BorderWallPreview borderWallPreview;
    private final ArenaGapGuide arenaGapGuide;
    private final ArenaCleaningService arenaCleaningService;
    private final ArenaBaselineService arenaBaselineService;
    private final ArenaBorderConfirmation borderConfirmation;
    private final Runnable arenaConfigurationSaver;

    private int maximumArenaCellCount = ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT;

    public ArenaSetupService(ActiveArenaState activeArena,
                             MatchService matchService,
                             SpectatorBorderService spectatorBorderService,
                             BorderWallPreview borderWallPreview,
                             ArenaGapGuide arenaGapGuide,
                             ArenaCleaningService arenaCleaningService,
                             ArenaBaselineService arenaBaselineService,
                             ArenaBorderConfirmation borderConfirmation,
                             Runnable arenaConfigurationSaver) {
        this.activeArena = activeArena;
        this.matchService = matchService;
        this.spectatorBorderService = spectatorBorderService;
        this.borderWallPreview = borderWallPreview;
        this.arenaGapGuide = arenaGapGuide;
        this.arenaCleaningService = arenaCleaningService;
        this.arenaBaselineService = arenaBaselineService;
        this.borderConfirmation = borderConfirmation;
        this.arenaConfigurationSaver = arenaConfigurationSaver;
    }

    public void setMaximumArenaCellCount(int maximumArenaCellCount) {
        this.maximumArenaCellCount = Math.max(1, maximumArenaCellCount);
    }

    public int getMaximumArenaCellCount() {
        return maximumArenaCellCount;
    }

    public boolean isArenaReady() {
        return activeArena.isArenaFullyConfigured();
    }

    public Optional<ArenaShape> getShape() {
        return activeArena.getCleanedVolume();
    }

    public void calculateShape(Player admin) {
        calculateShape(admin, maximumArenaCellCount, false);
    }

    public void calculateShape(Player admin, int debugCellLimit) {
        calculateShape(admin, debugCellLimit, true);
    }

    private void calculateShape(Player admin, int cellLimit, boolean isDiagnosisRun) {
        if (matchService.hasActiveMatch()) {
            TournamentMessages.sendError(admin,
                    "A match is running, the fight area can only be calculated between matches");
            return;
        }
        if (isDiagnosisRun) {
            TournamentMessages.sendInfo(admin, "Calculating once with a limit of " + cellLimit + " cells"
                    + TournamentMessages.LINE_BREAK + "instead of the " + maximumArenaCellCount + " of "
                    + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH + ", config.yml is not changed");
        }
        Location seedLocation = admin.getLocation();
        World world = seedLocation.getWorld();
        BorderColumnReader borderColumns = spectatorBorderService.createColumnReader(world);
        ArenaShapeCalculation calculation = ArenaShapeFloodFill.calculate(world.getName(),
                seedLocation.getBlockX(), seedLocation.getBlockY(), seedLocation.getBlockZ(),
                cellLimit, borderColumns);
        if (!calculation.isSuccessful()) {
            TournamentMessages.sendError(admin, calculation.getRejectionMessage().orElseThrow());
            guideToGap(admin, world, seedLocation.getBlockY(), calculation);
            return;
        }
        arenaGapGuide.stopFor(admin);
        ArenaShape calculatedShape = calculation.getShape().orElseThrow();
        ArenaVolumeUpdate update = activeArena.adoptCalculation(calculatedShape);
        ArenaShape shape = update.volume();
        Arena arena = activeArena.getArena();
        borderConfirmation.confirmCalculation();
        arenaConfigurationSaver.run();
        TournamentMessages.sendSuccess(admin,
                "The fight area was calculated from the border blocks"
                        + TournamentMessages.LINE_BREAK + shape.describe());
        reportVolume(admin, update);
        reportDiagnosisLimit(admin, shape, cellLimit);
        captureBaseline(admin);
        previewRecognizedWall(admin, world, shape, borderColumns);
        reportSpawnPlacementWarnings(admin, arena);
        reportMissingConfiguration(admin, arena);
    }

    private void reportVolume(Player admin, ArenaVolumeUpdate update) {
        ArenaShape shape = update.volume();
        switch (update.origin()) {
            case MARKS_TAKEN_OVER -> TournamentMessages.sendInfo(admin,
                    "The marked heights were taken over: the fight area reaches from y "
                            + shape.getFloorY() + " to y " + shape.getWallTopY()
                            + " instead of the height of the border wall"
                            + TournamentMessages.LINE_BREAK + "Both marks are used up. The area keeps these two "
                            + "heights as long as it is calculated again as the same area, mark them again with "
                            + Arena.MARK_HEIGHT_ACTION + " to change them");
            case MARKS_KEPT -> TournamentMessages.sendInfo(admin,
                    "The same fight area was calculated again, so it keeps the "
                            + "heights that were marked for it: it reaches from y " + shape.getFloorY()
                            + " to y " + shape.getWallTopY() + " instead of the height of the border wall"
                            + TournamentMessages.LINE_BREAK + "Mark two heights with " + Arena.MARK_HEIGHT_ACTION
                            + " and calculate again to replace them");
            case BORDER_WALL -> reportVolumeReadOffTheWall(admin, update);
        }
    }

    private void reportVolumeReadOffTheWall(Player admin, ArenaVolumeUpdate update) {
        ArenaShape shape = update.volume();
        activeArena.getHeightMarkers().findIncompleteHint()
                .ifPresent(hint -> TournamentMessages.sendWarning(admin, hint));
        update.findDroppedMarkedVolume().ifPresent(marked ->
                TournamentMessages.sendWarning(admin, "This is not the fight area the heights y "
                        + marked.getFloorY() + " to y " + marked.getWallTopY()
                        + " were marked for any more, so they are gone"
                        + TournamentMessages.LINE_BREAK + "The fight area reaches from y " + shape.getFloorY()
                        + " to y " + shape.getWallTopY() + ", the height of its own border wall. Mark two "
                        + "heights with " + Arena.MARK_HEIGHT_ACTION + " and calculate again if it needs them"));
    }

    private void captureBaseline(Player admin) {
        ArenaShape shape = getShape().orElseThrow();
        ArenaShape capturedVolume = arenaBaselineService.findCapturedVolume(shape);
        if (!arenaBaselineService.startCapture(shape, report -> reportBaseline(admin, report))) {
            TournamentMessages.sendWarning(admin, "No baseline is taken of the fight area"
                    + TournamentMessages.LINE_BREAK
                    + (capturedVolume.getFloorY() < shape.getFloorY()
                            ? "The fight area with the layers under its floor holds " : "The fight area holds ")
                    + capturedVolume.getBlockCount() + " blocks, more than the "
                    + ArenaBaselineSnapshot.MAXIMUM_BLOCK_COUNT + " a baseline covers."
                    + TournamentMessages.LINE_BREAK + Arena.CLEAN_ACTION
                    + " sets every block of the fight area to air, the way it always did");
            return;
        }
        TournamentMessages.sendInfo(admin, "Reading the " + capturedVolume.getBlockCount()
                + " blocks from y " + capturedVolume.getFloorY() + " to y " + capturedVolume.getWallTopY()
                + (capturedVolume.getFloorY() < shape.getFloorY()
                        ? ", the fight area and the layers under its floor," : ", the fight area,")
                + " as the baseline " + Arena.CLEAN_ACTION + " restores"
                + TournamentMessages.LINE_BREAK + "In batches of " + arenaBaselineService.getBlocksPerTick()
                + " per tick, the arena cannot be cleaned until that is done."
                + TournamentMessages.LINE_BREAK + "The baseline is gone when the server restarts");
    }

    private void reportBaseline(Player admin, ArenaBaselineReport report) {
        if (report.isCaptured()) {
            TournamentMessages.sendSuccess(admin, report.describe());
            return;
        }
        TournamentMessages.sendWarning(admin, report.describe());
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
        String guidance = "A trail of flame particles points towards " + target.describe()
                + " while you walk."
                + TournamentMessages.LINE_BREAK + target.reason().getHandOutHint() + "."
                + TournamentMessages.LINE_BREAK + "The trail ends when you get there, the next "
                + "calculation replaces it with its own target";
        if (compassHandedOut) {
            TournamentMessages.sendInfo(admin, "You received a compass that points to " + target.describe()
                    + TournamentMessages.LINE_BREAK + guidance);
            return;
        }
        TournamentMessages.sendWarning(admin, "Your inventory is full, the compass towards "
                + target.describe() + " could not be handed over"
                + TournamentMessages.LINE_BREAK + guidance);
    }

    private void reportDiagnosisLimit(Player admin, ArenaShape shape, int cellLimit) {
        if (cellLimit <= maximumArenaCellCount || shape.getCellCount() <= maximumArenaCellCount) {
            return;
        }
        TournamentMessages.sendWarning(admin, "The wall is closed, the area is only larger than "
                + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH
                + TournamentMessages.LINE_BREAK + "Raise it to at least " + shape.getCellCount()
                + " in config.yml, otherwise the next calculation without a limit is rejected again");
    }

    private void previewRecognizedWall(Player player, World world, ArenaShape shape,
                                       BorderColumnReader borderColumns) {
        BorderWallOutline outline = BorderWallOutline.around(shape, borderColumns);
        if (!borderWallPreview.show(player, world, outline)) {
            return;
        }
        TournamentMessages.sendInfo(player, "The recognized wall is shown for a few seconds"
                + TournamentMessages.LINE_BREAK + outline.describe());
    }

    public void setSpawn(Player admin, ArenaSpawnType spawnType) {
        Arena arena = activeArena.getArena();
        PositionSnapshot position = PositionSnapshot.fromLocation(admin.getLocation());
        Optional<String> rejection = arena.setSpawn(spawnType, position);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(admin, rejection.get());
            return;
        }
        arenaConfigurationSaver.run();
        TournamentMessages.sendSuccess(admin, "Set the " + spawnType.getDisplayName() + " to "
                + position.describe());
        reportSpawnPlacementWarnings(admin, arena);
        reportMissingConfiguration(admin, arena);
    }

    public void clearConfiguration(CommandSender sender) {
        activeArena.clear();
        arenaBaselineService.discardSnapshot();
        arenaConfigurationSaver.run();
        TournamentMessages.sendSuccess(sender, "The arena configuration was removed"
                + TournamentMessages.LINE_BREAK + "Its blocks and the border blocks stay untouched");
    }

    public Optional<String> findCleaningRejection() {
        Optional<ArenaShape> volume = getShape();
        if (volume.isEmpty()) {
            return Optional.of("The fight area is not calculated, there is nothing to clean"
                    + TournamentMessages.LINE_BREAK + "Use " + Arena.CALCULATE_ACTION
                    + " while standing inside the border wall first");
        }
        if (matchService.hasActiveMatch()) {
            return Optional.of("A match is running, the arena can only be cleaned between matches");
        }
        if (arenaCleaningService.isCleaning()) {
            return Optional.of("The arena is already being cleaned");
        }
        if (arenaBaselineService.isCapturing()) {
            return Optional.of("The baseline of the fight area is still being read, wait a moment"
                    + TournamentMessages.LINE_BREAK + "A cleaning that started now would find no "
                    + "baseline and empty the arena instead of restoring it");
        }
        return ArenaCleaningPlan.findBaselineMismatch(volume.get(), arenaBaselineService.findStoredSnapshot());
    }

    public Optional<ArenaShape> findCleanedVolume() {
        return getShape().map(shape -> arenaBaselineService.findSnapshot(shape)
                .map(ArenaBaselineSnapshot::getCapturedVolume)
                .orElse(shape));
    }

    public long countCleaningBlocks() {
        return findCleanedVolume().map(ArenaCleaningService::countBlocks).orElse(0L);
    }

    public boolean hasBaseline() {
        return getShape().flatMap(arenaBaselineService::findSnapshot).isPresent();
    }

    public List<String> describeCleaningEffect() {
        Optional<ArenaBaselineSnapshot> baseline = getShape().flatMap(arenaBaselineService::findSnapshot);
        List<String> effect = new ArrayList<>(baseline.isPresent()
                ? List.of("Restores every block of the fight area to the",
                        "state the last calculation found",
                        "Blocks placed since then become air again,",
                        "broken ones come back")
                : List.of("No baseline is stored, every block of the",
                        "fight area is set to air",
                        "A baseline is taken by the calculation and is",
                        "gone after a server restart"));
        int depthBelowFloor = baseline.map(ArenaBaselineSnapshot::getDepthBelowFloor).orElse(0);
        findCleanedVolume().ifPresent(volume -> {
            if (depthBelowFloor == 0) {
                effect.add("Covers y " + volume.getFloorY() + " to y " + volume.getWallTopY()
                        + ", the height of the fight area");
                return;
            }
            effect.add("Covers y " + volume.getFloorY() + " to y " + volume.getWallTopY()
                    + ", the fight area and the");
            effect.add(depthBelowFloor + " layers under its floor, those are restored too");
        });
        return effect;
    }

    public boolean startCleaning(CommandSender sender) {
        Optional<String> rejection = findCleaningRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(sender, rejection.get());
            return false;
        }
        ArenaCleaningPlan plan = ArenaCleaningPlan.of(getShape().orElseThrow(),
                arenaBaselineService.findStoredSnapshot());
        ArenaShape shape = plan.getCleanedVolume();
        long blockCount = plan.getBlockCount();
        boolean cleaningStarted = arenaCleaningService.startCleaning(plan,
                report -> reportCleaningOutcome(sender, report));
        if (!cleaningStarted) {
            TournamentMessages.sendError(sender, "The arena cleaning could not be started");
            return false;
        }
        if (plan.restoresABaseline()) {
            TournamentMessages.sendInfo(sender, "Restoring the arena to the baseline of the last calculation"
                    + TournamentMessages.LINE_BREAK + plan.findBaseline().orElseThrow().describe());
        } else {
            TournamentMessages.sendWarning(sender, "No baseline of the fight area is stored, every block of "
                    + "it is set to air instead"
                    + TournamentMessages.LINE_BREAK + "A baseline is taken by " + Arena.CALCULATE_ACTION
                    + " and does not survive a server restart");
        }
        TournamentMessages.sendInfo(sender, "Cleaning the arena, " + blockCount + " blocks between y "
                + shape.getFloorY() + " and y " + shape.getWallTopY()
                + TournamentMessages.LINE_BREAK + "Processed in batches of "
                + arenaCleaningService.getBlocksPerTick() + " per tick"
                + TournamentMessages.LINE_BREAK + arenaCleaningService.describeAnimation());
        return true;
    }

    public Optional<String> startAutomaticCleaning(Consumer<String> completionHandler) {
        Optional<String> rejection = findCleaningRejection();
        if (rejection.isPresent()) {
            return rejection;
        }
        if (!hasBaseline()) {
            return Optional.of("no baseline of the fight area is stored, and a cleaning nobody asked "
                    + "for never empties the arena"
                    + TournamentMessages.LINE_BREAK + "Run " + Arena.CALCULATE_ACTION
                    + " once, that takes one");
        }
        ArenaCleaningPlan plan = ArenaCleaningPlan.of(getShape().orElseThrow(),
                arenaBaselineService.findStoredSnapshot());
        if (!arenaCleaningService.startCleaning(plan,
                report -> completionHandler.accept(report.describeOutcome()))) {
            return Optional.of("the arena cleaning could not be started");
        }
        return Optional.empty();
    }

    private static void reportCleaningOutcome(CommandSender sender, ArenaCleaningReport report) {
        if (report.isSuccessful()) {
            TournamentMessages.sendSuccess(sender, report.describeOutcome());
            return;
        }
        TournamentMessages.sendError(sender, report.describeOutcome());
    }

    public List<String> describeArena() {
        Arena arena = activeArena.getArena();
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

    private static void reportSpawnPlacementWarnings(CommandSender sender, Arena arena) {
        for (String warning : arena.findSpawnPlacementWarnings()) {
            TournamentMessages.sendWarning(sender, warning);
        }
    }

    private static void reportMissingConfiguration(CommandSender sender, Arena arena) {
        List<String> missingParts = arena.getMissingConfiguration();
        if (missingParts.isEmpty()) {
            return;
        }
        TournamentMessages.sendBlock(sender, TournamentMessages.warning("The arena still misses:"),
                missingParts);
    }
}
