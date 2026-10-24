package de.simplepvparena.gui;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayAgainCommandTest {

    private static final String[] PLAY_AGAIN = {"playagain"};
    private static final int PLAY_AGAIN_SECONDS = 60;
    private static final String QUESTION = "Play again? Click here: [Play again]";

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final TournamentCommand command = fixture.createTournamentCommand();
    private final AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
    private final AdminMenuFixture.OnlinePlayer alex = fixture.letAnotherPlayerJoin("Alex", false, true);
    private final AdminMenuFixture.OnlinePlayer visitor = fixture.letAnotherPlayerJoin("Visitor", false, false);
    private final List<String> consoleTexts = new ArrayList<>();

    @BeforeEach
    void storeTheFightArea() {
        fixture.storeTheFightArea();
    }

    @Test
    void theClickedQuestionRegistersAgainAndOpensTheTeamChoiceOneTickLater() {
        finishAnAutoMatchOfColouredTeams();
        assertTrue(steve.messages().contains(QUESTION), steve.messages().toString());
        steve.messages().clear();

        command.onCommand(steve.player(), null, "tournament", PLAY_AGAIN);

        assertTrue(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty(), "not before the next tick");
        fixture.runNextTick();
        assertEquals(AdminMenuType.TEAM_CHOICE, fixture.findLastMenuOf(steve.player()).orElseThrow().holder()
                .getMenuType());
        assertEquals(List.of("You are registered for the next event"), steve.messages());
        assertTrue(fixture.teamRegistry().findTeamByPlayer(steve.player().getUniqueId()).isEmpty());
    }

    @Test
    void withoutAnOpenQuestionThereIsNoRematchToConfirm() {
        command.onCommand(steve.player(), null, "tournament", PLAY_AGAIN);
        fixture.runNextTick();

        assertEquals(List.of("There is no rematch to confirm"), steve.messages());
        assertFalse(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty());
    }

    @Test
    void afterThePlayAgainSecondsTheQuestionIsOver() {
        finishAnAutoMatchOfColouredTeams();
        steve.messages().clear();
        fixture.advanceTheClockSeconds(PLAY_AGAIN_SECONDS);

        command.onCommand(steve.player(), null, "tournament", PLAY_AGAIN);

        assertEquals(List.of("There is no rematch to confirm"), steve.messages());
        assertFalse(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
    }

    @Test
    void aMemberOfATeamWithoutAColourIsRegisteredWithoutTheTeamChoice() {
        fixture.teamRegistry().registerTeam("Alpha", List.of(member(steve)));
        fixture.teamRegistry().registerTeam("Bravo", List.of(member(alex)));
        fixture.finishAnAutoMatchOf("Alpha", "Bravo", PLAY_AGAIN_SECONDS);
        steve.messages().clear();

        command.onCommand(steve.player(), null, "tournament", PLAY_AGAIN);
        fixture.runNextTick();

        assertTrue(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
        assertTrue(fixture.findLastMenuOf(steve.player()).isEmpty(), "no team choice for the team of an admin");
        assertEquals("Alpha", fixture.teamRegistry().findTeamByPlayer(steve.player().getUniqueId()).orElseThrow()
                .getTeamName());
        assertEquals(List.of("You are registered for the next event"), steve.messages());
    }

    @Test
    void theConsoleAndAPlayerWithoutTheViewPermissionMayNotConfirm() {
        command.onCommand(console(), null, "tournament", PLAY_AGAIN);
        command.onCommand(visitor.player(), null, "tournament", PLAY_AGAIN);

        assertEquals(List.of("This command can only be used as a player"), consoleTexts);
        assertEquals(List.of("You are not allowed to use this command"), visitor.messages());
    }

    @Test
    void theTabCompletionOffersTheCommandOnlyWhileTheQuestionIsOpen() {
        assertEquals(List.of(), command.onTabComplete(steve.player(), null, "tournament", new String[] {"p"}));

        finishAnAutoMatchOfColouredTeams();

        assertEquals(List.of("playagain"), command.onTabComplete(steve.player(), null, "tournament",
                new String[] {"p"}));
        assertEquals(List.of("bracket", "join", "leave", "playagain"),
                command.onTabComplete(steve.player(), null, "tournament", new String[] {""}));
        assertEquals(List.of(), command.onTabComplete(console(), null, "tournament", new String[] {"p"}));

        command.onCommand(steve.player(), null, "tournament", PLAY_AGAIN);

        assertEquals(List.of(), command.onTabComplete(steve.player(), null, "tournament", new String[] {"p"}));
    }

    @Test
    void aPlayerWithoutTheViewPermissionIsNotOfferedTheCommand() {
        fixture.teamRegistry().registerTeam("Red", List.of(member(visitor)), TeamColour.RED);
        fixture.teamRegistry().registerTeam("Blue", List.of(member(alex)), TeamColour.BLUE);
        fixture.finishAnAutoMatchOf("Red", "Blue", PLAY_AGAIN_SECONDS);

        assertTrue(fixture.playAgainService().hasOpenQuestion(visitor.player().getUniqueId()));
        assertEquals(List.of(), command.onTabComplete(visitor.player(), null, "tournament", new String[] {"p"}));
    }

    private void finishAnAutoMatchOfColouredTeams() {
        fixture.teamRegistry().registerTeam("Red", List.of(member(steve)), TeamColour.RED);
        fixture.teamRegistry().registerTeam("Blue", List.of(member(alex)), TeamColour.BLUE);
        fixture.registrationList().register(steve.player().getUniqueId(), "Steve");
        fixture.registrationList().register(alex.player().getUniqueId(), "Alex");
        fixture.finishAnAutoMatchOf("Red", "Blue", PLAY_AGAIN_SECONDS);
        assertEquals(List.of(), fixture.teamRegistry().getTeamNames());
        assertFalse(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
    }

    private static TeamMember member(AdminMenuFixture.OnlinePlayer onlinePlayer) {
        return new TeamMember(onlinePlayer.player().getUniqueId(), onlinePlayer.player().getName());
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
