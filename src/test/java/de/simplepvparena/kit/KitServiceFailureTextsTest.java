package de.simplepvparena.kit;

import de.simplepvparena.message.FailureDescriptions;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.PlayerStateMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitServiceFailureTextsTest {

    @Test
    void namesAFailureWithoutAMessageByItsClassInsteadOfNull() {
        assertEquals("IOException", FailureDescriptions.describe(new IOException()));
        assertEquals("IllegalStateException", FailureDescriptions.describe(new IllegalStateException()));
        assertEquals("The inventory and state of Steve could not be written to disk (IOException), they are kept in "
                        + "memory only and are lost if the server stops unexpectedly",
                Messages.text(PlayerStateMessage.NOT_WRITTEN, Placeholder.of("player", "Steve"),
                        Placeholder.of("reason", FailureDescriptions.describe(new IOException()))));
    }

    @Test
    void keepsTheMessageOfAFailureThatHasOne() {
        assertEquals("disk full", FailureDescriptions.describe(new IOException("disk full")));
    }
}
