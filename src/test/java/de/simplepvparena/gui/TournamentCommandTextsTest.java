package de.simplepvparena.gui;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.CommandMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentCommandTextsTest {

    private static final String[] NO_ARGUMENTS = new String[0];
    private static final String[] SPECTATE = {"spectate"};

    private final List<String> receivedTexts = new ArrayList<>();

    @Test
    @UseLanguage("en")
    void answersTheConsoleAndAPlayerWithoutPermissionInEnglish() {
        AdminMenuFixture fixture = new AdminMenuFixture();
        TournamentCommand command = fixture.createTournamentCommand();
        AdminMenuFixture.OnlinePlayer player = fixture.letAnotherPlayerJoin("Steve", false);

        command.onCommand(console(), null, "tournament", NO_ARGUMENTS);
        command.onCommand(console(), null, "tournament", SPECTATE);
        command.onCommand(player.player(), null, "tournament", NO_ARGUMENTS);
        command.onCommand(player.player(), null, "tournament", SPECTATE);
        command.onCommand(spectator(), null, "tournament", SPECTATE);

        assertEquals(List.of("The tournament administration is a menu\n"
                        + "It can only be opened by a player, not from the console",
                "This command can only be used as a player",
                "The arena has no usable tribune position"), receivedTexts);
        assertEquals(List.of("You are not allowed to use this command", "You are not allowed to use this command"),
                player.messages());
    }

    @Test
    @UseLanguage("de")
    void answersTheConsoleAndAPlayerWithoutPermissionInGerman() {
        AdminMenuFixture fixture = new AdminMenuFixture();
        TournamentCommand command = fixture.createTournamentCommand();
        AdminMenuFixture.OnlinePlayer player = fixture.letAnotherPlayerJoin("Steve", false);

        command.onCommand(console(), null, "tournament", NO_ARGUMENTS);
        command.onCommand(console(), null, "tournament", SPECTATE);
        command.onCommand(player.player(), null, "tournament", NO_ARGUMENTS);
        command.onCommand(player.player(), null, "tournament", SPECTATE);
        command.onCommand(spectator(), null, "tournament", SPECTATE);

        assertEquals(List.of("Die Turnierverwaltung ist ein Menü\nNur ein Spieler kann es öffnen, nicht die Konsole",
                "Diesen Befehl kann nur ein Spieler benutzen",
                "Die Arena hat keine nutzbare Tribünenposition"), receivedTexts);
        assertEquals(List.of("Du darfst diesen Befehl nicht benutzen", "Du darfst diesen Befehl nicht benutzen"),
                player.messages());
    }

    @Test
    @UseLanguage("de")
    void namesFightingAndWatchingInGerman() {
        assertEquals("Du kannst die Arena nicht verlassen, während du kämpfst",
                Messages.text(CommandMessage.SPECTATE_WHILE_FIGHTING));
        assertEquals("Du schaust jetzt der Arena zu", Messages.text(CommandMessage.SPECTATE_WATCHING));
    }

    @Test
    @UseLanguage("en")
    void namesFightingAndWatchingInEnglish() {
        assertEquals("You cannot leave the arena while you are fighting",
                Messages.text(CommandMessage.SPECTATE_WHILE_FIGHTING));
        assertEquals("You are now watching the arena", Messages.text(CommandMessage.SPECTATE_WATCHING));
    }

    private CommandSender console() {
        return SharedBukkitServer.proxy(CommandSender.class, (method, arguments) -> switch (method.getName()) {
            case "hasPermission" -> true;
            case "sendMessage" -> {
                receivedTexts.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("CommandSender." + method.getName());
        });
    }

    private Player spectator() {
        UUID uniqueId = UUID.randomUUID();
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> uniqueId;
            case "hasPermission" -> TournamentCommand.SPECTATE_PERMISSION.equals(arguments[0]);
            case "sendMessage" -> {
                receivedTexts.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }
}
