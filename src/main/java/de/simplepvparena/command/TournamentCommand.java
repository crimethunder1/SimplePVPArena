package de.simplepvparena.command;

import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.gui.AdminMenuService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class TournamentCommand implements TabExecutor {

    public static final String ADMIN_PERMISSION = "tournament.admin";
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
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            TournamentMessages.sendError(sender, "You are not allowed to use this command");
            return;
        }
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, "The tournament administration is a menu"
                    + TournamentMessages.LINE_BREAK + "It can only be opened by a player, not from the console");
            return;
        }
        adminMenuService.openMainMenu(player);
    }

    private void handleSpectate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            TournamentMessages.sendError(sender, "This command can only be used as a player");
            return;
        }
        if (!player.hasPermission(SPECTATE_PERMISSION)) {
            TournamentMessages.sendError(player, "You are not allowed to use this command");
            return;
        }
        if (matchService.isActiveFighter(player.getUniqueId())) {
            TournamentMessages.sendError(player, "You cannot leave the arena while you are fighting");
            return;
        }
        if (!spectatorService.sendToTribune(player)) {
            TournamentMessages.sendError(player, "The arena has no usable tribune position");
            return;
        }
        TournamentMessages.sendSuccess(player, "You are now watching the arena");
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
