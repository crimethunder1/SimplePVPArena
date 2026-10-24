package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorMode;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundCleaning;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundCleaningBorderAndRespawnTest {

    private static final double TRIBUNE_COORDINATE = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final String NO_TRIBUNE_POSITION = "The arena has no usable tribune position";

    private final ArenaMatchHarness harness = new ArenaMatchHarness();
    private final RecordingRoundCleaning roundCleaning = new RecordingRoundCleaning();
    private final ArenaMatchHarness.FakePlayer alphaFighter = harness.addPlayer("AlphaFighter");
    private final ArenaMatchHarness.FakePlayer alphaMate = harness.addPlayer("AlphaMate");
    private final ArenaMatchHarness.FakePlayer betaFighter = harness.addPlayer("BetaFighter");
    private final ArenaMatchHarness.FakePlayer betaMate = harness.addPlayer("BetaMate");
    private final ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
    private final SpectatorBorderListener borderListener = new SpectatorBorderListener(harness.getMatchService(),
            harness.getSpectatorService(), harness.getBorderService());
    private final PlayerRespawnListener respawnListener = new PlayerRespawnListener(harness.getPlugin(),
            harness.getMatchService(), harness.getSpectatorService(), harness.getArenaRegistry());

    RoundCleaningBorderAndRespawnTest() {
        MatchService matchService = harness.getMatchService();
        matchService.setRoundCleaning(roundCleaning);
        matchService.setAdminNotifier(message -> { });
        matchService.setMatchCompletionHandler((match, winnerTeamName) -> { });
    }

    @Test
    void aSurvivingFighterIsStoppedAtTheBorderMarksDuringTheCleaningLikeASpectator() {
        startMatch();
        betaWinsTheRound();
        assertTrue(harness.getMatchService().isCleaningBetweenRounds());
        assertTrue(harness.getMatchService().isActiveFighter(betaFighter.uniqueId()));

        assertTrue(isStoppedAtTheWall(betaFighter), "the surviving fighter");
        assertTrue(isStoppedAtTheWall(betaMate), "and the mate");
        assertTrue(isStoppedAtTheWall(alphaFighter), "the eliminated fighter as before");
        assertTrue(isStoppedAtTheWall(spectator), "the spectator as before");
    }

    @Test
    void afterTheCleaningAFighterPassesTheBorderMarksAsBefore() {
        startMatch();
        betaWinsTheRound();

        roundCleaning.finish();

        assertFalse(harness.getMatchService().isCleaningBetweenRounds());
        assertFalse(isStoppedAtTheWall(betaFighter));
        assertTrue(isStoppedAtTheWall(spectator));
    }

    @Test
    void withoutACleaningAFighterPassesTheBorderMarksAsBefore() {
        harness.getMatchService().setCleanBetweenRounds(false);
        startMatch();
        assertFalse(isStoppedAtTheWall(betaFighter), "during the round");

        betaWinsTheRound();

        assertFalse(harness.getMatchService().isCleaningBetweenRounds());
        assertFalse(isStoppedAtTheWall(betaFighter), "between the rounds");
        assertTrue(isStoppedAtTheWall(spectator));
    }

    @Test
    void aFighterWhoRespawnsDuringTheCleaningComesBackOnTheTribune() {
        startMatch();
        betaWinsTheRound();
        int scheduledBefore = harness.getScheduledTaskCount();

        PlayerRespawnEvent event = respawn(betaFighter, harness.at(5.5D, 5.5D));

        assertEquals(TRIBUNE_COORDINATE, event.getRespawnLocation().getX(), 1.0E-9D);
        assertFalse(harness.getShape().contains(event.getRespawnLocation()), "not in the fight area");
        assertEquals(scheduledBefore + 1, harness.getScheduledTaskCount(), "only the way to the tribune is planned");
        harness.runTick();
        assertEquals(TRIBUNE_COORDINATE, betaFighter.location().getX(), 1.0E-9D);
        assertEquals(Optional.of(SpectatorMode.TRIBUNE),
                harness.getSpectatorService().findSpectatorMode(betaFighter.uniqueId()));
        assertTrue(harness.getMatchService().isActiveFighter(betaFighter.uniqueId()),
                "they still fight the next round");
        assertFalse(betaFighter.messages().contains(NO_TRIBUNE_POSITION));
    }

    @Test
    void withoutACleaningAFighterRespawnsOnTheTeamSpawnAsBefore() {
        harness.getMatchService().setCleanBetweenRounds(false);
        startMatch();
        betaWinsTheRound();

        PlayerRespawnEvent event = respawn(betaFighter, harness.at(-20.5D, -20.5D));

        assertEquals(8.5D, event.getRespawnLocation().getX(), 1.0E-9D);
        assertTrue(harness.getShape().contains(event.getRespawnLocation()));
    }

    @Test
    void withoutATribuneSpawnTheFighterKeepsTheRespawnOfMinecraftAndIsWarned() {
        startMatch();
        betaWinsTheRound();
        harness.forgetTribuneSpawn();
        Location respawnOfMinecraft = harness.at(-20.5D, -20.5D);

        PlayerRespawnEvent event = respawn(betaFighter, respawnOfMinecraft);
        harness.runTick();

        assertEquals(respawnOfMinecraft, event.getRespawnLocation());
        assertEquals(List.of(NO_TRIBUNE_POSITION), betaFighter.messages().stream()
                .filter(NO_TRIBUNE_POSITION::equals).toList());
    }

    @Test
    void anEliminatedFighterWithoutATribuneSpawnIsStillNotWarned() {
        startMatch();
        betaWinsTheRound();
        harness.forgetTribuneSpawn();

        respawn(alphaFighter, harness.at(-20.5D, -20.5D));
        harness.runTick();

        assertFalse(alphaFighter.messages().contains(NO_TRIBUNE_POSITION), alphaFighter.messages().toString());
    }

    private void startMatch() {
        harness.startMatch(alphaFighter, alphaMate, betaFighter, betaMate);
    }

    private void betaWinsTheRound() {
        harness.getMatchService().handlePlayerEliminated(alphaFighter.uniqueId());
        harness.getMatchService().handlePlayerEliminated(alphaMate.uniqueId());
    }

    private boolean isStoppedAtTheWall(ArenaMatchHarness.FakePlayer player) {
        PlayerMoveEvent event = new PlayerMoveEvent(player.player(), harness.at(-1.5D, 5.5D),
                harness.at(0.5D, 5.5D));
        borderListener.onPlayerMove(event);
        return event.isCancelled();
    }

    private PlayerRespawnEvent respawn(ArenaMatchHarness.FakePlayer player, Location location) {
        PlayerRespawnEvent event = new PlayerRespawnEvent(player.player(), location, false, false, false,
                PlayerRespawnEvent.RespawnReason.DEATH);
        respawnListener.onPlayerRespawn(event);
        return event;
    }

    private static final class RecordingRoundCleaning implements RoundCleaning {

        private Consumer<String> completionHandler;

        @Override
        public boolean hasBaseline() {
            return true;
        }

        @Override
        public Optional<String> startCleaningBetweenRounds(Consumer<String> completionHandler) {
            this.completionHandler = completionHandler;
            return Optional.empty();
        }

        private void finish() {
            Consumer<String> handler = completionHandler;
            assertTrue(handler != null, "no cleaning is running");
            completionHandler = null;
            handler.accept("The arena is clean");
        }
    }
}
