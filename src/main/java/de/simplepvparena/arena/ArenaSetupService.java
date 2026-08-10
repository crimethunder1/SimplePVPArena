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
    private final ArenaCleaningService arenaCleaningService;
    private final int maximumArenaCellCount;
    private final Runnable arenaConfigurationSaver;

    public ArenaSetupService(ArenaRegistry arenaRegistry,
                             MatchService matchService,
                             SpectatorBorderService spectatorBorderService,
                             BorderWallPreview borderWallPreview,
                             ArenaCleaningService arenaCleaningService,
                             int maximumArenaCellCount,
                             Runnable arenaConfigurationSaver) {
        this.arenaRegistry = arenaRegistry;
        this.matchService = matchService;
        this.spectatorBorderService = spectatorBorderService;
        this.borderWallPreview = borderWallPreview;
        this.arenaCleaningService = arenaCleaningService;
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
        if (matchService.hasActiveMatch()) {
            admin.sendMessage("A match is running in the arena, it can only be calculated between matches");
            return;
        }
        Location seedLocation = admin.getLocation();
        World world = seedLocation.getWorld();
        BorderColumnReader borderColumns = spectatorBorderService.createColumnReader(world);
        ArenaShapeCalculation calculation = ArenaShapeFloodFill.calculate(world.getName(),
                seedLocation.getBlockX(), seedLocation.getBlockY(), seedLocation.getBlockZ(),
                maximumArenaCellCount, borderColumns);
        if (!calculation.isSuccessful()) {
            admin.sendMessage(calculation.getRejectionMessage().orElseThrow());
            return;
        }
        ArenaShape shape = calculation.getShape().orElseThrow();
        Arena arena = arenaRegistry.getArena();
        arena.setShape(shape);
        arenaConfigurationSaver.run();
        admin.sendMessage("The fight area was calculated from the border blocks: " + shape.describe());
        previewRecognizedWall(admin, world, shape, borderColumns);
        arena.findSpawnPlacementWarnings().forEach(admin::sendMessage);
        reportMissingConfiguration(admin, arena);
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
        admin.sendMessage("Set spawn " + spawnType.getArgumentName() + " of the arena to " + position.describe());
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
            return Optional.of("The fight area is not calculated, there is nothing to clean. Run "
                    + Arena.CALCULATE_COMMAND + " inside the border wall first");
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
