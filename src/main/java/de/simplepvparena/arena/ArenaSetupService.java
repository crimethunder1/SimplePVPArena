package de.simplepvparena.arena;

import de.simplepvparena.border.BorderWallOutline;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
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
    private BooleanSupplier arenaReservedForMatch = () -> false;

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

    public void setArenaReservedForMatch(BooleanSupplier arenaReservedForMatch) {
        this.arenaReservedForMatch = Objects.requireNonNull(arenaReservedForMatch, "arenaReservedForMatch");
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
            TournamentMessages.sendError(admin, Messages.text(ArenaSetupMessage.CALCULATION_DURING_MATCH));
            return;
        }
        if (arenaReservedForMatch.getAsBoolean()) {
            TournamentMessages.sendError(admin, describeArenaReservedForMatch());
            return;
        }
        if (isDiagnosisRun) {
            TournamentMessages.sendInfo(admin, Messages.plural(ArenaSetupMessage.DIAGNOSIS_RUN, cellLimit,
                    Placeholder.of("maximumCellCount", maximumArenaCellCount),
                    Placeholder.of("path", ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH)));
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
        TournamentMessages.sendSuccess(admin, Messages.text(ArenaSetupMessage.CALCULATED,
                Placeholder.of("fightArea", shape.describe())));
        reportVolume(admin, update);
        reportDiagnosisLimit(admin, shape, cellLimit);
        captureBaseline(admin);
        previewRecognizedWall(admin, world, shape, borderColumns);
        reportSpawnPlacementWarnings(admin, arena);
        reportTribuneOutsideOfTheEffectArea(admin);
        reportMissingConfiguration(admin, arena);
    }

    private void reportTribuneOutsideOfTheEffectArea(Player admin) {
        activeArena.findTribuneOutsideOfTheEffectArea().ifPresent(warning -> TournamentMessages.sendWarning(admin,
                Messages.text(ArenaSetupMessage.TRIBUNE_OUTSIDE_AFTER_CALCULATION, Placeholder.of("warning", warning),
                        Placeholder.of("path", EffectArea.MARGIN_CONFIGURATION_PATH))));
    }

    private void reportVolume(Player admin, ArenaVolumeUpdate update) {
        ArenaShape shape = update.volume();
        switch (update.origin()) {
            case MARKS_TAKEN_OVER -> TournamentMessages.sendInfo(admin,
                    describeMarkedHeights(ArenaSetupMessage.MARKED_HEIGHTS_TAKEN_OVER, shape));
            case MARKS_KEPT -> TournamentMessages.sendInfo(admin,
                    describeMarkedHeights(ArenaSetupMessage.MARKED_HEIGHTS_KEPT, shape));
            case BORDER_WALL -> reportVolumeReadOffTheWall(admin, update);
        }
    }

    private static String describeMarkedHeights(MessageKey sentence, ArenaShape shape) {
        return Messages.text(sentence, Placeholder.of("floorY", shape.getFloorY()),
                Placeholder.of("topY", shape.getWallTopY()), markHeightAction());
    }

    private void reportVolumeReadOffTheWall(Player admin, ArenaVolumeUpdate update) {
        ArenaShape shape = update.volume();
        activeArena.getHeightMarkers().findIncompleteHint()
                .ifPresent(hint -> TournamentMessages.sendWarning(admin, hint));
        update.findDroppedMarkedVolume().ifPresent(marked ->
                TournamentMessages.sendWarning(admin, Messages.text(ArenaSetupMessage.MARKED_HEIGHTS_DROPPED,
                        Placeholder.of("markedFloorY", marked.getFloorY()),
                        Placeholder.of("markedTopY", marked.getWallTopY()),
                        Placeholder.of("floorY", shape.getFloorY()), Placeholder.of("topY", shape.getWallTopY()),
                        markHeightAction())));
    }

    private void captureBaseline(Player admin) {
        ArenaShape shape = getShape().orElseThrow();
        ArenaShape capturedVolume = arenaBaselineService.findCapturedVolume(shape);
        boolean coversLayersUnderTheFloor = capturedVolume.getFloorY() < shape.getFloorY();
        Placeholder cleanAction = Placeholder.of("action", Arena.describeArenaMenuTile(ArenaMenuMessage.CLEAN_TITLE));
        if (!arenaBaselineService.startCapture(shape, report -> reportBaseline(admin, report))) {
            TournamentMessages.sendWarning(admin, Messages.plural(coversLayersUnderTheFloor
                            ? ArenaSetupMessage.BASELINE_TOO_LARGE_WITH_LAYERS : ArenaSetupMessage.BASELINE_TOO_LARGE,
                    capturedVolume.getBlockCount(),
                    Placeholder.of("maximum", ArenaBaselineSnapshot.MAXIMUM_BLOCK_COUNT), cleanAction));
            return;
        }
        TournamentMessages.sendInfo(admin, Messages.plural(coversLayersUnderTheFloor
                        ? ArenaSetupMessage.BASELINE_READING_WITH_LAYERS : ArenaSetupMessage.BASELINE_READING,
                capturedVolume.getBlockCount(), Placeholder.of("floorY", capturedVolume.getFloorY()),
                Placeholder.of("topY", capturedVolume.getWallTopY()), cleanAction,
                Placeholder.of("batchSize", arenaBaselineService.getBlocksPerTick())));
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
        Placeholder targetPosition = Placeholder.of("target", target.describe());
        String guidance = Messages.text(ArenaSetupMessage.GAP_GUIDANCE, targetPosition,
                Placeholder.of("hint", target.reason().getHandOutHint()));
        if (compassHandedOut) {
            TournamentMessages.sendInfo(admin, Messages.text(ArenaSetupMessage.GAP_COMPASS_RECEIVED, targetPosition)
                    + TournamentMessages.LINE_BREAK + guidance);
            return;
        }
        TournamentMessages.sendWarning(admin, Messages.text(ArenaSetupMessage.GAP_COMPASS_NOT_HANDED_OVER,
                targetPosition) + TournamentMessages.LINE_BREAK + guidance);
    }

    private void reportDiagnosisLimit(Player admin, ArenaShape shape, int cellLimit) {
        if (cellLimit <= maximumArenaCellCount || shape.getCellCount() <= maximumArenaCellCount) {
            return;
        }
        TournamentMessages.sendWarning(admin, Messages.text(ArenaSetupMessage.DIAGNOSIS_LIMIT_TOO_LOW,
                Placeholder.of("path", ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH),
                Placeholder.of("cellCount", shape.getCellCount())));
    }

    private void previewRecognizedWall(Player player, World world, ArenaShape shape,
                                       BorderColumnReader borderColumns) {
        BorderWallOutline outline = BorderWallOutline.around(shape, borderColumns);
        if (!borderWallPreview.show(player, world, outline)) {
            return;
        }
        TournamentMessages.sendInfo(player, Messages.text(ArenaSetupMessage.RECOGNIZED_WALL_SHOWN,
                Placeholder.of("outline", outline.describe())));
    }

    public void setSpawn(Player admin, ArenaSpawnType spawnType) {
        Arena arena = activeArena.getArena();
        PositionSnapshot position = PositionSnapshot.fromLocation(admin.getLocation());
        Optional<String> rejection = findEffectAreaRejection(spawnType, position)
                .or(() -> arena.setSpawn(spawnType, position));
        if (rejection.isPresent()) {
            TournamentMessages.sendError(admin, rejection.get());
            return;
        }
        arenaConfigurationSaver.run();
        TournamentMessages.sendSuccess(admin, Messages.text(ArenaSetupMessage.SPAWN_SET,
                Placeholder.of("spawn", spawnType.getDisplayName()), Placeholder.of("position", position.describe())));
        reportSpawnPlacementWarnings(admin, arena);
        reportMissingConfiguration(admin, arena);
    }

    private Optional<String> findEffectAreaRejection(ArenaSpawnType spawnType, PositionSnapshot position) {
        if (spawnType != ArenaSpawnType.TRIBUNE) {
            return Optional.empty();
        }
        return activeArena.findEffectArea()
                .filter(effectArea -> !effectArea.contains(position.worldName(), position.x(), position.z()))
                .map(effectArea -> Messages.text(ArenaSetupMessage.SPAWN_OUTSIDE_OF_THE_EFFECT_AREA,
                        Placeholder.of("spawn", spawnType.getDisplayName()),
                        Placeholder.of("position", position.describe()),
                        Placeholder.of("distance",
                                effectArea.describeDistanceOf(position.worldName(), position.x(), position.z())),
                        Placeholder.of("path", EffectArea.MARGIN_CONFIGURATION_PATH)));
    }

    public Optional<String> findResetRejection() {
        if (matchService.hasActiveMatch()) {
            return Optional.of(Messages.text(ArenaSetupMessage.RESET_DURING_MATCH));
        }
        if (arenaReservedForMatch.getAsBoolean()) {
            return Optional.of(describeArenaReservedForMatch());
        }
        return Optional.empty();
    }

    private static String describeArenaReservedForMatch() {
        return Messages.text(ArenaSetupMessage.ARENA_RESERVED_FOR_MATCH);
    }

    public void clearConfiguration(CommandSender sender) {
        Optional<String> rejection = findResetRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(sender, rejection.get());
            return;
        }
        activeArena.clear();
        arenaBaselineService.discardSnapshot();
        arenaConfigurationSaver.run();
        TournamentMessages.sendSuccess(sender, Messages.text(ArenaSetupMessage.CONFIGURATION_REMOVED));
    }

    public Optional<String> findCleaningRejection() {
        Optional<ArenaShape> volume = getShape();
        if (volume.isEmpty()) {
            return Optional.of(Messages.text(ArenaSetupMessage.CLEANING_WITHOUT_FIGHT_AREA, calculateAction()));
        }
        if (matchService.hasActiveMatch()) {
            return Optional.of(Messages.text(ArenaSetupMessage.CLEANING_DURING_MATCH));
        }
        if (arenaReservedForMatch.getAsBoolean()) {
            return Optional.of(describeArenaReservedForMatch());
        }
        if (arenaCleaningService.isCleaning()) {
            return Optional.of(Messages.text(ArenaSetupMessage.CLEANING_ALREADY_RUNNING));
        }
        if (arenaBaselineService.isCapturing()) {
            return Optional.of(Messages.text(ArenaSetupMessage.CLEANING_DURING_BASELINE_CAPTURE));
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
        List<String> effect = new ArrayList<>(Messages.lines(baseline.isPresent()
                ? ArenaSetupMessage.CLEANING_EFFECT_WITH_BASELINE
                : ArenaSetupMessage.CLEANING_EFFECT_WITHOUT_BASELINE));
        int depthBelowFloor = baseline.map(ArenaBaselineSnapshot::getDepthBelowFloor).orElse(0);
        findCleanedVolume().ifPresent(volume -> {
            Placeholder floorY = Placeholder.of("floorY", volume.getFloorY());
            Placeholder topY = Placeholder.of("topY", volume.getWallTopY());
            if (depthBelowFloor == 0) {
                effect.add(Messages.text(ArenaSetupMessage.CLEANING_COVERS_FIGHT_AREA, floorY, topY));
                return;
            }
            effect.addAll(Arrays.asList(Messages.plural(ArenaSetupMessage.CLEANING_COVERS_LAYERS, depthBelowFloor,
                    floorY, topY).split(TournamentMessages.LINE_BREAK)));
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
            TournamentMessages.sendError(sender, Messages.text(ArenaSetupMessage.CLEANING_NOT_STARTED));
            return false;
        }
        if (plan.restoresABaseline()) {
            TournamentMessages.sendInfo(sender, Messages.text(ArenaSetupMessage.CLEANING_RESTORES_BASELINE,
                    Placeholder.of("baseline", plan.findBaseline().orElseThrow().describe())));
        } else {
            TournamentMessages.sendWarning(sender, Messages.text(ArenaSetupMessage.CLEANING_WITHOUT_BASELINE,
                    calculateAction()));
        }
        TournamentMessages.sendInfo(sender, Messages.plural(ArenaSetupMessage.CLEANING_STARTED, blockCount,
                Placeholder.of("floorY", shape.getFloorY()), Placeholder.of("topY", shape.getWallTopY()),
                Placeholder.of("batchSize", arenaCleaningService.getBlocksPerTick()),
                Placeholder.of("animation", arenaCleaningService.describeAnimation())));
        return true;
    }

    public Optional<String> startAutomaticCleaning(Consumer<String> completionHandler) {
        Optional<String> rejection = findCleaningRejection();
        if (rejection.isPresent()) {
            return rejection;
        }
        if (!hasBaseline()) {
            return Optional.of(Messages.text(ArenaSetupMessage.AUTOMATIC_CLEANING_WITHOUT_BASELINE,
                    calculateAction()));
        }
        ArenaCleaningPlan plan = ArenaCleaningPlan.of(getShape().orElseThrow(),
                arenaBaselineService.findStoredSnapshot());
        if (!arenaCleaningService.startCleaning(plan,
                report -> completionHandler.accept(report.describeOutcome()))) {
            return Optional.of(Messages.text(ArenaSetupMessage.AUTOMATIC_CLEANING_NOT_STARTED));
        }
        return Optional.empty();
    }

    private static Placeholder calculateAction() {
        return Placeholder.of("action", Arena.describeArenaMenuTile(ArenaMenuMessage.CALCULATE_TITLE));
    }

    private static Placeholder markHeightAction() {
        return Placeholder.of("action", Arena.describeArenaMenuTile(ArenaMenuMessage.HEIGHT_MARKER_TITLE));
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
                Messages.text(arena.isFullyConfigured()
                        ? ArenaSetupMessage.DESCRIPTION_READY : ArenaSetupMessage.DESCRIPTION_INCOMPLETE),
                arena.getShape()
                        .map(shape -> Messages.text(ArenaSetupMessage.DESCRIPTION_FIGHT_AREA,
                                Placeholder.of("fightArea", shape.describe())))
                        .orElseGet(() -> Messages.text(ArenaSetupMessage.DESCRIPTION_NO_FIGHT_AREA)),
                describeSpawn(ArenaSpawnType.FIRST_TEAM, arena.getConfiguredFirstTeamSpawn()),
                describeSpawn(ArenaSpawnType.SECOND_TEAM, arena.getConfiguredSecondTeamSpawn()),
                describeSpawn(ArenaSpawnType.TRIBUNE, arena.getTribuneSpawn()),
                activeArena.findEffectArea()
                        .map(effectArea -> Messages.text(ArenaSetupMessage.DESCRIPTION_EFFECT_AREA,
                                Placeholder.of("effectArea", effectArea.describe())))
                        .orElseGet(() -> Messages.text(ArenaSetupMessage.DESCRIPTION_NO_EFFECT_AREA)),
                matchService.getActiveMatch()
                        .map(match -> Messages.text(ArenaSetupMessage.DESCRIPTION_RUNNING_MATCH,
                                Placeholder.of("match", match.describe())))
                        .orElseGet(() -> Messages.text(ArenaSetupMessage.DESCRIPTION_NO_RUNNING_MATCH)));
    }

    private static String describeSpawn(ArenaSpawnType spawnType, Optional<PositionSnapshot> spawn) {
        Placeholder spawnName = Placeholder.of("spawn", spawnType.getDisplayName());
        return spawn.map(position -> Messages.text(ArenaSetupMessage.DESCRIPTION_SPAWN, spawnName,
                        Placeholder.of("position", position.describe())))
                .orElseGet(() -> Messages.text(ArenaSetupMessage.DESCRIPTION_SPAWN_NOT_CONFIGURED, spawnName));
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
        TournamentMessages.sendBlock(sender, TournamentMessages.warning(Messages.text(ArenaSetupMessage.STILL_MISSING)),
                missingParts);
    }
}
