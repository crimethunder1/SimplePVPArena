package de.simplepvparena.gui;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.registration.RegistrationEntry;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationCommandTest {

    private static final String[] JOIN = {"join"};
    private static final String[] LEAVE = {"leave"};

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final TournamentCommand command = fixture.createTournamentCommand();
    private final AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
    private final AdminMenuFixture.OnlinePlayer alex = fixture.letAnotherPlayerJoin("Alex", false, true);
    private final AdminMenuFixture.OnlinePlayer visitor = fixture.letAnotherPlayerJoin("Visitor", false, false);
    private final List<String> consoleTexts = new ArrayList<>();

    @Test
    void registersAPlayerForTheNextEvent() {
        command.onCommand(steve.player(), null, "tournament", JOIN);

        assertEquals(List.of(new RegistrationEntry(steve.player().getUniqueId(), "Steve")),
                fixture.registrationList().getEntries());
        assertEquals(List.of("You are registered for the next event"), steve.messages());
        assertEquals(1, fixture.registrationSaveCount());
    }

    @Test
    void registersAPlayerOnlyOnce() {
        command.onCommand(steve.player(), null, "tournament", JOIN);

        command.onCommand(steve.player(), null, "tournament", JOIN);

        assertEquals(1, fixture.registrationList().getCount());
        assertEquals(List.of("You are registered for the next event",
                "You are already registered, sneak and click the station or type /tournament leave to leave"),
                steve.messages());
        assertEquals(1, fixture.registrationSaveCount());
    }

    @Test
    void keepsTheOrderOfTheRegistrations() {
        command.onCommand(alex.player(), null, "tournament", JOIN);
        command.onCommand(steve.player(), null, "tournament", JOIN);

        assertEquals(List.of("Alex", "Steve"), fixture.registrationList().getEntries().stream()
                .map(RegistrationEntry::playerName).toList());
    }

    @Test
    void unregistersAPlayer() {
        command.onCommand(steve.player(), null, "tournament", JOIN);

        command.onCommand(steve.player(), null, "tournament", LEAVE);

        assertFalse(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
        assertEquals("You are no longer registered", steve.messages().get(1));
        assertEquals(2, fixture.registrationSaveCount());
    }

    @Test
    void tellsAPlayerWhoIsNotRegisteredThatThereIsNothingToLeave() {
        command.onCommand(steve.player(), null, "tournament", LEAVE);

        assertEquals(List.of("You are not registered"), steve.messages());
        assertEquals(0, fixture.registrationSaveCount());
    }

    @Test
    void acceptsTheArgumentsInCapitalLetters() {
        command.onCommand(steve.player(), null, "tournament", new String[] {"JOIN"});
        command.onCommand(alex.player(), null, "tournament", new String[] {"Join"});
        command.onCommand(steve.player(), null, "tournament", new String[] {"LEAVE"});

        assertEquals(List.of("Alex"), fixture.registrationList().getEntries().stream()
                .map(RegistrationEntry::playerName).toList());
    }

    @Test
    void registersAnAdminLikeEveryOtherPlayer() {
        command.onCommand(fixture.admin(), null, "tournament", JOIN);

        assertTrue(fixture.registrationList().isRegistered(fixture.admin().getUniqueId()));
        assertEquals(List.of("You are registered for the next event"), fixture.messages());
    }

    @Test
    void closesTheRegistrationWhileATournamentIsRunning() {
        command.onCommand(alex.player(), null, "tournament", JOIN);
        startATournament();

        command.onCommand(steve.player(), null, "tournament", JOIN);
        command.onCommand(alex.player(), null, "tournament", LEAVE);

        assertEquals(List.of("Alex"), fixture.registrationList().getEntries().stream()
                .map(RegistrationEntry::playerName).toList());
        assertEquals(List.of("Registration is closed while a tournament is in progress"), steve.messages());
        assertEquals("Registration is closed while a tournament is in progress", alex.messages().get(1));
        assertEquals(1, fixture.registrationSaveCount());
    }

    @Test
    void closesTheRegistrationWhileATournamentIsPaused() {
        startATournament();
        fixture.pauseTournament();

        command.onCommand(steve.player(), null, "tournament", JOIN);

        assertEquals(0, fixture.registrationList().getCount());
        assertEquals(List.of("Registration is closed while a tournament is in progress"), steve.messages());
    }

    @Test
    void refusesAPlayerWithoutTheViewPermission() {
        command.onCommand(visitor.player(), null, "tournament", JOIN);
        command.onCommand(visitor.player(), null, "tournament", LEAVE);

        assertEquals(0, fixture.registrationList().getCount());
        assertEquals(List.of("You are not allowed to use this command", "You are not allowed to use this command"),
                visitor.messages());
    }

    @Test
    void refusesTheConsole() {
        command.onCommand(console(), null, "tournament", JOIN);
        command.onCommand(console(), null, "tournament", LEAVE);

        assertEquals(0, fixture.registrationList().getCount());
        assertEquals(List.of("This command can only be used as a player",
                "This command can only be used as a player"), consoleTexts);
    }

    @Test
    void completesJoinAndLeaveForAPlayerWithTheViewPermission() {
        assertEquals(List.of("join"), command.onTabComplete(steve.player(), null, "tournament",
                new String[] {"j"}));
        assertEquals(List.of("leave"), command.onTabComplete(steve.player(), null, "tournament",
                new String[] {"LE"}));
        assertEquals(List.of("bracket", "join", "leave"), command.onTabComplete(fixture.admin(), null,
                "tournament", new String[] {""}));
    }

    @Test
    void completesNeitherJoinNorLeaveWithoutTheViewPermissionOrForTheConsole() {
        assertEquals(List.of(), command.onTabComplete(visitor.player(), null, "tournament", new String[] {"j"}));
        assertEquals(List.of(), command.onTabComplete(console(), null, "tournament", new String[] {"j"}));
        assertEquals(List.of(), command.onTabComplete(console(), null, "tournament", new String[] {"l"}));
    }

    private void startATournament() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
    }

    private CommandSender console() {
        return SharedBukkitServer.proxy(CommandSender.class, (method, arguments) -> switch (method.getName()) {
            case "hasPermission" -> true;
            case "sendMessage" -> {
                consoleTexts.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("CommandSender." + method.getName());
        });
    }
}
