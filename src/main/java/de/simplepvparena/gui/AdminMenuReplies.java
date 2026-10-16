package de.simplepvparena.gui;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.entity.Player;

import java.util.List;

final class AdminMenuReplies {

    private AdminMenuReplies() {
    }

    static boolean isStillAdmin(Player player) {
        if (AdminPermission.isAdmin(player)) {
            return true;
        }
        TournamentMessages.sendError(player, "You are not allowed to use the tournament menu");
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
}
