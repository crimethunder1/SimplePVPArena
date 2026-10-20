package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaGapMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaGapTextsTest {

    private static final String WORLD_NAME = "event";

    @Test
    @UseLanguage("en")
    void refusesACalculationThatSpreadInEnglish() {
        assertEquals(List.of(
                "The fight area could not be calculated, the flood fill was stopped at 3 cells, the limit of "
                        + "arena.shape-maximum-cells",
                "Either your border wall has a gap the fill ran through, or the area you enclosed is larger than "
                        + "that limit. The calculation cannot tell the two apart, it would have to keep filling to do "
                        + "so.",
                "The fill spread furthest north-east, 1 column east and 2 columns north of you, up to x/z 1/-2, it "
                        + "covers x 0..1, z -2..0, 2 by 3 columns, and the last columns it reached are 0/0, 1/0 and "
                        + "1/-2.",
                "Walk along your wall towards north-east and look for a missing green connection line between two "
                        + "border blocks, and if the wall is closed everywhere raise arena.shape-maximum-cells in "
                        + "config.yml and use \"Calculate the fight area\" in the arena menu of /tournament again.",
                "\"Diagnose with a higher cell limit\" in the arena menu of /tournament runs a single calculation "
                        + "with a higher limit, up to 5000000 cells, without changing config.yml, so you can see how "
                        + "far the fill really gets"), rejectionLines(3, spreadToTheNorthEast()));
    }

    @Test
    @UseLanguage("de")
    void refusesACalculationThatSpreadInGerman() {
        assertEquals(List.of(
                "Die Kampffläche konnte nicht berechnet werden, die Füllung wurde bei 3 Zellen gestoppt, dem Limit "
                        + "aus arena.shape-maximum-cells",
                "Entweder hat deine Arenawand eine Lücke, durch die die Füllung gelaufen ist, oder die Fläche, die "
                        + "du umschlossen hast, ist größer als dieses Limit. Die Berechnung kann beides nicht "
                        + "unterscheiden, dafür müsste sie weiterfüllen.",
                "Die Füllung kam am weitesten nach Nordosten, 1 Spalte östlich und 2 Spalten nördlich von dir, bis "
                        + "x/z 1/-2, sie umfasst x 0..1, z -2..0, 2 mal 3 Spalten, und die letzten Spalten, die sie "
                        + "erreicht hat, sind 0/0, 1/0 und 1/-2.",
                "Geh an deiner Wand entlang Richtung Nordosten und such nach einer fehlenden grünen Verbindungslinie "
                        + "zwischen zwei Wandblöcken, und wenn die Wand überall geschlossen ist, erhöhe "
                        + "arena.shape-maximum-cells in config.yml und nutze \"Kampffläche berechnen\" im Arena-Menü "
                        + "von /tournament erneut.",
                "\"Diagnose mit höherem Zellenlimit\" im Arena-Menü von /tournament führt eine einzelne Berechnung "
                        + "mit einem höheren Limit aus, bis zu 5000000 Zellen, ohne config.yml zu ändern, damit du "
                        + "siehst, wie weit die Füllung wirklich kommt"), rejectionLines(3, spreadToTheNorthEast()));
    }

    @Test
    @UseLanguage("en")
    void refusesACalculationThatNeverSpreadInEnglish() {
        List<String> lines = rejectionLines(1, ArenaFillDiagnostics.fromFillOrder(5, 5, cells(5, 5)));

        assertEquals("The fight area could not be calculated, the flood fill was stopped at 1 cell, the limit of "
                + "arena.shape-maximum-cells", lines.get(0));
        assertEquals("The fill spread furthest nowhere, it never left the column you are standing in, it covers "
                + "x 5..5, z 5..5, 1 by 1 columns, and the last columns it reached are 5/5.", lines.get(2));
        assertEquals("Walk along your wall and look for a missing green connection line between two border blocks, "
                + "and if the wall is closed everywhere raise arena.shape-maximum-cells in config.yml and use "
                + "\"Calculate the fight area\" in the arena menu of /tournament again.", lines.get(3));
    }

    @Test
    @UseLanguage("de")
    void refusesACalculationThatNeverSpreadInGerman() {
        List<String> lines = rejectionLines(1, ArenaFillDiagnostics.fromFillOrder(5, 5, cells(5, 5)));

        assertEquals("Die Kampffläche konnte nicht berechnet werden, die Füllung wurde bei 1 Zelle gestoppt, dem "
                + "Limit aus arena.shape-maximum-cells", lines.get(0));
        assertEquals("Die Füllung kam nirgendwohin, sie hat die Spalte, in der du stehst, nie verlassen, sie umfasst "
                + "x 5..5, z 5..5, 1 mal 1 Spalten, und die letzten Spalten, die sie erreicht hat, sind 5/5.",
                lines.get(2));
        assertEquals("Geh an deiner Wand entlang und such nach einer fehlenden grünen Verbindungslinie zwischen zwei "
                + "Wandblöcken, und wenn die Wand überall geschlossen ist, erhöhe arena.shape-maximum-cells in "
                + "config.yml und nutze \"Kampffläche berechnen\" im Arena-Menü von /tournament erneut.",
                lines.get(3));
    }

    @Test
    @UseLanguage("en")
    void namesTheColumnsAboveTheCalculationHeightInEnglish() {
        assertEquals(List.of("One of the columns the fill walked through does hold a border block, and it sits above "
                        + "the height you ran the calculation at, so it never barred its column.",
                "It is at x/z 4/2 (lowest border block at y 70, 6 above you).",
                "A border block only bars its column from its own height upwards, so the wall is built there and "
                        + "the fill still walked underneath it. That is what it looks like when you run the "
                        + "calculation standing in a dip while that part of the wall stands on a rise.",
                "Build the wall down to your own height there, or use \"Calculate the fight area\" in the arena menu "
                        + "of /tournament again while standing at least as high as the wall does."),
                heightMismatchLines(1, column(4, 2, 70)));
        assertEquals(List.of("2 of the columns the fill walked through do hold a border block, and every one of "
                        + "those blocks sits above the height you ran the calculation at, so none of them barred its "
                        + "column.",
                "They are at x/z 4/3 (lowest border block at y 65, 1 above you) and x/z 4/2 (lowest border block at "
                        + "y 70, 6 above you)."),
                heightMismatchLines(2, column(4, 2, 70), column(4, 3, 65)).subList(0, 2));
        assertEquals("The 3 nearest of them are at x/z 4/3 (lowest border block at y 65, 1 above you), x/z 5/3 "
                        + "(lowest border block at y 66, 2 above you) and x/z 4/2 (lowest border block at y 70, 6 "
                        + "above you).",
                heightMismatchLines(5, column(4, 2, 70), column(4, 3, 65), column(5, 3, 66), column(9, 9, 80))
                        .get(1));
    }

    @Test
    @UseLanguage("de")
    void namesTheColumnsAboveTheCalculationHeightInGerman() {
        assertEquals(List.of("Eine der Spalten, durch die die Füllung gelaufen ist, enthält einen Wandblock, und er "
                        + "sitzt über der Höhe, auf der du die Berechnung ausgeführt hast, deshalb hat er seine Spalte "
                        + "nie gesperrt.",
                "Sie liegt bei x/z 4/2 (unterster Wandblock auf y 70, 6 über dir).",
                "Ein Wandblock sperrt seine Spalte nur ab seiner eigenen Höhe aufwärts, die Wand steht dort also, "
                        + "und die Füllung ist trotzdem darunter durchgelaufen. So sieht es aus, wenn du die "
                        + "Berechnung in einer Senke stehend ausführst, während dieser Teil der Wand auf einer "
                        + "Erhebung steht.",
                "Baue die Wand dort bis auf deine eigene Höhe herunter, oder nutze \"Kampffläche berechnen\" im "
                        + "Arena-Menü von /tournament erneut, während du mindestens so hoch stehst wie die Wand."),
                heightMismatchLines(1, column(4, 2, 70)));
        assertEquals(List.of("2 der Spalten, durch die die Füllung gelaufen ist, enthalten einen Wandblock, und jeder "
                        + "dieser Blöcke sitzt über der Höhe, auf der du die Berechnung ausgeführt hast, deshalb hat "
                        + "keiner davon seine Spalte gesperrt.",
                "Sie liegen bei x/z 4/3 (unterster Wandblock auf y 65, 1 über dir) und x/z 4/2 (unterster Wandblock "
                        + "auf y 70, 6 über dir)."),
                heightMismatchLines(2, column(4, 2, 70), column(4, 3, 65)).subList(0, 2));
        assertEquals("Die 3 nächsten davon liegen bei x/z 4/3 (unterster Wandblock auf y 65, 1 über dir), x/z 5/3 "
                        + "(unterster Wandblock auf y 66, 2 über dir) und x/z 4/2 (unterster Wandblock auf y 70, 6 "
                        + "über dir).",
                heightMismatchLines(5, column(4, 2, 70), column(4, 3, 65), column(5, 3, 66), column(9, 9, 80))
                        .get(1));
    }

    @Test
    @UseLanguage("de")
    void namesEveryDirectionAndTheStepIntoTheWallInGerman() {
        assertEquals(List.of("Osten", "Westen", "Süden", "Norden", "Nordosten", "Südwesten", "Südosten", "Nordwesten",
                "nirgendwohin"), everyDirection());
        assertEquals("nirgendwohin, sie hat die Spalte, in der du stehst, nie verlassen",
                ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0)).describeEscapeDirection());
        assertEquals("Westen, 3 Spalten westlich und 1 Spalte südlich von dir, bis x/z -3/1",
                ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0, -3, 1)).describeEscapeDirection());
        assertEquals("Du stehst in einer Spalte mit Wandblöcken\n"
                        + "Geh in die Fläche, die die Arenawand umschließt, und berechne erneut",
                ArenaShapeCalculation.seedInsideBorder().getRejectionMessage().orElseThrow());
    }

    @Test
    @UseLanguage("en")
    void namesEveryDirectionAndTheStepIntoTheWallInEnglish() {
        assertEquals(List.of("east", "west", "south", "north", "north-east", "south-west", "south-east", "north-west",
                "nowhere"), everyDirection());
        assertEquals("west, 3 columns west and 1 column south of you, up to x/z -3/1",
                ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0, -3, 1)).describeEscapeDirection());
        assertEquals("You are standing in a border block column\n"
                        + "Step inside the area the border wall encloses and calculate again",
                ArenaShapeCalculation.seedInsideBorder().getRejectionMessage().orElseThrow());
    }

    @Test
    @UseLanguage("en")
    void describesTheTargetItsReasonsAndTheCompassInEnglish() {
        ArenaGapTarget target = new ArenaGapTarget(WORLD_NAME, 12, 64, -30, ArenaGapTarget.Reason.BORDER_TOO_HIGH);
        RecordingItemStack compass = labelledCompass(target);

        assertEquals("x/y/z 12/64/-30 in event", target.describe());
        assertEquals("Border gap compass", compass.getName());
        assertEquals(List.of("Points to a possible gap in your border wall", "Target x/y/z 12/64/-30 in event",
                "A border block stands there, above your calculation height",
                "Close the wall there and calculate the fight area again"), compass.getLore());
        assertEquals("You reached x/y/z 12/64/-30 in event, the trail ends here\n"
                        + "This is a column where you built the wall, above the height you calculated at, so the fill "
                        + "walked underneath it. Build it down to your own height here, or run the calculation again "
                        + "from a position at least as high as these blocks.\n"
                        + "Your compass keeps pointing at the same column", reached(target));
    }

    @Test
    @UseLanguage("de")
    void describesTheTargetItsReasonsAndTheCompassInGerman() {
        ArenaGapTarget target = new ArenaGapTarget(WORLD_NAME, 12, 64, -30, ArenaGapTarget.Reason.FILL_FRONT);
        RecordingItemStack compass = labelledCompass(target);

        assertEquals("x/y/z 12/64/-30 in event", target.describe());
        assertEquals("Kompass zur Wandlücke", compass.getName());
        assertEquals(List.of("Zeigt auf eine mögliche Lücke in deiner Arenawand", "Ziel x/y/z 12/64/-30 in event",
                "So weit kam die Füllung, bevor sie gestoppt wurde",
                "Schließe die Wand dort und berechne die Kampffläche erneut"), compass.getLore());
        assertEquals("Das ist die Spalte, die die Füllung als erste der Spalten erreicht hat, an denen sie gestoppt "
                + "wurde, kein Loch, das jemand gefunden hat. Fang dort an zu suchen",
                ArenaGapTarget.Reason.FILL_FRONT.getHandOutHint());
        assertEquals("Du hast x/y/z 12/64/-30 in event erreicht, die Spur endet hier\n"
                + "Hier war die Füllung, als sie gestoppt wurde, such um dich herum nach einer fehlenden grünen "
                + "Verbindungslinie zwischen zwei Wandblöcken.\n"
                + "Dein Kompass zeigt weiter auf dieselbe Spalte", reached(target));
        assertEquals(List.of("Dort steht ein Wandblock, über deiner Berechnungshöhe",
                "In dieser Spalte steht ein Wandblock, aber über der Höhe, auf der du die Berechnung ausgeführt hast, "
                        + "deshalb hat er die Füllung nie gesperrt. Das ist keine Vermutung, es ist ein Stück Wand, "
                        + "das nicht schließt",
                "Das ist eine Spalte, in der du die Wand gebaut hast, über der Höhe, auf der du berechnet hast, "
                        + "deshalb ist die Füllung darunter durchgelaufen. Baue sie hier bis auf deine eigene Höhe "
                        + "herunter, oder führe die Berechnung erneut von einer Stelle aus, die mindestens so hoch "
                        + "liegt wie diese Blöcke"),
                List.of(ArenaGapTarget.Reason.BORDER_TOO_HIGH.getLoreLine(),
                        ArenaGapTarget.Reason.BORDER_TOO_HIGH.getHandOutHint(),
                        ArenaGapTarget.Reason.BORDER_TOO_HIGH.getArrivalHint()));
    }

    private static ArenaFillDiagnostics spreadToTheNorthEast() {
        return ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0, 1, 0, 1, -2));
    }

    private static List<String> rejectionLines(int maximumCellCount, ArenaFillDiagnostics diagnostics) {
        return Arrays.asList(ArenaShapeCalculation.stoppedAtCellLimit(maximumCellCount, diagnostics)
                .getRejectionMessage().orElseThrow().split("\n"));
    }

    private static List<String> heightMismatchLines(int mismatchCount, ArenaHeightMismatchColumn... columns) {
        ArenaFillDiagnostics diagnostics = ArenaFillDiagnostics.fromFillOrder(4, 4, cells(4, 4, 5, 4), List.of(columns),
                mismatchCount);
        List<String> lines = rejectionLines(2, diagnostics);
        return lines.subList(1, 5);
    }

    private static ArenaHeightMismatchColumn column(int blockX, int blockZ, int lowestBorderY) {
        return new ArenaHeightMismatchColumn(blockX, blockZ, lowestBorderY, 64);
    }

    private static List<String> everyDirection() {
        return List.of(directionOf(10, 1), directionOf(-10, 1), directionOf(1, 10), directionOf(1, -10),
                directionOf(10, -10), directionOf(-6, 8), directionOf(30, 60), directionOf(-30, -35),
                ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0)).getEscapeDirectionName());
    }

    private static String directionOf(int farthestX, int farthestZ) {
        return ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0, farthestX, farthestZ)).getEscapeDirectionName();
    }

    private static List<Long> cells(int... coordinatePairs) {
        Long[] cells = new Long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return List.of(cells);
    }

    private static RecordingItemStack labelledCompass(ArenaGapTarget target) {
        return (RecordingItemStack) ArenaGapCompass.labelled(new RecordingItemStack(ArenaGapCompass.MATERIAL, 1), null,
                target);
    }

    private static String reached(ArenaGapTarget target) {
        return Messages.text(ArenaGapMessage.REACHED, Placeholder.of("target", target.describe()),
                Placeholder.of("hint", target.reason().getArrivalHint()));
    }
}
