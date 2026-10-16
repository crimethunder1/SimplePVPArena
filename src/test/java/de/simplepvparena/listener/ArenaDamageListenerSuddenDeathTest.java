package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundTimeoutSettings;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDamageListenerSuddenDeathTest {

    private static final RoundTimeoutSettings ONE_SECOND_ROUND = new RoundTimeoutSettings(1, 0, 2.0D, 1.0D);

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaDamageListener damageListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        harness.getMatchService().setRoundTimeoutSettings(ONE_SECOND_ROUND);
        harness.startMatch(fighter, mate, opponent, opponentMate);
        damageListener = new ArenaDamageListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), harness.getPlugin().getServer());
    }

    @Test
    void neverCancelsTheDamageWhileTheMatchServiceAppliesSuddenDeath() {
        MatchService matchService = harness.getMatchService();
        List<Boolean> applyingDuringDamage = new ArrayList<>();
        List<Boolean> cancelledDuringDamage = new ArrayList<>();
        fighter.reactToDamageWith(() -> {
            applyingDuringDamage.add(matchService.isApplyingSuddenDeathDamage());
            cancelledDuringDamage.add(isCancelled(fighter, DamageCause.FALL));
        });
        harness.runTick();
        assertTrue(harness.getMatchService().findRoundTimer().orElseThrow().isSuddenDeathActive());
        assertEquals(List.of(), cancelledDuringDamage, "starting sudden death deals no damage yet");

        harness.runRepeatingTasks();

        assertEquals(List.of(true), applyingDuringDamage);
        assertEquals(List.of(false), cancelledDuringDamage, "a cause that is cancelled otherwise goes through");
    }

    @Test
    void cancelsTheSameDamageAgainOnceTheSuddenDeathDamageIsApplied() {
        harness.runTick();
        harness.runRepeatingTasks();

        assertFalse(harness.getMatchService().isApplyingSuddenDeathDamage());
        assertTrue(isCancelled(fighter, DamageCause.FALL));
    }

    private boolean isCancelled(ArenaMatchHarness.FakePlayer victim, DamageCause cause) {
        EntityDamageEvent event = new EntityDamageEvent(victim.player(), cause, createDamageSource(), 1.0D);
        damageListener.onEntityDamage(event);
        return event.isCancelled();
    }

    private static DamageSource createDamageSource() {
        return ArenaMatchHarness.proxy(DamageSource.class, (method, arguments) -> {
            throw new UnsupportedOperationException("DamageSource." + method.getName());
        });
    }
}
