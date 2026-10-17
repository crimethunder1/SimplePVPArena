package de.simplepvparena.input;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatInputValuesTest {

    @Test
    void splitsALineIntoItsValues() {
        assertEquals(List.of("Alpha", "Steve", "Alex"), ChatInputValues.of("Alpha Steve Alex").values());
    }

    @Test
    void ignoresLeadingTrailingAndRepeatedSpaces() {
        ChatInputValues values = ChatInputValues.of("   glass    16  ");

        assertEquals(List.of("glass", "16"), values.values());
        assertEquals(2, values.count());
    }

    @Test
    void readsATabbedLineAsValuesAsWell() {
        assertEquals(List.of("glass", "16"), ChatInputValues.of("glass\t16").values());
    }

    @Test
    void readsAnEmptyLineAsNoValues() {
        assertEquals(0, ChatInputValues.of("").count());
        assertEquals(0, ChatInputValues.of("   ").count());
        assertEquals(0, ChatInputValues.of(null).count());
    }

    @Test
    void acceptsTheExpectedNumberOfValues() {
        assertTrue(ChatInputValues.of("glass 16").findCountRejection(2, "<material> <amount>").isEmpty());
    }

    @Test
    void namesTheQuestionAgainWhenNothingWasTyped() {
        assertEquals("Nothing was typed, expected <material> <amount>",
                ChatInputValues.of("  ").findCountRejection(2, "<material> <amount>").orElseThrow());
    }

    @Test
    void reportsHowManyValuesWereTypedInsteadOfTheExpectedOnes() {
        assertEquals("Expected 3 values separated by spaces (<teamName> <firstPlayer> <secondPlayer>), "
                        + "but got 2: Alpha Steve",
                ChatInputValues.of("Alpha Steve")
                        .findCountRejection(3, "<teamName> <firstPlayer> <secondPlayer>").orElseThrow());
        assertEquals("Expected 3 values separated by spaces (<teamName> <firstPlayer> <secondPlayer>), "
                        + "but got 4: Alpha Steve Alex Notch",
                ChatInputValues.of("Alpha Steve Alex Notch")
                        .findCountRejection(3, "<teamName> <firstPlayer> <secondPlayer>").orElseThrow());
    }

    @Test
    void acceptsEveryNumberOfValuesInsideTheExpectedRange() {
        assertTrue(ChatInputValues.of("Alpha Steve").findCountRejection(2, 4, "<team> <player> ...").isEmpty());
        assertTrue(ChatInputValues.of("Alpha Steve Alex").findCountRejection(2, 4, "<team> <player> ...").isEmpty());
        assertTrue(ChatInputValues.of("Alpha Steve Alex Notch").findCountRejection(2, 4, "<team> <player> ...")
                .isEmpty());
    }

    @Test
    void reportsTheExpectedRangeWhenTooFewOrTooManyValuesWereTyped() {
        assertEquals("Expected 2 to 4 values separated by spaces (<team> <player> ...), but got 1: Alpha",
                ChatInputValues.of("Alpha").findCountRejection(2, 4, "<team> <player> ...").orElseThrow());
        assertEquals("Expected 2 to 4 values separated by spaces (<team> <player> ...), "
                        + "but got 5: Alpha Steve Alex Notch Jeb_",
                ChatInputValues.of("Alpha Steve Alex Notch Jeb_").findCountRejection(2, 4, "<team> <player> ...")
                        .orElseThrow());
        assertEquals("Nothing was typed, expected <team> <player> ...",
                ChatInputValues.of(" ").findCountRejection(2, 4, "<team> <player> ...").orElseThrow());
    }
}
