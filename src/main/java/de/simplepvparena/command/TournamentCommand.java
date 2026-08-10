package de.simplepvparena.command;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaShapeCalculation;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderItemRequest;
import de.simplepvparena.border.BorderItemRequestResult;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.FinalMatchGate;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TournamentCommand implements TabExecutor {

    public static final String ADMIN_PERMISSION = "tournament.admin";
    public static final String SPECTATE_PERMISSION = "tournament.spectate";

    private static final String CLEAN_CONFIRMATION_ARGUMENT = "confirm";
    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "team", "arena", "border", "match", "final", "start", "forcestart", "pause", "cancel", "status",
            "bracket", "spectate", "help");
    private static final List<String> TEAM_SUBCOMMANDS = List.of("add", "remove", "list");
    private static final List<String> ARENA_SUBCOMMANDS = List.of("calculate", "setspawn", "info", "clean", "remove");
    private static final List<String> BORDER_SUBCOMMANDS = List.of("give");
    private static final List<String> MATCH_SUBCOMMANDS = List.of("forceresult", "list");
    private static final List<String> FINAL_SUBCOMMANDS = List.of("start");
    private static final List<String> SPAWN_TYPES = List.of("team1", "team2", "tribune");

    private final Server server;
    private final TournamentManager tournamentManager;
    private final TeamRegistry teamRegistry;
    private final ArenaRegistry arenaRegistry;
    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final SpectatorBorderService spectatorBorderService;
    private final ArenaCleaningService arenaCleaningService;
    private final int maximumArenaCellCount;
    private final Runnable arenaConfigurationSaver;
    private final Runnable teamConfigurationSaver;

    public TournamentCommand(Server server,
                             TournamentManager tournamentManager,
                             TeamRegistry teamRegistry,
                             ArenaRegistry arenaRegistry,
                             MatchService matchService,
                             SpectatorService spectatorService,
                             SpectatorBorderService spectatorBorderService,
                             ArenaCleaningService arenaCleaningService,
                             int maximumArenaCellCount,
                             Runnable arenaConfigurationSaver,
                             Runnable teamConfigurationSaver) {
        this.server = server;
        this.tournamentManager = tournamentManager;
        this.teamRegistry = teamRegistry;
        this.arenaRegistry = arenaRegistry;
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.spectatorBorderService = spectatorBorderService;
        this.arenaCleaningService = arenaCleaningService;
        this.maximumArenaCellCount = maximumArenaCellCount;
        this.arenaConfigurationSaver = arenaConfigurationSaver;
        this.teamConfigurationSaver = teamConfigurationSaver;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] arguments) {
        if (arguments.length == 0) {
            sendHelp(sender);
            return true;
        }
        String subcommand = arguments[0].toLowerCase(Locale.ROOT);
        String[] remainingArguments = Arrays.copyOfRange(arguments, 1, arguments.length);
        switch (subcommand) {
            case "team" -> handleTeamCommand(sender, remainingArguments);
            case "arena" -> handleArenaCommand(sender, remainingArguments);
            case "border" -> handleBorderCommand(sender, remainingArguments);
            case "match" -> handleMatchCommand(sender, remainingArguments);
            case "final" -> handleFinalCommand(sender, remainingArguments);
            case "start" -> handleStart(sender, false);
            case "forcestart" -> handleStart(sender, true);
            case "pause" -> handleAdminOperation(sender, tournamentManager::togglePause);
            case "cancel" -> handleAdminOperation(sender, tournamentManager::cancelTournament);
            case "status" -> sender.sendMessage(tournamentManager.describeStatus());
            case "bracket" -> tournamentManager.describeBracket().forEach(sender::sendMessage);
            case "spectate" -> handleSpectate(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleTeamCommand(CommandSender sender, String[] arguments) {
        if (arguments.length == 0) {
            sender.sendMessage("Usage: /tournament team <add|remove|list>");
            return;
        }
        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleTeamAdd(sender, arguments);
            case "remove" -> handleTeamRemove(sender, arguments);
            case "list" -> handleTeamList(sender);
            default -> sender.sendMessage("Usage: /tournament team <add|remove|list>");
        }
    }

    private void handleTeamAdd(CommandSender sender, String[] arguments) {
        if (!requireAdminPermission(sender)) {
            return;
        }
        if (arguments.length != 4) {
            sender.sendMessage("Usage: /tournament team add <teamName> <firstPlayer> <secondPlayer>");
            return;
        }
        Optional<TeamMember> firstMember = resolveTeamMember(arguments[2]);
        Optional<TeamMember> secondMember = resolveTeamMember(arguments[3]);
        if (firstMember.isEmpty()) {
            sender.sendMessage("Unknown player " + arguments[2] + ", the player has to be online or known to the server");
            return;
        }
        if (secondMember.isEmpty()) {
            sender.sendMessage("Unknown player " + arguments[3] + ", the player has to be online or known to the server");
            return;
        }
        try {
            Team team = teamRegistry.registerTeam(arguments[1], firstMember.get(), secondMember.get());
            teamConfigurationSaver.run();
            sender.sendMessage("Team " + team.getTeamName() + " registered with " + team.getMemberNamesJoined());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            sender.sendMessage(failure.getMessage());
        }
    }

    private void handleTeamRemove(CommandSender sender, String[] arguments) {
        if (!requireAdminPermission(sender)) {
            return;
        }
        if (arguments.length != 2) {
            sender.sendMessage("Usage: /tournament team remove <teamName>");
            return;
        }
        if (teamRegistry.removeTeam(arguments[1])) {
            teamConfigurationSaver.run();
            sender.sendMessage("Team " + arguments[1] + " removed");
            return;
        }
        sender.sendMessage("Unknown team " + arguments[1]);
    }

    private void handleTeamList(CommandSender sender) {
        if (teamRegistry.getTeamCount() == 0) {
            sender.sendMessage("No teams registered");
            return;
        }
        sender.sendMessage("Registered teams (" + teamRegistry.getTeamCount() + "):");
        teamRegistry.getTeams().forEach(team ->
                sender.sendMessage("  " + team.getTeamName() + ": " + team.getMemberNamesJoined()));
    }

    private void handleArenaCommand(CommandSender sender, String[] arguments) {
        if (arguments.length == 0) {
            sender.sendMessage("Usage: /tournament arena <calculate|setspawn|info|clean|remove>");
            return;
        }
        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "calculate" -> handleArenaCalculate(sender);
            case "setspawn" -> handleArenaSetSpawn(sender, arguments);
            case "info" -> handleArenaInfo(sender);
            case "clean" -> handleArenaClean(sender, arguments);
            case "remove" -> handleArenaRemove(sender);
            default -> sender.sendMessage("Usage: /tournament arena <calculate|setspawn|info|clean|remove>");
        }
    }

    private void handleArenaCalculate(CommandSender sender) {
        Optional<Player> player = requireAdminPlayer(sender);
        if (player.isEmpty()) {
            return;
        }
        if (matchService.hasActiveMatch()) {
            sender.sendMessage("A match is running in the arena, it can only be calculated between matches");
            return;
        }
        Location seedLocation = player.get().getLocation();
        World world = seedLocation.getWorld();
        ArenaShapeCalculation calculation = ArenaShapeFloodFill.calculate(world.getName(),
                seedLocation.getBlockX(), seedLocation.getBlockY(), seedLocation.getBlockZ(),
                maximumArenaCellCount, spectatorBorderService.createColumnReader(world));
        if (!calculation.isSuccessful()) {
            sender.sendMessage(calculation.getRejectionMessage().orElseThrow());
            return;
        }
        ArenaShape shape = calculation.getShape().orElseThrow();
        Arena arena = arenaRegistry.getArena();
        arena.setShape(shape);
        arenaConfigurationSaver.run();
        sender.sendMessage("The fight area was calculated from the border blocks: " + shape.describe());
        arena.findSpawnPlacementWarnings().forEach(sender::sendMessage);
        reportMissingConfiguration(sender, arena);
    }

    private void handleArenaSetSpawn(CommandSender sender, String[] arguments) {
        Optional<Player> player = requireAdminPlayer(sender);
        if (player.isEmpty()) {
            return;
        }
        if (arguments.length != 2) {
            sender.sendMessage("Usage: /tournament arena setspawn <team1|team2|tribune>");
            return;
        }
        Arena arena = arenaRegistry.getArena();
        PositionSnapshot position = PositionSnapshot.fromLocation(player.get().getLocation());
        switch (arguments[1].toLowerCase(Locale.ROOT)) {
            case "team1" -> arena.setFirstTeamSpawn(position);
            case "team2" -> arena.setSecondTeamSpawn(position);
            case "tribune" -> arena.setTribuneSpawn(position);
            default -> {
                sender.sendMessage("Unknown spawn type " + arguments[1] + ", expected team1, team2 or tribune");
                return;
            }
        }
        arenaConfigurationSaver.run();
        sender.sendMessage("Set spawn " + arguments[1] + " of the arena to " + position.describe());
        arena.findSpawnPlacementWarnings().forEach(sender::sendMessage);
        reportMissingConfiguration(sender, arena);
    }

    private void handleArenaInfo(CommandSender sender) {
        Arena arena = arenaRegistry.getArena();
        sender.sendMessage("Arena " + (arena.isFullyConfigured() ? "ready" : "incomplete"));
        sender.sendMessage("  fight area: "
                + arena.getShape().map(ArenaShape::describe).orElse("not calculated"));
        sender.sendMessage("  first team spawn: "
                + arena.getFirstTeamSpawn().map(PositionSnapshot::describe).orElse("not configured"));
        sender.sendMessage("  second team spawn: "
                + arena.getSecondTeamSpawn().map(PositionSnapshot::describe).orElse("not configured"));
        sender.sendMessage("  tribune spawn: "
                + arena.getTribuneSpawn().map(PositionSnapshot::describe).orElse("not configured"));
        sender.sendMessage("  running match: "
                + matchService.getActiveMatch().map(Match::describe).orElse("none"));
    }

    private void handleArenaClean(CommandSender sender, String[] arguments) {
        if (!requireAdminPermission(sender)) {
            return;
        }
        Optional<ArenaShape> shape = arenaRegistry.getArena().getShape();
        if (shape.isEmpty()) {
            sender.sendMessage("The fight area is not calculated, there is nothing to clean. Run "
                    + Arena.CALCULATE_COMMAND + " inside the border wall first");
            return;
        }
        if (matchService.hasActiveMatch()) {
            sender.sendMessage("A match is running in the arena, the arena can only be cleaned between matches");
            return;
        }
        if (arenaCleaningService.isCleaning()) {
            sender.sendMessage("The arena is already being cleaned");
            return;
        }
        long blockCount = ArenaCleaningService.countBlocks(shape.get());
        if (arguments.length != 2 || !arguments[1].equalsIgnoreCase(CLEAN_CONFIRMATION_ARGUMENT)) {
            sender.sendMessage("Cleaning sets every block of the calculated fight area to air, "
                    + blockCount + " blocks are affected and the change cannot be undone");
            sender.sendMessage("  " + shape.get().describe());
            sender.sendMessage("Run /tournament arena clean " + CLEAN_CONFIRMATION_ARGUMENT + " to start it");
            return;
        }
        boolean cleaningStarted = arenaCleaningService.startCleaning(shape.get(),
                report -> sender.sendMessage("The arena is clean, " + report.describe()));
        if (!cleaningStarted) {
            sender.sendMessage("The arena cleaning could not be started");
            return;
        }
        sender.sendMessage("Cleaning the arena, " + blockCount + " blocks are processed in batches of "
                + ArenaCleaningService.DEFAULT_BLOCKS_PER_TICK + " per tick");
    }

    private void handleArenaRemove(CommandSender sender) {
        if (!requireAdminPermission(sender)) {
            return;
        }
        arenaRegistry.clearArenaConfiguration();
        arenaConfigurationSaver.run();
        sender.sendMessage("The arena configuration was removed, its blocks and border blocks stay untouched");
    }

    private void reportMissingConfiguration(CommandSender sender, Arena arena) {
        List<String> missingParts = arena.getMissingConfiguration();
        if (!missingParts.isEmpty()) {
            sender.sendMessage("The arena still misses: " + String.join(", ", missingParts));
        }
    }

    private void handleBorderCommand(CommandSender sender, String[] arguments) {
        if (arguments.length == 0 || !arguments[0].equalsIgnoreCase("give")) {
            sender.sendMessage("Usage: /tournament border give <material> <amount>");
            return;
        }
        handleBorderGive(sender, arguments);
    }

    private void handleBorderGive(CommandSender sender, String[] arguments) {
        Optional<Player> player = requireAdminPlayer(sender);
        if (player.isEmpty()) {
            return;
        }
        if (arguments.length != 3) {
            sender.sendMessage("Usage: /tournament border give <material> <amount>");
            return;
        }
        BorderItemRequestResult requestResult = BorderItemRequest.parse(arguments[1], arguments[2],
                TournamentCommand::isPlaceableBlock);
        if (!requestResult.isAccepted()) {
            sender.sendMessage(requestResult.getRejectionMessage().orElseThrow());
            return;
        }
        BorderItemRequest request = requestResult.getRequest().orElseThrow();
        ItemStack borderItem = spectatorBorderService.createBorderItem(request);
        Map<Integer, ItemStack> itemsThatDidNotFit = player.get().getInventory().addItem(borderItem);
        if (!itemsThatDidNotFit.isEmpty()) {
            sender.sendMessage("Your inventory is full, not every border block could be handed out");
        }
        sender.sendMessage("You received " + request.amount() + " " + request.material().name()
                + " border blocks, every block you place with them stops everybody who is not fighting");
    }

    private static boolean isPlaceableBlock(Material material) {
        return material.isBlock() && material.isItem() && !material.isAir();
    }

    private void handleMatchCommand(CommandSender sender, String[] arguments) {
        if (arguments.length == 0) {
            sender.sendMessage("Usage: /tournament match <forceresult|list>");
            return;
        }
        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "forceresult" -> handleMatchForceResult(sender, arguments);
            case "list" -> handleMatchList(sender);
            default -> sender.sendMessage("Usage: /tournament match <forceresult|list>");
        }
    }

    private void handleMatchForceResult(CommandSender sender, String[] arguments) {
        if (!requireAdminPermission(sender)) {
            return;
        }
        if (arguments.length != 3) {
            sender.sendMessage("Usage: /tournament match forceresult <matchId> <teamName>");
            return;
        }
        TournamentOperationResult result = tournamentManager.forceMatchResult(arguments[1], arguments[2]);
        sender.sendMessage(result.message());
    }

    private void handleMatchList(CommandSender sender) {
        Optional<Match> activeMatch = matchService.getActiveMatch();
        if (activeMatch.isEmpty()) {
            sender.sendMessage("No match is running");
            return;
        }
        sender.sendMessage("Running match: " + activeMatch.get().describe());
    }

    private void handleFinalCommand(CommandSender sender, String[] arguments) {
        if (arguments.length != 1 || !arguments[0].equalsIgnoreCase("start")) {
            sender.sendMessage("Usage: " + FinalMatchGate.FINAL_START_COMMAND);
            return;
        }
        handleAdminOperation(sender, tournamentManager::startFinalMatch);
    }

    private void handleStart(CommandSender sender, boolean allowUnevenTeamCount) {
        handleAdminOperation(sender, () -> tournamentManager.startTournament(allowUnevenTeamCount));
    }

    private void handleAdminOperation(CommandSender sender, java.util.function.Supplier<TournamentOperationResult> operation) {
        if (!requireAdminPermission(sender)) {
            return;
        }
        sender.sendMessage(operation.get().message());
    }

    private void handleSpectate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return;
        }
        if (!player.hasPermission(SPECTATE_PERMISSION)) {
            player.sendMessage("You are not allowed to use this command");
            return;
        }
        if (matchService.isActiveFighter(player.getUniqueId())) {
            player.sendMessage("You cannot leave the arena while you are fighting");
            return;
        }
        if (!spectatorService.sendToTribune(player)) {
            player.sendMessage("The arena has no usable tribune position");
            return;
        }
        player.sendMessage("You are now watching the arena");
    }

    private Optional<TeamMember> resolveTeamMember(String playerName) {
        Player onlinePlayer = server.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return Optional.of(new TeamMember(onlinePlayer.getUniqueId(), onlinePlayer.getName()));
        }
        OfflinePlayer cachedPlayer = server.getOfflinePlayerIfCached(playerName);
        if (cachedPlayer == null || cachedPlayer.getName() == null) {
            return Optional.empty();
        }
        return Optional.of(new TeamMember(cachedPlayer.getUniqueId(), cachedPlayer.getName()));
    }

    private boolean requireAdminPermission(CommandSender sender) {
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        sender.sendMessage("You are not allowed to use this command");
        return false;
    }

    private Optional<Player> requireAdminPlayer(CommandSender sender) {
        if (!requireAdminPermission(sender)) {
            return Optional.empty();
        }
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage("Only players can use this command");
        return Optional.empty();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("/tournament team add <teamName> <firstPlayer> <secondPlayer>");
        sender.sendMessage("/tournament team remove <teamName>");
        sender.sendMessage("/tournament team list");
        sender.sendMessage(Arena.CALCULATE_COMMAND + " (while standing inside the closed border wall)");
        sender.sendMessage("/tournament arena setspawn <team1|team2|tribune>");
        sender.sendMessage("/tournament arena info");
        sender.sendMessage("/tournament arena clean [" + CLEAN_CONFIRMATION_ARGUMENT + "]");
        sender.sendMessage("/tournament arena remove");
        sender.sendMessage("/tournament border give <material> <amount>");
        sender.sendMessage("/tournament match forceresult <matchId> <teamName>");
        sender.sendMessage("/tournament match list");
        sender.sendMessage(FinalMatchGate.FINAL_START_COMMAND);
        sender.sendMessage("/tournament start");
        sender.sendMessage("/tournament forcestart");
        sender.sendMessage("/tournament pause");
        sender.sendMessage("/tournament cancel");
        sender.sendMessage("/tournament status");
        sender.sendMessage("/tournament bracket");
        sender.sendMessage("/tournament spectate");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] arguments) {
        if (arguments.length == 1) {
            return filterByPrefix(ROOT_SUBCOMMANDS, arguments[0]);
        }
        String subcommand = arguments[0].toLowerCase(Locale.ROOT);
        if (arguments.length == 2) {
            return switch (subcommand) {
                case "team" -> filterByPrefix(TEAM_SUBCOMMANDS, arguments[1]);
                case "arena" -> filterByPrefix(ARENA_SUBCOMMANDS, arguments[1]);
                case "border" -> filterByPrefix(BORDER_SUBCOMMANDS, arguments[1]);
                case "match" -> filterByPrefix(MATCH_SUBCOMMANDS, arguments[1]);
                case "final" -> filterByPrefix(FINAL_SUBCOMMANDS, arguments[1]);
                default -> List.of();
            };
        }
        if (arguments.length == 3) {
            return switch (subcommand) {
                case "team" -> arguments[1].equalsIgnoreCase("remove")
                        ? filterByPrefix(teamRegistry.getTeamNames(), arguments[2])
                        : List.of();
                case "arena" -> filterByPrefix(collectArenaArguments(arguments[1]), arguments[2]);
                case "border" -> filterByPrefix(collectPlaceableBlockNames(), arguments[2]);
                case "match" -> filterByPrefix(collectActiveMatchIds(), arguments[2]);
                default -> List.of();
            };
        }
        if (arguments.length == 4) {
            return switch (subcommand) {
                case "match" -> filterByPrefix(teamRegistry.getTeamNames(), arguments[3]);
                default -> List.of();
            };
        }
        return List.of();
    }

    private List<String> collectArenaArguments(String arenaSubcommand) {
        return switch (arenaSubcommand.toLowerCase(Locale.ROOT)) {
            case "setspawn" -> SPAWN_TYPES;
            case "clean" -> List.of(CLEAN_CONFIRMATION_ARGUMENT);
            default -> List.of();
        };
    }

    private List<String> collectPlaceableBlockNames() {
        return Arrays.stream(Material.values())
                .filter(TournamentCommand::isPlaceableBlock)
                .map(Material::name)
                .toList();
    }

    private List<String> collectActiveMatchIds() {
        return matchService.getActiveMatch().map(match -> List.of(match.getMatchId())).orElse(List.of());
    }

    private List<String> filterByPrefix(List<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                matches.add(candidate);
            }
        }
        return matches;
    }
}
