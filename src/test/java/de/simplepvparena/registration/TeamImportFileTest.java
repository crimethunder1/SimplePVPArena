package de.simplepvparena.registration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamImportFileTest {

    @Test
    void readsOneGroupPerLine() {
        TeamImportFile file = TeamImportFile.parse(List.of("Steve Alex", "Notch", "Herobrine Jeb_", "Dinnerbone"), 2);

        assertEquals(List.of(List.of("Steve", "Alex"), List.of("Notch"), List.of("Herobrine", "Jeb_"),
                List.of("Dinnerbone")), file.groups());
        assertTrue(file.problems().isEmpty());
    }

    @Test
    void acceptsCommasSemicolonsAndBlankLines() {
        TeamImportFile file = TeamImportFile.parse(List.of("", "  Steve,Alex  ", "Notch; Jeb_", "   "), 2);

        assertEquals(List.of(List.of("Steve", "Alex"), List.of("Notch", "Jeb_")), file.groups());
    }

    @Test
    void skipsAByteOrderMarkInTheFirstLine() {
        TeamImportFile file = TeamImportFile.parse(List.of("﻿Steve Alex"), 2);

        assertEquals(List.of(List.of("Steve", "Alex")), file.groups());
    }

    @Test
    void leavesOutInvalidNamesAndReportsThem() {
        TeamImportFile file = TeamImportFile.parse(List.of("Steve Al", "Ex-Player Notch"), 2);

        assertEquals(List.of(List.of("Steve"), List.of("Notch")), file.groups());
        assertEquals(List.of(new TeamImportProblem(TeamImportProblem.Kind.INVALID_NAME, 1, "Al"),
                new TeamImportProblem(TeamImportProblem.Kind.INVALID_NAME, 2, "Ex-Player")), file.problems());
    }

    @Test
    void keepsOnlyTheFirstMentionOfAName() {
        TeamImportFile file = TeamImportFile.parse(List.of("Steve Alex", "steve Notch", "ALEX"), 2);

        assertEquals(List.of(List.of("Steve", "Alex"), List.of("Notch")), file.groups());
        assertEquals(List.of(new TeamImportProblem(TeamImportProblem.Kind.NAMED_TWICE, 2, "steve"),
                new TeamImportProblem(TeamImportProblem.Kind.NAMED_TWICE, 3, "ALEX")), file.problems());
    }

    @Test
    void splitsAGroupLargerThanTheTeamSize() {
        TeamImportFile file = TeamImportFile.parse(List.of("Steve Alex Notch Jeb_ Dinnerbone"), 2);

        assertEquals(List.of(List.of("Steve", "Alex"), List.of("Notch", "Jeb_"), List.of("Dinnerbone")),
                file.groups());
        assertEquals(List.of(new TeamImportProblem(TeamImportProblem.Kind.GROUP_TOO_LARGE, 1,
                "Steve Alex Notch Jeb_ Dinnerbone")), file.problems());
    }

    @Test
    void listsEveryPlayerName() {
        TeamImportFile file = TeamImportFile.parse(List.of("Steve Alex", "Notch"), 2);

        assertEquals(List.of("Steve", "Alex", "Notch"), file.playerNames());
    }

    @Test
    void refusesATeamSizeBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> TeamImportFile.parse(List.of("Steve"), 0));
    }
}
