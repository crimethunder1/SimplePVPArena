package de.simplepvparena.listener;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.tournament.AutoMatchState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchPlayAgainQuestionTest {

    private static final String QUESTION = "Play again? Click here: [Play again]";
    private static final String NO_REMATCH = "There is no rematch to confirm";
    private static final String REGISTERED = "You are registered for the next event";
    private static final String RANDOM_MODE = "The teams are formed at random, an admin forms them";

    @Test
    void everyParticipantOnTheTribuneGetsTheQuestionOnceWithAClickablePart() {
        AutoMatchHarness harness = finishedMatchOfColouredTeams();

        for (String teamName : List.of("Red", "Blue")) {
            ArenaMatchHarness.FakePlayer participant = harness.fighterOf(teamName);
            assertEquals(1, participant.messages().stream().filter(QUESTION::equals).count(),
                    participant.messages().toString());
            Component clickablePart = findClickablePart(participant.components().stream()
                    .filter(component -> TournamentMessages.toPlainBody(component).equals(QUESTION))
                    .findFirst().orElseThrow()).orElseThrow();
            assertEquals("[Play again]", TournamentMessages.toPlainText(clickablePart));
            assertEquals(ClickEvent.runCommand("/tournament playagain"), clickablePart.clickEvent());
            assertEquals(HoverEvent.Action.SHOW_TEXT, clickablePart.hoverEvent().action());
            assertEquals("Register again and choose a new team",
                    TournamentMessages.toPlainText((Component) clickablePart.hoverEvent().value()));
            assertEquals(TournamentMessages.SUCCESS_COLOUR, clickablePart.color());
            assertTrue(harness.hasOpenQuestion(participant));
        }
    }

    @Test
    void aSpectatorInTheAreaWhoDidNotPlayGetsNoQuestion() {
        AutoMatchHarness harness = new AutoMatchHarness();
        harness.addColouredTeam("Red", TeamColour.RED);
        harness.addColouredTeam("Blue", TeamColour.BLUE);
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayerInTheArea("Spectator");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        harness.winMatch("Red");

        assertFalse(spectator.messages().contains(QUESTION), spectator.messages().toString());
        assertFalse(harness.hasOpenQuestion(spectator));
    }

    @Test
    void aParticipantOutsideTheAreaGetsNoQuestionButLeavesRegistrationAndTeam() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winRound("Red");
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");
        harness.arena().forgetTribuneSpawn();
        redFighter.moveTo(harness.arena().at(AutoMatchHarness.FAR_AWAY, AutoMatchHarness.FAR_AWAY));

        harness.winRound("Red");

        assertFalse(redFighter.messages().contains(QUESTION), redFighter.messages().toString());
        assertFalse(harness.hasOpenQuestion(redFighter));
        assertFalse(harness.isRegistered(redFighter));
        assertTrue(harness.teamOf(redFighter).isEmpty());
        assertTrue(harness.fighterOf("Blue").messages().contains(QUESTION), "the other one stood in the fight area");
    }

    @Test
    void confirmingRegistersThePlayerAgainAndOffersTheTeamChoice() {
        AutoMatchHarness harness = finishedMatchOfColouredTeams();
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");
        redFighter.forgetReceivedTexts();

        harness.playAgain(redFighter);

        assertTrue(harness.isRegistered(redFighter));
        assertEquals(List.of(redFighter.uniqueId()), harness.shownTeamChoices());
        assertTrue(harness.teamOf(redFighter).isEmpty(), "the player is in no team until they choose one");
        assertEquals(List.of(REGISTERED), redFighter.messages());
        assertFalse(harness.hasOpenQuestion(redFighter), "the question is used up");
    }

    @Test
    void withTeamsFormedAtRandomConfirmingOnlyRegistersWithTheHint() {
        AutoMatchHarness harness = finishedMatchOfColouredTeams();
        harness.formTeamsAtRandom();
        ArenaMatchHarness.FakePlayer blueFighter = harness.fighterOf("Blue");
        blueFighter.forgetReceivedTexts();

        harness.playAgain(blueFighter);

        assertTrue(harness.isRegistered(blueFighter));
        assertEquals(List.of(), harness.shownTeamChoices());
        assertEquals(List.of(REGISTERED, RANDOM_MODE), blueFighter.messages());
    }

    @Test
    void theQuestionCanBeConfirmedOnlyOnce() {
        AutoMatchHarness harness = finishedMatchOfColouredTeams();
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");
        harness.playAgain(redFighter);
        redFighter.forgetReceivedTexts();

        harness.playAgain(redFighter);

        assertEquals(List.of(NO_REMATCH), redFighter.messages());
        assertTrue(harness.isRegistered(redFighter));
        assertEquals(1, harness.shownTeamChoices().size());
    }

    @Test
    void afterThePlayAgainSecondsThereIsNoRematchToConfirm() {
        AutoMatchHarness harness = finishedMatchOfColouredTeams();
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");
        harness.advanceSeconds(AutoMatchHarness.PLAY_AGAIN_SECONDS - 1);
        assertTrue(harness.hasOpenQuestion(redFighter));

        harness.advanceSeconds(1);
        redFighter.forgetReceivedTexts();
        harness.playAgain(redFighter);

        assertFalse(harness.hasOpenQuestion(redFighter));
        assertEquals(List.of(NO_REMATCH), redFighter.messages());
        assertFalse(harness.isRegistered(redFighter));
    }

    @Test
    void aPlayerWhoNeverGotTheQuestionHasNoRematchToConfirm() {
        AutoMatchHarness harness = new AutoMatchHarness();
        ArenaMatchHarness.FakePlayer visitor = harness.addPlayerInTheArea("Visitor");

        harness.playAgain(visitor);

        assertEquals(List.of(NO_REMATCH), visitor.messages());
        assertFalse(harness.isRegistered(visitor));
    }

    @Test
    void aMemberOfATeamWithoutAColourConfirmsWithoutATeamChoiceAndHisTeamIsPairedAgain() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.registerEveryFighter();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();
        ArenaMatchHarness.FakePlayer alphaFighter = harness.fighterOf("Alpha");
        assertTrue(alphaFighter.messages().contains(QUESTION), alphaFighter.messages().toString());
        alphaFighter.forgetReceivedTexts();

        harness.playAgain(alphaFighter);

        assertEquals(List.of(REGISTERED), alphaFighter.messages());
        assertTrue(harness.isRegistered(alphaFighter));
        assertEquals(List.of(), harness.shownTeamChoices(), "the team of an admin is kept, no team is chosen");
        assertEquals("Alpha", harness.teamOf(alphaFighter).orElseThrow().getTeamName());
        assertFalse(harness.playAgainService().isWaitingForPlayAgain(harness.teamRegistry().findTeam("Alpha")
                .orElseThrow()));

        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS + 5);

        assertEquals(AutoMatchState.IDLE, harness.state(), "Bravo still waits for a confirmation");

        harness.playAgain(harness.fighterOf("Bravo"));
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
    }

    @Test
    @UseLanguage("de")
    void asksAndAnswersInGerman() {
        AutoMatchHarness harness = finishedMatchOfColouredTeams();
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");

        assertTrue(redFighter.messages().contains("Nochmal spielen? Klicke hier: [Nochmal spielen]"),
                redFighter.messages().toString());
        Component clickablePart = findClickablePart(redFighter.components().getLast()).orElseThrow();
        assertEquals("Melde dich neu an und wähle ein neues Team",
                TournamentMessages.toPlainText((Component) clickablePart.hoverEvent().value()));

        harness.playAgain(redFighter);
        redFighter.forgetReceivedTexts();
        harness.playAgain(redFighter);

        assertEquals(List.of("Es gibt keine Revanche zu bestätigen"), redFighter.messages());
    }

    private static AutoMatchHarness colouredTeams() {
        AutoMatchHarness harness = new AutoMatchHarness();
        harness.addColouredTeam("Red", TeamColour.RED);
        harness.addColouredTeam("Blue", TeamColour.BLUE);
        harness.registerEveryFighter();
        return harness;
    }

    private static AutoMatchHarness finishedMatchOfColouredTeams() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Red");
        return harness;
    }

    static Optional<Component> findClickablePart(Component component) {
        if (component.clickEvent() != null) {
            return Optional.of(component);
        }
        for (Component child : component.children()) {
            Optional<Component> clickablePart = findClickablePart(child);
            if (clickablePart.isPresent()) {
                return clickablePart;
            }
        }
        return Optional.empty();
    }
}
