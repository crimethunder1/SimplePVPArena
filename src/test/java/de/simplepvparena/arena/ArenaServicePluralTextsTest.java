package de.simplepvparena.arena;

import de.simplepvparena.border.BorderWallOutline;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.ArenaGapMessage;
import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.BorderMessage;
import de.simplepvparena.message.MessageCatalog;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.StartGateMessage;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.position;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaServicePluralTextsTest {

    private static final String SINGULAR_MARK = "[one] ";
    private static final String PLURAL_MARK = "[other] ";
    private static final List<MessageKey> PLURAL_KEYS = List.of(ArenaSetupMessage.SHAPE_DESCRIPTION,
            ArenaCleaningMessage.BASELINE_DESCRIPTION_BLOCKS, ArenaCleaningMessage.BASELINE_DESCRIPTION_DEPTH,
            ArenaCleaningMessage.BASELINE_DESCRIPTION_MATERIALS, ArenaCleaningMessage.BASELINE_DESCRIPTION_BLOCK_STATES,
            ArenaCleaningMessage.BASELINE_DESCRIPTION_NOT_AIR, ArenaCleaningMessage.BASELINE_DESCRIPTION_CONTENTS,
            ArenaCleaningMessage.REPORT_UNCHANGED, ArenaCleaningMessage.REPORT_SET_TO_AIR,
            ArenaCleaningMessage.REPORT_RESTORED, ArenaCleaningMessage.REPORT_BORDER_MARKS_REMOVED,
            ArenaGapMessage.STOPPED_AT_CELL_LIMIT, ArenaGapMessage.DIAGNOSE_HINT,
            ArenaGapMessage.HEIGHT_MISMATCH_ALL_LISTED, ArenaGapMessage.COLUMNS_EAST, ArenaGapMessage.COLUMNS_WEST,
            ArenaGapMessage.COLUMNS_SOUTH, ArenaGapMessage.COLUMNS_NORTH, BorderMessage.OUTLINE_BLOCKS,
            BorderMessage.OUTLINE_LINES, StartGateMessage.DURATION, StartGateMessage.TILE_BLOCKS);

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void theServicesTakeTheSingularForOneAndThePluralForTwo() {
        Messages.use(catalogWithMarkedPluralForms());

        assertEquals(List.of(
                "[one] event with 1 cell between 0/0 and 0/0, from y 64 to y 64, calculated at y 64",
                "[other] event with 2 cells between 0/0 and 1/0, from y 64 to y 65, calculated at y 64",
                "[one] 1 block reaching [other] 0 blocks below the floor, [one] 1 material in [one] 1 block state, "
                        + "[one] 1 of them is not air, [other] 0 hold contents such as text, patterns or items",
                "[other] 6 blocks reaching [other] 2 blocks below the floor, [other] 2 different materials in "
                        + "[other] 2 different block states, [other] 2 of them are not air, [other] 0 hold contents "
                        + "such as text, patterns or items",
                "[one] not a single block had to be changed, the 1 checked block already held what the cleaning "
                        + "writes. [other] 2 border marks were removed",
                "[one] 1 of 4 checked blocks was restored to the baseline the last calculation took, [one] 1 border "
                        + "mark was removed",
                "[other] 2 of 4 checked blocks were set to air, [other] 0 border marks were removed",
                "north-west, [one] 1 column west and [other] 2 columns north of you, up to x/z -1/-2",
                "south-east, [other] 2 columns east and [one] 1 column south of you, up to x/z 2/1",
                "[one] The fight area could not be calculated, the flood fill was stopped at 1 cell, the limit of "
                        + "arena.shape-maximum-cells",
                "[other] The fight area could not be calculated, the flood fill was stopped at 2 cells, the limit of "
                        + "arena.shape-maximum-cells",
                "[one] 1 border block connected by [other] 0 lines",
                "[other] 2 border blocks connected by [one] 1 line",
                "[one] 1 second",
                "[other] 2 seconds"), collectThePluralTextsOfTheServices());
    }

    @Test
    @UseLanguage("en")
    void theShippedEnglishTextsCountOne() {
        assertEquals(List.of(
                "<world> with 1 cell between <minimumX>/<minimumZ> and <maximumX>/<maximumZ>, from y <floorY> to y "
                        + "<topY>, calculated at y <referenceY>",
                "1 block reaching <depth>",
                "1 block below the floor",
                "1 material in <blockStates>",
                "1 block state",
                "1 of them is not air",
                "1 holds contents such as text, patterns or items",
                "not a single block had to be changed, the 1 checked block already held what the cleaning writes. "
                        + "<borderMarks>",
                "1 of <checkedCount> checked blocks was set to air",
                "1 of <checkedCount> checked blocks was restored to the baseline the last calculation took",
                "1 border mark was removed",
                "The fight area could not be calculated, the flood fill was stopped at 1 cell, the limit of <path>",
                "<action> runs a single calculation with a higher limit, up to 1 cell, without changing config.yml, so "
                        + "you can see how far the fill really gets",
                "One of the columns the fill walked through does hold a border block, and it sits above the height "
                        + "you ran the calculation at, so it never barred its column.\nIt is at <columns>.",
                "1 column east",
                "1 column west",
                "1 column south",
                "1 column north",
                "1 border block connected by <lines>",
                "1 line",
                "1 second",
                "The gate holds 1 block"), renderEveryPluralText(1));
    }

    @Test
    @UseLanguage("en")
    void theShippedEnglishTextsCountTwo() {
        assertEquals(List.of(
                "<world> with 2 cells between <minimumX>/<minimumZ> and <maximumX>/<maximumZ>, from y <floorY> to y "
                        + "<topY>, calculated at y <referenceY>",
                "2 blocks reaching <depth>",
                "2 blocks below the floor",
                "2 different materials in <blockStates>",
                "2 different block states",
                "2 of them are not air",
                "2 hold contents such as text, patterns or items",
                "not a single block had to be changed, all 2 checked blocks already held what the cleaning writes. "
                        + "<borderMarks>",
                "2 of <checkedCount> checked blocks were set to air",
                "2 of <checkedCount> checked blocks were restored to the baseline the last calculation took",
                "2 border marks were removed",
                "The fight area could not be calculated, the flood fill was stopped at 2 cells, the limit of <path>",
                "<action> runs a single calculation with a higher limit, up to 2 cells, without changing config.yml, "
                        + "so you can see how far the fill really gets",
                "2 of the columns the fill walked through do hold a border block, and every one of those blocks sits "
                        + "above the height you ran the calculation at, so none of them barred its column.\n"
                        + "They are at <columns>.",
                "2 columns east",
                "2 columns west",
                "2 columns south",
                "2 columns north",
                "2 border blocks connected by <lines>",
                "2 lines",
                "2 seconds",
                "The gate holds 2 blocks"), renderEveryPluralText(2));
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextsCountOne() {
        assertEquals(List.of(
                "<world> mit 1 Zelle zwischen <minimumX>/<minimumZ> und <maximumX>/<maximumZ>, von y <floorY> bis y "
                        + "<topY>, berechnet auf y <referenceY>",
                "1 Block, der bis <depth> reicht",
                "1 Block unter den Boden",
                "1 Material in <blockStates>",
                "1 Blockzustand",
                "1 davon ist keine Luft",
                "1 enthält Inhalte wie Text, Muster oder Items",
                "kein einziger Block musste geändert werden, der 1 geprüfte Block enthielt schon, was die Reinigung "
                        + "schreibt. <borderMarks>",
                "1 von <checkedCount> geprüften Blöcken wurde auf Luft gesetzt",
                "1 von <checkedCount> geprüften Blöcken wurde auf den Ausgangszustand der letzten Berechnung "
                        + "zurückgesetzt",
                "1 Wandmarkierung wurde entfernt",
                "Die Kampffläche konnte nicht berechnet werden, die Füllung wurde bei 1 Zelle gestoppt, dem Limit aus "
                        + "<path>",
                "<action> führt eine einzelne Berechnung mit einem höheren Limit aus, bis zu 1 Zelle, ohne config.yml "
                        + "zu ändern, damit du siehst, wie weit die Füllung wirklich kommt",
                "Eine der Spalten, durch die die Füllung gelaufen ist, enthält einen Wandblock, und er sitzt über der "
                        + "Höhe, auf der du die Berechnung ausgeführt hast, deshalb hat er seine Spalte nie gesperrt.\n"
                        + "Sie liegt bei <columns>.",
                "1 Spalte östlich",
                "1 Spalte westlich",
                "1 Spalte südlich",
                "1 Spalte nördlich",
                "1 Wandblock, verbunden durch <lines>",
                "1 Linie",
                "1 Sekunde",
                "Das Starttor hat 1 Block"), renderEveryPluralText(1));
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextsCountTwo() {
        assertEquals(List.of(
                "<world> mit 2 Zellen zwischen <minimumX>/<minimumZ> und <maximumX>/<maximumZ>, von y <floorY> bis y "
                        + "<topY>, berechnet auf y <referenceY>",
                "2 Blöcke, die bis <depth> reichen",
                "2 Blöcke unter den Boden",
                "2 verschiedene Materialien in <blockStates>",
                "2 verschiedenen Blockzuständen",
                "2 davon sind keine Luft",
                "2 enthalten Inhalte wie Text, Muster oder Items",
                "kein einziger Block musste geändert werden, alle 2 geprüften Blöcke enthielten schon, was die "
                        + "Reinigung schreibt. <borderMarks>",
                "2 von <checkedCount> geprüften Blöcken wurden auf Luft gesetzt",
                "2 von <checkedCount> geprüften Blöcken wurden auf den Ausgangszustand der letzten Berechnung "
                        + "zurückgesetzt",
                "2 Wandmarkierungen wurden entfernt",
                "Die Kampffläche konnte nicht berechnet werden, die Füllung wurde bei 2 Zellen gestoppt, dem Limit "
                        + "aus <path>",
                "<action> führt eine einzelne Berechnung mit einem höheren Limit aus, bis zu 2 Zellen, ohne "
                        + "config.yml zu ändern, damit du siehst, wie weit die Füllung wirklich kommt",
                "2 der Spalten, durch die die Füllung gelaufen ist, enthalten einen Wandblock, und jeder dieser "
                        + "Blöcke sitzt über der Höhe, auf der du die Berechnung ausgeführt hast, deshalb hat keiner "
                        + "davon seine Spalte gesperrt.\nSie liegen bei <columns>.",
                "2 Spalten östlich",
                "2 Spalten westlich",
                "2 Spalten südlich",
                "2 Spalten nördlich",
                "2 Wandblöcke, verbunden durch <lines>",
                "2 Linien",
                "2 Sekunden",
                "Das Starttor hat 2 Blöcke"), renderEveryPluralText(2));
    }

    private static List<String> collectThePluralTextsOfTheServices() {
        List<String> texts = new ArrayList<>();
        texts.add(shape(64, 64, 0, 0).describe());
        texts.add(shape(64, 65, 0, 0, 1, 0).describe());
        texts.add(capture(shape(64, 64, 0, 0), Map.of(position(0, 64, 0), Material.STONE), 1).describe());
        texts.add(capture(shape(64, 64, 0, 0, 1, 0),
                Map.of(position(0, 64, 0), Material.STONE, position(1, 64, 0), Material.STONE), 1, 2).describe());
        texts.add(new ArenaCleaningReport(1L, 0L, 2, ArenaCleaningTarget.EMPTY_ARENA.describeChange()).describe());
        texts.add(new ArenaCleaningReport(4L, 1L, 1, ArenaCleaningMessage.REPORT_RESTORED).describe());
        texts.add(new ArenaCleaningReport(4L, 2L, 0, ArenaCleaningTarget.EMPTY_ARENA.describeChange()).describe());
        texts.add(ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0, -1, -2)).describeEscapeDirection());
        texts.add(ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0, 2, 1)).describeEscapeDirection());
        texts.add(firstLineOfTheRefusal(1, ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0))));
        texts.add(firstLineOfTheRefusal(2, ArenaFillDiagnostics.fromFillOrder(0, 0, cells(0, 0, 1, 0))));
        ArenaShape room = new ArenaShape("event", 64, 65, new long[] {HorizontalCellCodec.encode(1, 1)});
        texts.add(BorderWallOutline.around(room, (blockX, blockZ) -> blockX == 0 && blockZ == 1
                ? new int[] {64} : BorderColumnReader.NO_BORDER_BLOCKS).describe());
        texts.add(BorderWallOutline.around(room, (blockX, blockZ) -> blockX == 0 && blockZ == 1
                ? new int[] {64, 65} : BorderColumnReader.NO_BORDER_BLOCKS).describe());
        texts.add(new StartGateDuration(1).describe());
        texts.add(new StartGateDuration(2).describe());
        return texts;
    }

    private static String firstLineOfTheRefusal(int maximumCellCount, ArenaFillDiagnostics diagnostics) {
        return ArenaShapeCalculation.stoppedAtCellLimit(maximumCellCount, diagnostics).getRejectionMessage()
                .orElseThrow().split("\n")[0];
    }

    private static List<String> renderEveryPluralText(long count) {
        List<String> texts = new ArrayList<>();
        for (MessageKey key : PLURAL_KEYS) {
            Placeholder[] placeholders = key.placeholderNames().stream()
                    .filter(name -> !name.equals(Messages.COUNT_PLACEHOLDER))
                    .sorted()
                    .map(name -> Placeholder.of(name, "<" + name + ">"))
                    .toArray(Placeholder[]::new);
            texts.add(Messages.plural(key, count, placeholders));
        }
        return texts;
    }

    private static List<Long> cells(int... coordinatePairs) {
        List<Long> cells = new ArrayList<>();
        for (int cellIndex = 0; cellIndex < coordinatePairs.length / 2; cellIndex++) {
            cells.add(HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2], coordinatePairs[cellIndex * 2 + 1]));
        }
        return cells;
    }

    private static MessageCatalog catalogWithMarkedPluralForms() {
        MessageCatalog shippedEnglish = MessageCatalogLoader.shippedCatalog(MessageCatalogLoader.DEFAULT_LANGUAGE);
        Map<String, List<String>> markedForms = new HashMap<>();
        for (MessageKey key : PLURAL_KEYS) {
            mark(shippedEnglish, markedForms, key.path() + Messages.SINGULAR_SUFFIX, SINGULAR_MARK);
            mark(shippedEnglish, markedForms, key.path() + Messages.PLURAL_SUFFIX, PLURAL_MARK);
        }
        return new MessageCatalog() {
            @Override
            public Optional<List<String>> find(String path) {
                return Optional.ofNullable(markedForms.get(path)).or(() -> shippedEnglish.find(path));
            }

            @Override
            public Set<String> paths() {
                return shippedEnglish.paths();
            }
        };
    }

    private static void mark(MessageCatalog catalog, Map<String, List<String>> markedForms, String path,
                             String formMark) {
        List<String> lines = new ArrayList<>(catalog.find(path).orElseThrow());
        lines.set(0, formMark + lines.get(0));
        markedForms.put(path, lines);
    }
}
