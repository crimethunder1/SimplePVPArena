package de.simplepvparena.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentMessagesTest {

    @Test
    void everyCategoryCarriesThePrefix() {
        assertEquals("[Tournament] Saved", TournamentMessages.toPlainText(TournamentMessages.success("Saved")));
        assertEquals("[Tournament] Refused", TournamentMessages.toPlainText(TournamentMessages.error("Refused")));
        assertEquals("[Tournament] Careful", TournamentMessages.toPlainText(TournamentMessages.warning("Careful")));
        assertEquals("[Tournament] A fact", TournamentMessages.toPlainText(TournamentMessages.info("A fact")));
    }

    @Test
    void everyCategoryHasItsOwnColour() {
        assertEquals(TournamentMessages.SUCCESS_COLOUR,
                colourOf(TournamentMessages.success("Saved"), "Saved"));
        assertEquals(TournamentMessages.ERROR_COLOUR,
                colourOf(TournamentMessages.error("Refused"), "Refused"));
        assertEquals(TournamentMessages.WARNING_COLOUR,
                colourOf(TournamentMessages.warning("Careful"), "Careful"));
        assertEquals(TournamentMessages.INFO_COLOUR,
                colourOf(TournamentMessages.info("A fact"), "A fact"));
    }

    @Test
    void theFourColoursAreDifferentFromEachOther() {
        List<TextColor> colours = List.of(TournamentMessages.SUCCESS_COLOUR, TournamentMessages.ERROR_COLOUR,
                TournamentMessages.WARNING_COLOUR, TournamentMessages.INFO_COLOUR);

        for (int first = 0; first < colours.size(); first++) {
            for (int second = first + 1; second < colours.size(); second++) {
                assertNotEquals(colours.get(first), colours.get(second),
                        "category " + first + " and category " + second + " share a colour");
            }
        }
    }

    @Test
    void thePrefixIsColouredAndNotPartOfTheBody() {
        Component message = TournamentMessages.info("A fact");

        assertEquals(TournamentMessages.BRACKET_COLOUR, colourOf(message, "["));
        assertEquals(TournamentMessages.PREFIX_COLOUR, colourOf(message, TournamentMessages.PREFIX_NAME));
        assertEquals(TournamentMessages.BRACKET_COLOUR, colourOf(message, "] "));
        assertEquals(TournamentMessages.INFO_COLOUR, colourOf(message, "A fact"),
                "the name of the plugin is no part of the sentence it introduces");
    }

    @Test
    void furtherLinesOfAMessageAreDetailLinesWithoutASecondPrefix() {
        Component message = TournamentMessages.error("It was refused"
                + TournamentMessages.LINE_BREAK + "because of this"
                + TournamentMessages.LINE_BREAK + "and because of that");

        String plainText = TournamentMessages.toPlainText(message);

        assertEquals("[Tournament] It was refused\nbecause of this\nand because of that", plainText);
        assertEquals(1, countOccurrences(plainText, TournamentMessages.PLAIN_PREFIX),
                "a message carries its prefix once: " + plainText);
        assertEquals(TournamentMessages.ERROR_COLOUR, colourOf(message, "It was refused"),
                "the head line is the category");
        assertEquals(TournamentMessages.DETAIL_COLOUR, colourOf(message, "because of this"));
        assertEquals(TournamentMessages.DETAIL_COLOUR, colourOf(message, "and because of that"));
    }

    @Test
    void aSingleLineMessageHoldsNoLineBreak() {
        assertFalse(TournamentMessages.toPlainText(TournamentMessages.success("Team Alpha removed"))
                .contains(TournamentMessages.LINE_BREAK));
    }

    @Test
    void detailLinesCarryNoPrefixAndTheNeutralColour() {
        Component detail = TournamentMessages.detail("  round1-match1 Alpha vs Beta");

        assertEquals("  round1-match1 Alpha vs Beta", TournamentMessages.toPlainText(detail),
                "a detail line keeps the indentation its producer gave it");
        assertEquals(TournamentMessages.DETAIL_COLOUR, detail.color());
    }

    @Test
    void everyLineSwitchesTheItalicDecorationOff() {
        for (Component messagePart : flatten(TournamentMessages.warning("Careful"
                + TournamentMessages.LINE_BREAK + "very careful"))) {
            assertEquals(TextDecoration.State.FALSE, messagePart.decoration(TextDecoration.ITALIC),
                    "italic is switched off on \"" + plainTextOf(messagePart) + "\"");
        }
    }

    @Test
    void thePlainBodyDropsThePrefixAndKeepsEverythingElse() {
        assertEquals("Saved", TournamentMessages.toPlainBody(TournamentMessages.success("Saved")));
        assertEquals("Saved\nfor good",
                TournamentMessages.toPlainBody(TournamentMessages.success("Saved"
                        + TournamentMessages.LINE_BREAK + "for good")));
        assertEquals("no prefix here",
                TournamentMessages.toPlainBody(TournamentMessages.detail("no prefix here")));
    }

    @Test
    void aBlockWritesOneHeadLineAndOneLinePerDetail() {
        RecordingReceiver receiver = new RecordingReceiver();

        TournamentMessages.sendBlock(receiver.asSender(), TournamentMessages.info("Bracket"),
                List.of("Round 1:", "  round1-match1 Alpha vs Beta"));

        assertEquals(List.of("[Tournament] Bracket", "Round 1:", "  round1-match1 Alpha vs Beta"),
                receiver.plainMessages());
    }

    @Test
    void aReportTakesItsHeadLineOutOfTheFirstLine() {
        RecordingReceiver receiver = new RecordingReceiver();

        TournamentMessages.sendSuccessReport(receiver.asSender(),
                List.of("Kit saved: 17 items", "It is written into config.yml"));

        assertEquals(List.of("[Tournament] Kit saved: 17 items", "It is written into config.yml"),
                receiver.plainMessages());
        assertEquals(TournamentMessages.SUCCESS_COLOUR,
                colourOf(receiver.messages.get(0), "Kit saved: 17 items"));
    }

    @Test
    void anEmptyReportWritesNothing() {
        RecordingReceiver receiver = new RecordingReceiver();

        TournamentMessages.sendInfoReport(receiver.asSender(), List.of());

        assertTrue(receiver.messages.isEmpty());
    }

    private static TextColor colourOf(Component message, String text) {
        return flatten(message).stream()
                .filter(messagePart -> plainTextOf(messagePart).equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no part of the message holds \"" + text + "\": "
                        + TournamentMessages.toPlainText(message)))
                .color();
    }

    private static String plainTextOf(Component component) {
        return TournamentMessages.toPlainText(component.children(List.of()));
    }

    private static List<Component> flatten(Component component) {
        List<Component> parts = new ArrayList<>();
        collect(component, parts);
        return parts;
    }

    private static void collect(Component component, List<Component> parts) {
        parts.add(component);
        component.children().forEach(child -> collect(child, parts));
    }

    private static int countOccurrences(String text, String part) {
        int count = 0;
        int index = text.indexOf(part);
        while (index >= 0) {
            count++;
            index = text.indexOf(part, index + part.length());
        }
        return count;
    }

    private static final class RecordingReceiver {

        private final List<Component> messages = new ArrayList<>();
        private final CommandSender sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class},
                (proxyInstance, method, arguments) -> switch (method.getName()) {
                    case "sendMessage" -> {
                        messages.add((Component) arguments[0]);
                        yield null;
                    }
                    case "toString" -> "recording receiver";
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "equals" -> proxyInstance == arguments[0];
                    default -> throw new UnsupportedOperationException("CommandSender." + method.getName());
                });

        private CommandSender asSender() {
            return sender;
        }

        private List<String> plainMessages() {
            return messages.stream().map(TournamentMessages::toPlainText).toList();
        }
    }
}
