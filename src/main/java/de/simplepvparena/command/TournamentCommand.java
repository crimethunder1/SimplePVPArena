package de.simplepvparena.command;

import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.gui.AdminMenuService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.CommandMessage;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class TournamentCommand implements TabExecutor {

    public static final String SPECTATE_PERMISSION = "tournament.spectate";

    private static final String SPECTATE_ARGUMENT = "spectate";
    private static final List<String> ARGUMENTS = List.of(SPECTATE_ARGUMENT);

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final AdminMenuService adminMenuService;

    public TournamentCommand(MatchService matchService,
                             SpectatorService spectatorService,
                             AdminMenuService adminMenuService) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.spectatorService = Objects.requireNonNull(spectatorService, "spectatorService");
        this.adminMenuService = Objects.requireNonNull(adminMenuService, "adminMenuService");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] arguments) {
        if (arguments.length > 0 && arguments[0].toLowerCase(Locale.ROOT).equals(SPECTATE_ARGUMENT)) {
            handleSpectate(sender);
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
        if (!spectatorService.sendToTribune(player)) {
            TournamentMessages.sendError(player, Messages.text(CommandMessage.SPECTATE_NO_TRIBUNE_POSITION));
            return;
        }
        TournamentMessages.sendSuccess(player, Messages.text(CommandMessage.SPECTATE_WATCHING));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] arguments) {
        if (arguments.length != 1 || !sender.hasPermission(SPECTATE_PERMISSION)) {
            return List.of();
        }
        String prefix = arguments[0].toLowerCase(Locale.ROOT);
        return ARGUMENTS.stream().filter(argument -> argument.startsWith(prefix)).toList();
    }
}
