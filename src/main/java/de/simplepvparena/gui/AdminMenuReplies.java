package de.simplepvparena.gui;

import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class AdminMenuReplies {

    private AdminMenuReplies() {
    }

    static boolean isStillAdmin(Player player) {
        if (AdminPermission.isAdmin(player)) {
            return true;
        }
        TournamentMessages.sendError(player, Messages.text(MenuMessage.NOT_ALLOWED));
        return false;
    }

    static void reportOperation(Player player, TournamentOperationResult result) {
        if (result.successful()) {
            TournamentMessages.sendSuccess(player, result.message());
            return;
        }
        TournamentMessages.sendError(player, result.message());
    }

    static List<String> toLoreLines(String message) {
        return List.of(message.split(TournamentMessages.LINE_BREAK, -1));
    }

    static String describeUnknownPlayer(String playerName) {
        return Messages.text(MenuMessage.UNKNOWN_PLAYER, Placeholder.of("player", playerName));
    }

    static String startWithCapitalLetter(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
