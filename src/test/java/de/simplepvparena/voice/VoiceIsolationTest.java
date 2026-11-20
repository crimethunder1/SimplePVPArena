package de.simplepvparena.voice;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class VoiceIsolationTest {

    private static final UUID RED = UUID.randomUUID();
    private static final UUID BLUE = UUID.randomUUID();
    private static final UUID SPECTATOR = UUID.randomUUID();
    private static final VoiceRoster MATCH = new VoiceRoster(Map.of(RED, "Red", BLUE, "Blue"), Set.of(), Set.of());

    private final AtomicReference<VoiceRoster> source = new AtomicReference<>(MATCH);
    private final List<String> notices = new ArrayList<>();
    private final Set<UUID> playersInAGroup = new HashSet<>(Set.of(RED, SPECTATOR));
    private final VoiceIsolation isolation = new VoiceIsolation(source::get, new RecordingNotices());

    @Test
    void staysOpenUntilSimpleVoiceChatIsAttached() {
        isolation.refresh();

        assertFalse(isolation.isAttached());
        assertSame(VoiceRoster.EMPTY, isolation.getRoster());
        assertTrue(isolation.canHear(SPECTATOR, RED));
        assertEquals(List.of(), notices);
    }

    @Test
    void theMatchStartIsolatesTheFightersAndTakesThemOutOfTheirGroup() {
        isolation.attach(this::leaveGroup);

        isolation.refresh();

        assertFalse(isolation.canHear(SPECTATOR, RED));
        assertTrue(isolation.canHear(RED, SPECTATOR));
        assertFalse(isolation.mayJoinGroup(RED));
        assertTrue(isolation.mayJoinGroup(SPECTATOR));
        assertEquals(Set.of(SPECTATOR), playersInAGroup, "only the fighter leaves his group");
        assertTrue(notices.containsAll(List.of("isolated " + RED, "isolated " + BLUE, "left " + RED)), notices::toString);
        assertEquals(3, notices.size(), notices::toString);
    }

    @Test
    void tellsTheFightersOnlyOnceAndTellsWhoIsOut() {
        isolation.attach(this::leaveGroup);
        isolation.refresh();
        notices.clear();

        isolation.refresh();
        assertEquals(List.of(), notices);

        source.set(new VoiceRoster(MATCH.fighterTeams(), Set.of(BLUE), Set.of()));
        isolation.refresh();
        isolation.refresh();
        assertEquals(List.of("out " + BLUE), notices);
    }

    @Test
    void aFighterWhoJoinsAGroupAnywayIsTakenOutAgain() {
        isolation.attach(this::leaveGroup);
        isolation.refresh();
        notices.clear();

        playersInAGroup.add(BLUE);
        isolation.refresh();

        assertFalse(playersInAGroup.contains(BLUE));
        assertEquals(List.of("left " + BLUE), notices);
    }

    @Test
    void switchedOffNobodyIsIsolated() {
        isolation.attach(this::leaveGroup);

        isolation.setEnabled(false);

        assertFalse(isolation.isEnabled());
        assertTrue(isolation.canHear(SPECTATOR, RED));
        assertTrue(isolation.mayJoinGroup(RED));
        assertEquals(List.of(), notices);

        isolation.setEnabled(true);
        assertFalse(isolation.canHear(SPECTATOR, RED));
    }

    @Test
    void theMatchEndOpensTheVoiceChatAgain() {
        isolation.attach(this::leaveGroup);
        isolation.refresh();

        source.set(VoiceRoster.EMPTY);
        isolation.refresh();

        assertTrue(isolation.canHear(SPECTATOR, RED));
        assertTrue(isolation.mayJoinGroup(RED));
    }

    @Test
    void detachingOpensTheVoiceChat() {
        isolation.attach(this::leaveGroup);
        isolation.refresh();

        isolation.detach();

        assertFalse(isolation.isAttached());
        assertTrue(isolation.canHear(SPECTATOR, RED));
    }

    @Test
    void aRefusedGroupIsReportedToThePlayer() {
        isolation.refuseGroup(RED);

        assertEquals(List.of("refused " + RED), notices);
    }

    private boolean leaveGroup(UUID player) {
        return playersInAGroup.remove(player);
    }

    private final class RecordingNotices implements VoiceNotices {

        @Override
        public void isolated(UUID fighter) {
            notices.add("isolated " + fighter);
        }

        @Override
        public void leftGroup(UUID fighter) {
            notices.add("left " + fighter);
        }

        @Override
        public void eliminated(UUID fighter) {
            notices.add("out " + fighter);
        }

        @Override
        public void groupRefused(UUID fighter) {
            notices.add("refused " + fighter);
        }
    }
}
