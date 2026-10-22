package de.simplepvparena.gui;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.registration.TeamMode;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamChoicePopUpTest {

    private static final String[] JOIN = {"join"};
    private static final String[] LEAVE = {"leave"};
    private static final String[] TEAM = {"team"};

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final TournamentCommand command = fixture.createTournamentCommand();
    private final AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
    private final AdminMenuFixture.OnlinePlayer alex = fixture.letAnotherPlayerJoin("Alex", false, true);
    private final AdminMenuFixture.OnlinePlayer visitor = fixture.letAnotherPlayerJoin("Visitor", false, false);
    private final List<String> consoleTexts = new ArrayList<>();

    @Test
    void opensTheTeamChoiceOneTickAfterTheRegistration() {
        command.onCommand(steve.player(), null, "tournament", JOIN);

        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty(), "not before the next tick");
        assertEquals(1, fixture.scheduledTaskCount());
        fixture.runNextTick();

        assertEquals(AdminMenuType.TEAM_CHOICE, fixture.findLastMenuOf(steve.player()).orElseThrow().holder()
                .getMenuType());
        assertEquals(List.of("You are registered for the next event"), steve.messages());
    }

    @Test
    void opensTheTeamChoiceWhileTheAutoMatchModeIsOn() {
        fixture.autoMatchService().setEnabled(true);

        command.onCommand(steve.player(), null, "tournament", JOIN);
        fixture.runNextTick();

        assertEquals(AdminMenuType.TEAM_CHOICE, fixture.findLastMenuOf(steve.player()).orElseThrow().holder()
                .getMenuType());
    }

    @Test
    void opensTheTeamChoiceForARegisteringAdminAsWell() {
        command.onCommand(fixture.admin(), null, "tournament", JOIN);
        fixture.runNextTick();

        assertEquals(AdminMenuType.TEAM_CHOICE, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void opensNoTeamChoiceWhenTheTeamsAreFormedAtRandom() {
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);

        command.onCommand(steve.player(), null, "tournament", JOIN);
        fixture.runNextTick();

        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty());
        assertTrue(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
    }

    @Test
    void opensNoTeamChoiceForAPlayerWhoWasRegisteredAlready() {
        command.onCommand(steve.player(), null, "tournament", JOIN);
        fixture.runNextTick();
        fixture.openBracketViewFor(steve.player());

        command.onCommand(steve.player(), null, "tournament", JOIN);
        fixture.runNextTick();

        assertEquals(AdminMenuType.BRACKET_VIEW, fixture.findLastMenuOf(steve.player()).orElseThrow().holder()
                .getMenuType());
    }

    @Test
    void opensNoTeamChoiceWhenThePlayerLeftOrUnregisteredBeforeTheNextTick() {
        command.onCommand(steve.player(), null, "tournament", JOIN);
        command.onCommand(alex.player(), null, "tournament", JOIN);
        fixture.letPlayerLeave(steve);
        command.onCommand(alex.player(), null, "tournament", LEAVE);

        fixture.runNextTick();

        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty());
        assertTrue(fixture.findLastMenuOf(alex.player()).isEmpty());
    }

    @Test
    void opensNoTeamChoiceWhenTheModeSwitchedBeforeTheNextTick() {
        command.onCommand(steve.player(), null, "tournament", JOIN);
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);

        fixture.runNextTick();

        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty());
    }

    @Test
    void addsTheBedsForTheNewRegistrationsToAnOpenTeamChoice() {
        fixture.setTeamSize(1);
        command.onCommand(steve.player(), null, "tournament", JOIN);
        fixture.runNextTick();
        RecordingAdminMenuWindows.RecordedMenu menuOfSteve = fixture.findLastMenuOf(steve.player()).orElseThrow();
        fixture.registerPlayers("Notch");

        command.onCommand(alex.player(), null, "tournament", JOIN);

        assertTrue(menuOfSteve.findTile(2).isPresent(), "the third bed for the third registration");
        assertEquals(List.of("3 players are registered", "A team has room for 1 player",
                "Choose a bed to join its team"), menuOfSteve.findTile(TeamChoiceMenu.INFO_SLOT).orElseThrow()
                .loreLines());
    }

    @Test
    void opensTheTeamChoiceAgainWithTheCommand() {
        command.onCommand(steve.player(), null, "tournament", JOIN);
        fixture.runNextTick();
        fixture.openBracketViewFor(steve.player());

        command.onCommand(steve.player(), null, "tournament", new String[] {"TEAM"});

        assertEquals(AdminMenuType.TEAM_CHOICE, fixture.findLastMenuOf(steve.player()).orElseThrow().holder()
                .getMenuType());
        assertEquals(List.of("You are registered for the next event"), steve.messages());
    }

    @Test
    void asksAPlayerWhoIsNotRegisteredToRegisterFirst() {
        command.onCommand(steve.player(), null, "tournament", TEAM);

        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty());
        assertEquals(List.of("Register first with the station or /tournament join"), steve.messages());
    }

    @Test
    void namesTheRandomTeamsWhenTheTeamsAreFormedAtRandom() {
        fixture.registrationList().register(steve.player().getUniqueId(), "Steve");
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);

        command.onCommand(steve.player(), null, "tournament", TEAM);

        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty());
        assertEquals(List.of("The teams are formed at random, an admin forms them"), steve.messages());
    }

    @Test
    void closesTheTeamChoiceWhileATournamentIsInProgress() {
        fixture.registrationList().register(steve.player().getUniqueId(), "Steve");
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();

        command.onCommand(steve.player(), null, "tournament", TEAM);

        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty());
        assertEquals(List.of("The team choice is closed while a tournament is in progress"), steve.messages());
    }

    @Test
    void refusesAPlayerWithoutTheViewPermissionAndTheConsole() {
        fixture.registrationList().register(visitor.player().getUniqueId(), "Visitor");

        command.onCommand(visitor.player(), null, "tournament", TEAM);
        command.onCommand(console(), null, "tournament", TEAM);

        assertTrue(fixture.findLastMenuOf(visitor.player()).isEmpty());
        assertEquals(List.of("You are not allowed to use this command"), visitor.messages());
        assertEquals(List.of("This command can only be used as a player"), consoleTexts);
    }

    @Test
    void completesTeamOnlyForAPlayerWhoMayChooseATeam() {
        assertEquals(List.of(), command.onTabComplete(steve.player(), null, "tournament", new String[] {"t"}));
        fixture.registrationList().register(steve.player().getUniqueId(), "Steve");
        fixture.registrationList().register(visitor.player().getUniqueId(), "Visitor");

        assertEquals(List.of("team"), command.onTabComplete(steve.player(), null, "tournament", new String[] {"t"}));
        assertEquals(List.of("bracket", "join", "leave", "team"),
                command.onTabComplete(steve.player(), null, "tournament", new String[] {""}));
        assertEquals(List.of(), command.onTabComplete(visitor.player(), null, "tournament", new String[] {"t"}));
        assertEquals(List.of(), command.onTabComplete(console(), null, "tournament", new String[] {"t"}));
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);
        assertEquals(List.of(), command.onTabComplete(steve.player(), null, "tournament", new String[] {"t"}));
    }

    @Test
    @UseLanguage("de")
    void answersTheCommandInGerman() {
        command.onCommand(steve.player(), null, "tournament", TEAM);
        fixture.registrationList().register(steve.player().getUniqueId(), "Steve");
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);
        command.onCommand(steve.player(), null, "tournament", TEAM);
        fixture.teamModeSetting().setTeamMode(TeamMode.FREE_CHOICE);
        command.onCommand(steve.player(), null, "tournament", TEAM);

        assertEquals(List.of("Melde dich zuerst über die Station oder /tournament join an",
                "Die Teams werden zufällig gebildet, ein Admin bildet sie"), steve.messages());
        assertEquals("Wähle dein Team", fixture.findLastMenuOf(steve.player()).orElseThrow().title());
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
