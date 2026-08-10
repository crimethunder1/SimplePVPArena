package de.simplepvparena.command;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaRegionType;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.RegionCorner;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TournamentCommand implements TabExecutor {

    public static final String ADMIN_PERMISSION = "tournament.admin";
    public static final String SPECTATE_PERMISSION = "tournament.spectate";

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "team", "arena", "match", "start", "forcestart", "pause", "cancel", "status", "bracket", "spectate", "help");
    private static final List<String> TEAM_SUBCOMMANDS = List.of("add", "remove", "list");
    private static final List<String> ARENA_SUBCOMMANDS = List.of("setpos", "setspawn", "list", "info", "remove");
    private static final List<String> MATCH_SUBCOMMANDS = List.of("forceresult", "list");
    private static final List<String> SPAWN_TYPES = List.of("team1", "team2", "tribune");

    private final Server server;
    private final TournamentManager tournamentManager;
    private final TeamRegistry teamRegistry;
    private final ArenaRegistry arenaRegistry;
    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final Runnable arenaConfigurationSaver;
    private final Runnable teamConfigurationSaver;

    public TournamentCommand(Server server,
                             TournamentManager tournamentManager,
                             TeamRegistry teamRegistry,
                             ArenaRegistry arenaRegistry,
                             MatchService matchService,
                             SpectatorService spectatorService,
                             Runnable arenaConfigurationSaver,
                             Runnable teamConfigurationSaver) {
        this.server = server;
        this.tournamentManager = tournamentManager;
        this.teamRegistry = teamRegistry;
        this.arenaRegistry = arenaRegistry;
        this.matchService = matchService;
        this.spectatorService = spectatorService;
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
            case "match" -> handleMatchCommand(sender, remainingArguments);
            case "start" -> handleStart(sender, false);
            case "forcestart" -> handleStart(sender, true);
            case "pause" -> handleAdminOperation(sender, tournamentManager::togglePause);
            case "cancel" -> handleAdminOperation(sender, tournamentManager::cancelTournament);
            case "status" -> sender.sendMessage(tournamentManager.describeStatus());
            case "bracket" -> tournamentManager.describeBracket().forEach(sender::sendMessage);
            case "spectate" -> handleSpectate(sender, remainingArguments);
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
            sender.sendMessage("Usage: /tournament arena <setpos|setspawn|list|info|remove>");
            return;
        }
        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "setpos" -> handleArenaSetPosition(sender, arguments);
            case "setspawn" -> handleArenaSetSpawn(sender, arguments);
            case "list" -> handleArenaList(sender);
            case "info" -> handleArenaInfo(sender, arguments);
            case "remove" -> handleArenaRemove(sender, arguments);
            default -> sender.sendMessage("Usage: /tournament arena <setpos|setspawn|list|info|remove>");
        }
    }

    private void handleArenaSetPosition(CommandSender sender, String[] arguments) {
        Optional<Player> player = requireAdminPlayer(sender);
        if (player.isEmpty()) {
            return;
        }
        if (arguments.length != 4) {
            sender.sendMessage("Usage: /tournament arena setpos <arenaName> <fight|spectatorbox|tribune> <corner1|corner2>");
            return;
        }
        Optional<ArenaRegionType> regionType = ArenaRegionType.fromCommandArgument(arguments[2]);
        if (regionType.isEmpty()) {
            sender.sendMessage("Unknown region type " + arguments[2] + ", expected fight, spectatorbox or tribune");
            return;
        }
        Optional<RegionCorner> corner = RegionCorner.fromCommandArgument(arguments[3]);
        if (corner.isEmpty()) {
            sender.sendMessage("Unknown corner " + arguments[3] + ", expected corner1 or corner2");
            return;
        }
        Arena arena = arenaRegistry.getOrCreateArena(arguments[1]);
        PositionSnapshot position = PositionSnapshot.fromLocation(player.get().getLocation());
        arena.setCorner(regionType.get(), corner.get(), position);
        arenaConfigurationSaver.run();
        sender.sendMessage("Set " + regionType.get().getCommandArgument() + " " + corner.get().getCommandArgument()
                + " of arena " + arena.getArenaName() + " to " + position.describe());
        if (!arena.isFullyConfigured()) {
            sender.sendMessage("Arena " + arena.getArenaName() + " still misses: "
                    + arena.getMissingRegionTypes().stream().map(ArenaRegionType::getCommandArgument).toList());
        }
    }

    private void handleArenaSetSpawn(CommandSender sender, String[] arguments) {
        Optional<Player> player = requireAdminPlayer(sender);
        if (player.isEmpty()) {
            return;
        }
        if (arguments.length != 3) {
            sender.sendMessage("Usage: /tournament arena setspawn <arenaName> <team1|team2|tribune>");
            return;
        }
        Arena arena = arenaRegistry.getOrCreateArena(arguments[1]);
        PositionSnapshot position = PositionSnapshot.fromLocation(player.get().getLocation());
        switch (arguments[2].toLowerCase(Locale.ROOT)) {
            case "team1" -> arena.setFirstTeamSpawn(position);
            case "team2" -> arena.setSecondTeamSpawn(position);
            case "tribune" -> arena.setTribuneSpawn(position);
            default -> {
                sender.sendMessage("Unknown spawn type " + arguments[2] + ", expected team1, team2 or tribune");
                return;
            }
        }
        arenaConfigurationSaver.run();
        sender.sendMessage("Set spawn " + arguments[2] + " of arena " + arena.getArenaName() + " to " + position.describe());
    }

    private void handleArenaList(CommandSender sender) {
        if (arenaRegistry.getArenas().isEmpty()) {
            sender.sendMessage("No arenas configured");
            return;
        }
        sender.sendMessage("Configured arenas (" + arenaRegistry.getArenas().size() + "):");
        arenaRegistry.getArenas().forEach(arena -> sender.sendMessage("  " + arena.getArenaName()
                + (arena.isFullyConfigured() ? " ready" : " incomplete")
                + (arena.isOccupied() ? " occupied by match " + arena.getOccupyingMatchId().orElse("") : " free")));
    }

    private void handleArenaInfo(CommandSender sender, String[] arguments) {
        if (arguments.length != 2) {
            sender.sendMessage("Usage: /tournament arena info <arenaName>");
            return;
        }
        Optional<Arena> arena = arenaRegistry.findArena(arguments[1]);
        if (arena.isEmpty()) {
            sender.sendMessage("Unknown arena " + arguments[1]);
            return;
        }
        sender.sendMessage("Arena " + arena.get().getArenaName());
        for (ArenaRegionType regionType : ArenaRegionType.values()) {
            sender.sendMessage("  " + regionType.getCommandArgument() + ": "
                    + arena.get().getRegion(regionType).map(region -> region.describe()).orElse("not configured"));
        }
        sender.sendMessage("  first team spawn: "
                + arena.get().getFirstTeamSpawn().map(PositionSnapshot::describe).orElse("not configured"));
        sender.sendMessage("  second team spawn: "
                + arena.get().getSecondTeamSpawn().map(PositionSnapshot::describe).orElse("not configured"));
        sender.sendMessage("  tribune spawn: "
                + arena.get().getTribuneSpawn().map(PositionSnapshot::describe).orElse("not configured"));
    }

    private void handleArenaRemove(CommandSender sender, String[] arguments) {
        if (!requireAdminPermission(sender)) {
            return;
        }
        if (arguments.length != 2) {
            sender.sendMessage("Usage: /tournament arena remove <arenaName>");
            return;
        }
        if (arenaRegistry.removeArena(arguments[1])) {
            arenaConfigurationSaver.run();
            sender.sendMessage("Arena " + arguments[1] + " removed");
            return;
        }
        sender.sendMessage("Unknown arena " + arguments[1]);
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
        List<Match> activeMatches = List.copyOf(matchService.getActiveMatches());
        if (activeMatches.isEmpty()) {
            sender.sendMessage("No matches are running");
            return;
        }
        sender.sendMessage("Running matches (" + activeMatches.size() + "):");
        activeMatches.forEach(match -> sender.sendMessage("  " + match.describe()));
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

    private void handleSpectate(CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return;
        }
        if (!player.hasPermission(SPECTATE_PERMISSION)) {
            player.sendMessage("You are not allowed to use this command");
            return;
        }
        if (arguments.length != 1) {
            player.sendMessage("Usage: /tournament spectate <arenaName>");
            return;
        }
        if (matchService.isActiveFighter(player.getUniqueId())) {
            player.sendMessage("You cannot leave the arena while you are fighting");
            return;
        }
        Optional<Arena> arena = arenaRegistry.findArena(arguments[0]);
        if (arena.isEmpty()) {
            player.sendMessage("Unknown arena " + arguments[0]);
            return;
        }
        if (!spectatorService.sendToTribune(player, arena.get())) {
            player.sendMessage("Arena " + arena.get().getArenaName() + " has no usable tribune position");
            return;
        }
        player.sendMessage("You are now watching arena " + arena.get().getArenaName());
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
        sender.sendMessage("/tournament arena setpos <arenaName> <fight|spectatorbox|tribune> <corner1|corner2>");
        sender.sendMessage("/tournament arena setspawn <arenaName> <team1|team2|tribune>");
        sender.sendMessage("/tournament arena list");
        sender.sendMessage("/tournament arena info <arenaName>");
        sender.sendMessage("/tournament arena remove <arenaName>");
        sender.sendMessage("/tournament match forceresult <matchId> <teamName>");
        sender.sendMessage("/tournament match list");
        sender.sendMessage("/tournament start");
        sender.sendMessage("/tournament forcestart");
        sender.sendMessage("/tournament pause");
        sender.sendMessage("/tournament cancel");
        sender.sendMessage("/tournament status");
        sender.sendMessage("/tournament bracket");
        sender.sendMessage("/tournament spectate <arenaName>");
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
                case "match" -> filterByPrefix(MATCH_SUBCOMMANDS, arguments[1]);
                case "spectate" -> filterByPrefix(arenaRegistry.getArenaNames(), arguments[1]);
                default -> List.of();
            };
        }
        if (arguments.length == 3) {
            return switch (subcommand) {
                case "team" -> arguments[1].equalsIgnoreCase("remove")
                        ? filterByPrefix(teamRegistry.getTeamNames(), arguments[2])
                        : List.of();
                case "arena" -> filterByPrefix(arenaRegistry.getArenaNames(), arguments[2]);
                case "match" -> filterByPrefix(collectActiveMatchIds(), arguments[2]);
                default -> List.of();
            };
        }
        if (arguments.length == 4) {
            return switch (subcommand) {
                case "arena" -> arguments[1].equalsIgnoreCase("setpos")
                        ? filterByPrefix(collectRegionTypeArguments(), arguments[3])
                        : filterByPrefix(SPAWN_TYPES, arguments[3]);
                case "match" -> filterByPrefix(teamRegistry.getTeamNames(), arguments[3]);
                default -> List.of();
            };
        }
        if (arguments.length == 5 && subcommand.equals("arena") && arguments[1].equalsIgnoreCase("setpos")) {
            return filterByPrefix(collectCornerArguments(), arguments[4]);
        }
        return List.of();
    }

    private List<String> collectActiveMatchIds() {
        return matchService.getActiveMatches().stream().map(Match::getMatchId).toList();
    }

    private List<String> collectRegionTypeArguments() {
        return Arrays.stream(ArenaRegionType.values()).map(ArenaRegionType::getCommandArgument).toList();
    }

    private List<String> collectCornerArguments() {
        return Arrays.stream(RegionCorner.values()).map(RegionCorner::getCommandArgument).toList();
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
