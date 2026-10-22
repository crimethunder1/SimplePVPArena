package de.simplepvparena.gui;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketViewCommandTest {

    private static final String[] NO_ARGUMENTS = new String[0];
    private static final String[] BRACKET = {"bracket"};
    private static final int BACK_SLOT = 48;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final TournamentCommand command = fixture.createTournamentCommand();
    private final AdminMenuFixture.OnlinePlayer viewer = fixture.letAnotherPlayerJoin("Viewer", false, true);
    private final AdminMenuFixture.OnlinePlayer visitor = fixture.letAnotherPlayerJoin("Visitor", false, false);
    private final List<String> consoleTexts = new ArrayList<>();

    @Test
    void opensTheBracketViewForAPlayerWithTheViewPermission() {
        command.onCommand(viewer.player(), null, "tournament", BRACKET);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.findLastMenuOf(viewer.player()).orElseThrow();
        assertEquals(AdminMenuType.BRACKET_VIEW, menu.holder().getMenuType());
        assertEquals("Bracket (round 1)", menu.title());
        assertFalse(menu.holder().isOpenedFromMainMenu());
        assertFalse(menu.findTile(BACK_SLOT).isPresent());
        assertEquals(List.of(), viewer.messages());
    }

    @Test
    void acceptsTheArgumentInCapitalLetters() {
        command.onCommand(viewer.player(), null, "tournament", new String[] {"BRACKET"});

        assertEquals(AdminMenuType.BRACKET_VIEW,
                fixture.findLastMenuOf(viewer.player()).orElseThrow().holder().getMenuType());
    }

    @Test
    void opensTheRoundOfTheRunningMatch() {
        startATournamentOfEightTeams();
        decideRunningMatches(4);

        command.onCommand(viewer.player(), null, "tournament", BRACKET);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.findLastMenuOf(viewer.player()).orElseThrow();
        assertEquals("round2-match1", fixture.runningMatch().getMatchId());
        assertEquals("Bracket (round 2)", menu.title());
        assertEquals(1, menu.holder().getPageIndex());
    }

    @Test
    void opensTheRoundOfTheNextPlayableMatchWhileNoMatchRuns() {
        startATournamentOfEightTeams();
        decideRunningMatches(6);

        command.onCommand(viewer.player(), null, "tournament", BRACKET);

        assertFalse(fixture.matchService().hasActiveMatch());
        assertEquals("Bracket (round 3)", fixture.findLastMenuOf(viewer.player()).orElseThrow().title());
    }

    @Test
    void opensTheLastRoundOnceTheTournamentIsOver() {
        startATournamentOfEightTeams();
        decideRunningMatches(6);
        fixture.startFinal();
        decideRunningMatches(1);

        command.onCommand(viewer.player(), null, "tournament", BRACKET);

        assertEquals("Bracket (round 3)", fixture.findLastMenuOf(viewer.player()).orElseThrow().title());
    }

    @Test
    void answersTheConsoleThatOnlyAPlayerCanUseTheCommand() {
        command.onCommand(console(), null, "tournament", BRACKET);

        assertEquals(List.of("This command can only be used as a player"), consoleTexts);
    }

    @Test
    void refusesAPlayerWithoutTheViewPermission() {
        command.onCommand(visitor.player(), null, "tournament", BRACKET);

        assertTrue(fixture.findLastMenuOf(visitor.player()).isEmpty());
        assertEquals(List.of("You are not allowed to use this command"), visitor.messages());
    }

    @Test
    void letsAnAdminWithoutTheViewPermissionOpenTheBracketView() {
        command.onCommand(fixture.admin(), null, "tournament", BRACKET);

        assertEquals(AdminMenuType.BRACKET_VIEW, fixture.lastMenu().holder().getMenuType());
        assertFalse(fixture.lastMenu().findTile(BACK_SLOT).isPresent());
    }

    @Test
    void keepsTheAdministrationMenuClosedToAPlayerWithTheViewPermission() {
        command.onCommand(viewer.player(), null, "tournament", NO_ARGUMENTS);

        assertTrue(fixture.findLastMenuOf(viewer.player()).isEmpty());
        assertEquals(List.of("You are not allowed to use this command"), viewer.messages());
    }

    @Test
    void stillOpensTheAdministrationMenuForAnAdmin() {
        command.onCommand(fixture.admin(), null, "tournament", NO_ARGUMENTS);

        assertEquals(AdminMenuType.MAIN, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void completesBracketForEveryoneWhoMayViewIt() {
        assertEquals(List.of("bracket", "join", "leave"), command.onTabComplete(viewer.player(), null, "tournament",
                new String[] {""}));
        assertEquals(List.of("bracket"), command.onTabComplete(viewer.player(), null, "tournament",
                new String[] {"BR"}));
        assertEquals(List.of("bracket"), command.onTabComplete(fixture.admin(), null, "tournament",
                new String[] {"b"}));
        assertEquals(List.of(), command.onTabComplete(viewer.player(), null, "tournament", new String[] {"s"}));
    }

    @Test
    void completesNothingForAPlayerWithoutAnyPermission() {
        assertEquals(List.of(), command.onTabComplete(visitor.player(), null, "tournament", new String[] {""}));
    }

    @Test
    void completesBracketAndSpectateForASenderWithBothPermissions() {
        assertEquals(List.of("bracket", "spectate"), command.onTabComplete(console(), null, "tournament",
                new String[] {""}));
        assertEquals(List.of("spectate"), command.onTabComplete(console(), null, "tournament",
                new String[] {"sp"}));
        assertEquals(List.of(), command.onTabComplete(console(), null, "tournament",
                new String[] {"bracket", ""}));
    }

    private void startATournamentOfEightTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.startTournament();
    }

    private void decideRunningMatches(int matchCount) {
        for (int decidedMatches = 0; decidedMatches < matchCount; decidedMatches++) {
            fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        }
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
