package de.simplepvparena.input;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatInputRegistryTest {

    private static final long TIMEOUT_MILLIS = 60_000L;

    private final AtomicLong clock = new AtomicLong(1_000L);
    private final ChatInputRegistry registry = new ChatInputRegistry(TIMEOUT_MILLIS, clock::get);
    private final UUID admin = UUID.randomUUID();
    private final UUID otherAdmin = UUID.randomUUID();
    private final List<String> handledLines = new ArrayList<>();

    @Test
    void handsTheTypedLineToTheHandlerThatWasWaitingForIt() {
        registry.await(admin, "the team to add", handledLines::add);

        registry.take(admin).orElseThrow().lineHandler().accept("Alpha Steve Alex");

        assertEquals(List.of("Alpha Steve Alex"), handledLines);
    }

    @Test
    void answersOnlyOnce() {
        registry.await(admin, "the team to add", handledLines::add);

        assertTrue(registry.take(admin).isPresent());
        assertTrue(registry.take(admin).isEmpty());
        assertFalse(registry.isWaiting(admin));
    }

    @Test
    void takesNothingForAnAdminWhoWasNeverAsked() {
        assertTrue(registry.take(admin).isEmpty());
        assertTrue(registry.take(null).isEmpty());
        assertFalse(registry.isWaiting(admin));
    }

    @Test
    void keepsTheQuestionsOfTwoAdminsApart() {
        registry.await(admin, "the team to add", line -> handledLines.add("admin " + line));
        registry.await(otherAdmin, "the border blocks", line -> handledLines.add("other " + line));

        registry.take(otherAdmin).orElseThrow().lineHandler().accept("glass 16");

        assertEquals(List.of("other glass 16"), handledLines);
        assertTrue(registry.isWaiting(admin));
    }

    @Test
    void replacesAnEarlierQuestionOfTheSameAdmin() {
        registry.await(admin, "the team to add", line -> handledLines.add("first " + line));
        registry.await(admin, "the border blocks", line -> handledLines.add("second " + line));

        registry.take(admin).orElseThrow().lineHandler().accept("glass 16");

        assertEquals(List.of("second glass 16"), handledLines);
        assertEquals(0, registry.getPendingCount());
    }

    @Test
    void staysOpenUntilTheTimeoutIsReached() {
        registry.await(admin, "the team to add", handledLines::add);

        clock.set(1_000L + TIMEOUT_MILLIS - 1L);

        assertTrue(registry.isWaiting(admin));
        assertEquals(List.of(), registry.collectExpired());
        assertTrue(registry.take(admin).isPresent());
    }

    @Test
    void answersNoLineAfterTheTimeout() {
        registry.await(admin, "the team to add", handledLines::add);

        clock.set(1_000L + TIMEOUT_MILLIS);

        assertFalse(registry.isWaiting(admin));
        assertTrue(registry.take(admin).isEmpty());
        assertEquals(List.of(), handledLines);
    }

    @Test
    void collectsAndDropsTheExpiredQuestionsAndKeepsTheOpenOnes() {
        registry.await(admin, "the team to add", handledLines::add);
        clock.set(1_000L + TIMEOUT_MILLIS - 1L);
        registry.await(otherAdmin, "the border blocks", handledLines::add);

        clock.set(1_000L + TIMEOUT_MILLIS);
        List<ExpiredChatInput> expired = registry.collectExpired();

        assertEquals(List.of(new ExpiredChatInput(admin, "the team to add")), expired);
        assertEquals(1, registry.getPendingCount());
        assertTrue(registry.isWaiting(otherAdmin));
    }

    @Test
    void reportsAnExpiredQuestionOnlyOnce() {
        registry.await(admin, "the team to add", handledLines::add);
        clock.set(1_000L + TIMEOUT_MILLIS);

        assertEquals(1, registry.collectExpired().size());
        assertEquals(List.of(), registry.collectExpired());
    }

    @Test
    void forgetsTheQuestionOfAnAdminWhoWentOffline() {
        registry.await(admin, "the team to add", handledLines::add);

        assertTrue(registry.forget(admin));
        assertFalse(registry.forget(admin));
        assertFalse(registry.isWaiting(admin));
        assertEquals(List.of(), handledLines);
    }

    @Test
    void dropsEveryQuestionAtOnce() {
        registry.await(admin, "the team to add", handledLines::add);
        registry.await(otherAdmin, "the border blocks", handledLines::add);

        registry.clear();

        assertEquals(0, registry.getPendingCount());
    }

    @Test
    void refusesATimeoutThatIsNotAWait() {
        assertThrows(IllegalArgumentException.class, () -> new ChatInputRegistry(0L, clock::get));
    }
}
