package de.simplepvparena.command;

import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.gui.AdminMenuService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.CommandMessage;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.ListenerMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.permission.ViewPermission;
import de.simplepvparena.registration.RegistrationService;
import de.simplepvparena.registration.TeamImportService;
import de.simplepvparena.tournament.PlayAgainService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@NullMarked
public final class TournamentCommand implements TabExecutor {

    public static final String SPECTATE_PERMISSION = "tournament.spectate";

    private static final String SPECTATE_ARGUMENT = "spectate";
    private static final String BRACKET_ARGUMENT = "bracket";
    private static final String JOIN_ARGUMENT = "join";
    private static final String LEAVE_ARGUMENT = "leave";
    private static final String VOTE_ARGUMENT = "vote";
    private static final String TEAM_ARGUMENT = "team";
    private static final String PLAY_AGAIN_ARGUMENT = PlayAgainService.COMMAND_ARGUMENT;
    private static final String IMPORT_ARGUMENT = "import";

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final AdminMenuService adminMenuService;
    private final RegistrationService registrationService;
    private final PlayAgainService playAgainService;
    private final TeamImportService teamImportService;
    private final Logger logger;

    public TournamentCommand(MatchService matchService,
                             SpectatorService spectatorService,
                             AdminMenuService adminMenuService,
                             RegistrationService registrationService,
                             PlayAgainService playAgainService,
                             TeamImportService teamImportService,
                             Logger logger) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.spectatorService = Objects.requireNonNull(spectatorService, "spectatorService");
        this.adminMenuService = Objects.requireNonNull(adminMenuService, "adminMenuService");
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.playAgainService = Objects.requireNonNull(playAgainService, "playAgainService");
        this.teamImportService = Objects.requireNonNull(teamImportService, "teamImportService");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    @SuppressWarnings("null")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(SPECTATE_ARGUMENT)) {
            handleSpectate(sender);
            return true;
        }
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(BRACKET_ARGUMENT)) {
            openBracketView(sender);
            return true;
        }
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(JOIN_ARGUMENT)) {
            handleRegistration(sender, true);
            return true;
        }
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(LEAVE_ARGUMENT)) {
            handleRegistration(sender, false);
            return true;
        }
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(VOTE_ARGUMENT)) {
            openUnequalTeamVote(sender);
            return true;
        }
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(TEAM_ARGUMENT)) {
            openTeamChoice(sender);
            return true;
        }
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(PLAY_AGAIN_ARGUMENT)) {
            playAgain(sender);
            return true;
        }
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(IMPORT_ARGUMENT)) {
            importTeams(sender, arguments);
            return true;
        }
        openAdminMenu(sender);
        return true;
    }

    private void openAdminMenu(CommandSender sender) {
        if (!AdminPermission.isAdmin(sender)) {
            TournamentMessages.sendError(sender, Messages.text(CommonMessage.COMMAND_NOT_ALLOWED));
            return;
        }
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, Messages.text(CommandMessage.MENU_ONLY_FOR_PLAYERS));
            return;
        }
        adminMenuService.openMainMenu(player);
    }

    private void openBracketView(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, Messages.text(CommandMessage.ONLY_FOR_PLAYERS));
            return;
        }
        if (!ViewPermission.mayView(player)) {
            TournamentMessages.sendError(player, Messages.text(CommonMessage.COMMAND_NOT_ALLOWED));
            return;
        }
        adminMenuService.openBracketView(player);
    }

    private void openUnequalTeamVote(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, Messages.text(CommandMessage.ONLY_FOR_PLAYERS));
            return;
        }
        adminMenuService.openUnequalTeamVote(player);
    }

    private void openTeamChoice(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, Messages.text(CommandMessage.ONLY_FOR_PLAYERS));
            return;
        }
        if (!ViewPermission.mayView(player)) {
            TournamentMessages.sendError(player, Messages.text(CommonMessage.COMMAND_NOT_ALLOWED));
            return;
        }
        adminMenuService.openTeamChoice(player);
    }

    private void playAgain(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, Messages.text(CommandMessage.ONLY_FOR_PLAYERS));
            return;
        }
        if (!ViewPermission.mayView(player)) {
            TournamentMessages.sendError(player, Messages.text(CommonMessage.COMMAND_NOT_ALLOWED));
            return;
        }
        playAgainService.playAgain(player);
    }

    private void importTeams(CommandSender sender, String[] arguments) {
        if (!AdminPermission.isAdmin(sender)) {
            TournamentMessages.sendError(sender, Messages.text(CommonMessage.COMMAND_NOT_ALLOWED));
            return;
        }
        if (arguments.length < 2) {
            TournamentMessages.sendInfo(sender, Messages.text(RegistrationMessage.IMPORT_USAGE));
            teamImportService.showFiles(sender);
            return;
        }
        teamImportService.importFile(sender, String.join(" ", List.of(arguments).subList(1, arguments.length)));
    }

    private void handleRegistration(CommandSender sender, boolean join) {
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, Messages.text(CommandMessage.ONLY_FOR_PLAYERS));
            return;
        }
        if (!ViewPermission.mayView(player)) {
            TournamentMessages.sendError(player, Messages.text(CommonMessage.COMMAND_NOT_ALLOWED));
            return;
        }
        if (join) {
            registrationService.join(player);
            return;
        }
        registrationService.leave(player);
    }

    private void handleSpectate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, Messages.text(CommandMessage.ONLY_FOR_PLAYERS));
            return;
        }
        if (!player.hasPermission(SPECTATE_PERMISSION)) {
            TournamentMessages.sendError(player, Messages.text(CommonMessage.COMMAND_NOT_ALLOWED));
            return;
        }
        if (matchService.isActiveFighter(player.getUniqueId())) {
            TournamentMessages.sendError(player, Messages.text(CommandMessage.SPECTATE_WHILE_FIGHTING));
            return;
        }
        boolean sentToTribune;
        try {
            sentToTribune = spectatorService.sendToTribune(player);
        } catch (RuntimeException failure) {
            logger.log(Level.WARNING, Messages.text(ListenerMessage.NOT_SENT_TO_TRIBUNE,
                    Placeholder.of("player", player.getName())), failure);
            sentToTribune = false;
        }
        if (!sentToTribune) {
            TournamentMessages.sendError(player, Messages.text(CommandMessage.SPECTATE_NO_TRIBUNE_POSITION));
            return;
        }
        TournamentMessages.sendSuccess(player, Messages.text(CommandMessage.SPECTATE_WATCHING));
    }

    @Override
    @SuppressWarnings("null")
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] arguments) {
        if (arguments.length == 2 && arguments[0].equalsIgnoreCase(IMPORT_ARGUMENT)
                && AdminPermission.isAdmin(sender)) {
            String filePrefix = arguments[1].toLowerCase(Locale.ROOT);
            return teamImportService.listFileNames().stream()
                    .filter(fileName -> fileName.toLowerCase(Locale.ROOT).startsWith(filePrefix))
                    .toList();
        }
        if (arguments.length != 1) {
            return List.of();
        }
        String prefix = arguments[0].toLowerCase(Locale.ROOT);
        return findAvailableArguments(sender).stream().filter(argument -> argument.startsWith(prefix)).toList();
    }

    private List<String> findAvailableArguments(CommandSender sender) {
        List<String> availableArguments = new ArrayList<>();
        if (ViewPermission.mayView(sender)) {
            availableArguments.add(BRACKET_ARGUMENT);
        }
        if (sender instanceof Player && ViewPermission.mayView(sender)) {
            availableArguments.add(JOIN_ARGUMENT);
            availableArguments.add(LEAVE_ARGUMENT);
        }
        if (sender instanceof Player player && ViewPermission.mayView(player)
                && adminMenuService.mayChooseTeam(player)) {
            availableArguments.add(TEAM_ARGUMENT);
        }
        if (sender instanceof Player player && ViewPermission.mayView(player)
                && playAgainService.hasOpenQuestion(player.getUniqueId())) {
            availableArguments.add(PLAY_AGAIN_ARGUMENT);
        }
        if (AdminPermission.isAdmin(sender)) {
            availableArguments.add(IMPORT_ARGUMENT);
        }
        if (sender.hasPermission(SPECTATE_PERMISSION)) {
            availableArguments.add(SPECTATE_ARGUMENT);
        }
        if (sender instanceof Player player
                && matchService.getUnequalTeamVoteService().isVoter(player.getUniqueId())) {
            availableArguments.add(VOTE_ARGUMENT);
        }
        return availableArguments;
    }
}
