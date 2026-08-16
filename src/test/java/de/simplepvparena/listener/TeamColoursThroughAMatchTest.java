package de.simplepvparena.listener;

import de.simplepvparena.match.TeamColourSettings;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamColoursThroughAMatchTest {

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;
    private ArenaMatchHarness.FakePlayer spectator;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
    }

    @Test
    void paintsBothTeamsOfTheStartedRoundInTheirOwnColour() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        RecordingFighterColours colours = harness.getFighterColours();
        assertEquals(NamedTextColor.RED, colours.findColourOf("Fighter").orElseThrow());
        assertEquals(NamedTextColor.RED, colours.findColourOf("Mate").orElseThrow());
        assertEquals(NamedTextColor.AQUA, colours.findColourOf("Opponent").orElseThrow());
        assertEquals(NamedTextColor.AQUA, colours.findColourOf("OpponentMate").orElseThrow());
        assertEquals(1, colours.getColourCallCount(), "one round start, one painting");
    }

    @Test
    void leavesEverybodyWhoIsNotFightingUncoloured() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        assertTrue(harness.getFighterColours().findColourOf("Spectator").isEmpty());
        assertEquals(List.of("Fighter", "Mate", "Opponent", "OpponentMate"),
                harness.getFighterColours().colouredFighters());
    }

    @Test
    void paintsTheConfiguredColours() {
        harness.getFighterColours().applySettings(
                new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE));

        harness.startMatch(fighter, mate, opponent, opponentMate);

        assertEquals(NamedTextColor.GOLD, harness.getFighterColours().findColourOf("Fighter").orElseThrow());
        assertEquals(NamedTextColor.DARK_PURPLE,
                harness.getFighterColours().findColourOf("Opponent").orElseThrow());
    }

    @Test
    void keepsTheColourOfAFighterWhoWasEliminatedInTheRunningRound() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        harness.getMatchService().handlePlayerEliminated(opponent.uniqueId());

        assertEquals(NamedTextColor.AQUA, harness.getFighterColours().findColourOf("Opponent").orElseThrow());
    }

    @Test
    void paintsAgainWhenTheNextRoundOfTheSameMatchStarts() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        winARoundForTheFirstTeam();

        assertEquals(2, harness.getFighterColours().getColourCallCount());
        assertEquals(List.of("Fighter", "Mate", "Opponent", "OpponentMate"),
                harness.getFighterColours().colouredFighters());
    }

    @Test
    void takesEveryColourOffWhenTheMatchIsOver() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.forgetTribuneSpawn();

        winARoundForTheFirstTeam();
        winARoundForTheFirstTeam();

        assertTrue(harness.getMatchService().getActiveMatch().isEmpty(), "the match really is over");
        assertEquals(List.of(), harness.getFighterColours().colouredFighters());
        assertTrue(harness.getFighterColours().getClearCallCount() >= 1);
    }

    @Test
    void takesEveryColourOffWhenTheMatchIsCancelled() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.forgetTribuneSpawn();

        harness.getMatchService().cancelActiveMatch();

        assertEquals(List.of(), harness.getFighterColours().colouredFighters());
    }

    @Test
    void startsTheNextMatchOnTheSameArenaWithoutARestOfTheOneBefore() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.forgetTribuneSpawn();
        winARoundForTheFirstTeam();
        winARoundForTheFirstTeam();
        ArenaMatchHarness.FakePlayer newFighter = harness.addPlayer("NewFighter");
        ArenaMatchHarness.FakePlayer newMate = harness.addPlayer("NewMate");

        harness.startMatch(newFighter, newMate, spectator, opponentMate);

        RecordingFighterColours colours = harness.getFighterColours();
        assertEquals(List.of("NewFighter", "NewMate", "Spectator", "OpponentMate"), colours.colouredFighters());
        assertTrue(colours.findColourOf("Fighter").isEmpty(), "a fighter of the match before carries nothing");
        assertTrue(colours.findColourOf("Mate").isEmpty());
        assertEquals(NamedTextColor.AQUA, colours.findColourOf("OpponentMate").orElseThrow(),
                "and a player who fights again is painted for the team he fights for now");
    }

    private void winARoundForTheFirstTeam() {
        harness.getMatchService().getActiveMatch().orElseThrow().getSecondTeam().getMemberUniqueIds()
                .forEach(memberUniqueId -> harness.getMatchService().handlePlayerEliminated(memberUniqueId));
        harness.runTick();
    }
}
