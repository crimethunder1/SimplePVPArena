package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaDebugCellLimit;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.input.ChatInputValues;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.InputMessage;
import de.simplepvparena.message.MessageCatalog;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaPluralTextsTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_DIAGNOSE_SLOT = 11;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int AFFECTED_BLOCKS_LINE = 5;
    private static final String SINGULAR_MARK = "[one] ";
    private static final String PLURAL_MARK = "[other] ";
    private static final List<MessageKey> PLURAL_KEYS = List.of(ArenaSetupMessage.DIAGNOSIS_RUN,
            ArenaSetupMessage.BASELINE_TOO_LARGE, ArenaSetupMessage.BASELINE_TOO_LARGE_WITH_LAYERS,
            ArenaSetupMessage.BASELINE_READING, ArenaSetupMessage.BASELINE_READING_WITH_LAYERS,
            ArenaSetupMessage.CLEANING_COVERS_LAYERS, ArenaSetupMessage.CLEANING_STARTED,
            ArenaSetupMessage.EFFECT_AREA_DESCRIPTION, ArenaSetupMessage.EFFECT_AREA_DISTANCE,
            ArenaMenuMessage.DIAGNOSE_LIMIT, ArenaMenuMessage.AFFECTED_BLOCKS, ArenaMenuMessage.START_GATE_HANDED_OUT,
            InputMessage.EXPECTED_VALUES);

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void theArenaTakesTheSingularForOneAndThePluralForTwo() {
        Messages.use(catalogWithMarkedPluralForms());

        assertEquals(List.of(
                "[other] One calculation with a limit you type, up to 5000000 cells",
                "[other] 100 blocks are affected",
                "[other] Calculating once with a limit of 60000 cells\n"
                        + "[other] instead of the 50000 of arena.shape-maximum-cells, config.yml is not changed",
                "[other] Reading the 375 blocks from y 53 to y 67, the fight area and the layers under its floor, as "
                        + "the baseline \"Clean the arena\" in the arena menu of /tournament restores",
                "[one] 1 block from its [other] centre 0.5/0.5, radius 0.7 blocks",
                "[other] 2 blocks from its [other] centre 0.5/0.5, radius 0.7 blocks",
                "[one] Expected 1 value separated by spaces (<cells>), but got 2: 1 2",
                "[other] Expected 2 values separated by spaces (<material> <amount>), but got 1: barrier"),
                collectThePluralTextsOfTheArena());
    }

    @Test
    @UseLanguage("en")
    void theShippedEnglishTextsCountOne() {
        assertEquals(List.of(
                "Calculating once with a limit of 1 cell\n"
                        + "instead of the 50000 of arena.shape-maximum-cells, config.yml is not changed",
                "No baseline is taken of the fight area\n"
                        + "The fight area holds 1 block, more than the 4000000 a baseline covers.\n"
                        + "A sets every block of the fight area to air, the way it always did",
                "No baseline is taken of the fight area\n"
                        + "The fight area with the layers under its floor holds 1 block, more than the 4000000 a "
                        + "baseline covers.\n"
                        + "A sets every block of the fight area to air, the way it always did",
                "Reading the 1 block from y 53 to y 67, the fight area, as the baseline A restores\n"
                        + "In batches of 16384 per tick, the arena cannot be cleaned until that is done.\n"
                        + "The baseline is gone when the server restarts",
                "Reading the 1 block from y 53 to y 67, the fight area and the layers under its floor, as the "
                        + "baseline A restores\n"
                        + "In batches of 16384 per tick, the arena cannot be cleaned until that is done.\n"
                        + "The baseline is gone when the server restarts",
                "Covers y 53 to y 67, the fight area and the\n1 layer under its floor, it is restored too",
                "Cleaning the arena, 1 block between y 64 and y 67\nProcessed in batches of 4096 per tick\nB",
                "centre 3/4, radius 1 block",
                "1 block from its E",
                "One calculation with a limit you type, up to 1 cell",
                "1 block is affected",
                "You received 1 BARRIER start gate block\n"
                        + "Build the box around a team spawn out of whatever your map is made of and close it with "
                        + "this block\n"
                        + "It is part of the gate right away once you place it, breaking it takes it out again\n"
                        + "Every round start puts the whole gate back and takes it apart again from the top",
                "Expected 1 value separated by spaces (<x>), but got 3: a b c"), renderEveryPluralText(1));
    }

    @Test
    @UseLanguage("en")
    void theShippedEnglishTextsCountTwo() {
        assertEquals(List.of(
                "Calculating once with a limit of 2 cells\n"
                        + "instead of the 50000 of arena.shape-maximum-cells, config.yml is not changed",
                "No baseline is taken of the fight area\n"
                        + "The fight area holds 2 blocks, more than the 4000000 a baseline covers.\n"
                        + "A sets every block of the fight area to air, the way it always did",
                "No baseline is taken of the fight area\n"
                        + "The fight area with the layers under its floor holds 2 blocks, more than the 4000000 a "
                        + "baseline covers.\n"
                        + "A sets every block of the fight area to air, the way it always did",
                "Reading the 2 blocks from y 53 to y 67, the fight area, as the baseline A restores\n"
                        + "In batches of 16384 per tick, the arena cannot be cleaned until that is done.\n"
                        + "The baseline is gone when the server restarts",
                "Reading the 2 blocks from y 53 to y 67, the fight area and the layers under its floor, as the "
                        + "baseline A restores\n"
                        + "In batches of 16384 per tick, the arena cannot be cleaned until that is done.\n"
                        + "The baseline is gone when the server restarts",
                "Covers y 53 to y 67, the fight area and the\n2 layers under its floor, those are restored too",
                "Cleaning the arena, 2 blocks between y 64 and y 67\nProcessed in batches of 4096 per tick\nB",
                "centre 3/4, radius 2 blocks",
                "2 blocks from its E",
                "One calculation with a limit you type, up to 2 cells",
                "2 blocks are affected",
                "You received 2 BARRIER start gate blocks\n"
                        + "Build the box around a team spawn out of whatever your map is made of and close it with "
                        + "these blocks\n"
                        + "Every one of them you place is part of the gate right away, breaking one takes it out "
                        + "again\n"
                        + "Every round start puts the whole gate back and takes it apart again from the top",
                "Expected 2 values separated by spaces (<x>), but got 3: a b c"), renderEveryPluralText(2));
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextsCountOne() {
        assertEquals(List.of(
                "Einmalige Berechnung mit einem Limit von 1 Zelle\n"
                        + "statt der 50000 aus arena.shape-maximum-cells, config.yml wird nicht geändert",
                "Von der Kampffläche wird kein Ausgangszustand aufgenommen\n"
                        + "Die Kampffläche enthält 1 Block, mehr als die 4000000, die ein Ausgangszustand umfasst.\n"
                        + "A setzt jeden Block der Kampffläche auf Luft, wie bisher",
                "Von der Kampffläche wird kein Ausgangszustand aufgenommen\n"
                        + "Die Kampffläche mit den Schichten unter ihrem Boden enthält 1 Block, mehr als die "
                        + "4000000, die ein Ausgangszustand umfasst.\n"
                        + "A setzt jeden Block der Kampffläche auf Luft, wie bisher",
                "Der 1 Block von y 53 bis y 67, die Kampffläche, wird als der Ausgangszustand eingelesen, den A "
                        + "wiederherstellt\n"
                        + "In Portionen von 16384 pro Tick, bis dahin lässt sich die Arena nicht reinigen.\n"
                        + "Der Ausgangszustand ist weg, wenn der Server neu startet",
                "Der 1 Block von y 53 bis y 67, die Kampffläche und die Schichten unter ihrem Boden, wird als der "
                        + "Ausgangszustand eingelesen, den A wiederherstellt\n"
                        + "In Portionen von 16384 pro Tick, bis dahin lässt sich die Arena nicht reinigen.\n"
                        + "Der Ausgangszustand ist weg, wenn der Server neu startet",
                "Umfasst y 53 bis y 67, die Kampffläche und die\n"
                        + "1 Schicht unter ihrem Boden, auch sie wird wiederhergestellt",
                "Die Arena wird gereinigt, 1 Block zwischen y 64 und y 67\nVerarbeitet in Portionen von 4096 pro "
                        + "Tick\nB",
                "Mitte 3/4, Radius 1 Block",
                "1 Block entfernt von seiner E",
                "Eine Berechnung mit einem Limit, das du eingibst, bis zu 1 Zelle",
                "1 Block ist betroffen",
                "Du hast 1 Starttor-Block aus BARRIER bekommen\n"
                        + "Baue die Box um einen Team-Spawn aus dem, woraus deine Map besteht, und schließe sie mit "
                        + "diesem Block\n"
                        + "Sobald du ihn setzt, gehört er sofort zum Starttor, abgebaut ist er wieder draußen\n"
                        + "Jeder Rundenstart stellt das ganze Starttor wieder her und baut es von oben wieder ab",
                "Erwartet wird 1 Wert (<x>), eingegeben wurden 3: a b c"), renderEveryPluralText(1));
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextsCountTwo() {
        assertEquals(List.of(
                "Einmalige Berechnung mit einem Limit von 2 Zellen\n"
                        + "statt der 50000 aus arena.shape-maximum-cells, config.yml wird nicht geändert",
                "Von der Kampffläche wird kein Ausgangszustand aufgenommen\n"
                        + "Die Kampffläche enthält 2 Blöcke, mehr als die 4000000, die ein Ausgangszustand "
                        + "umfasst.\n"
                        + "A setzt jeden Block der Kampffläche auf Luft, wie bisher",
                "Von der Kampffläche wird kein Ausgangszustand aufgenommen\n"
                        + "Die Kampffläche mit den Schichten unter ihrem Boden enthält 2 Blöcke, mehr als die "
                        + "4000000, die ein Ausgangszustand umfasst.\n"
                        + "A setzt jeden Block der Kampffläche auf Luft, wie bisher",
                "Die 2 Blöcke von y 53 bis y 67, die Kampffläche, werden als der Ausgangszustand eingelesen, den A "
                        + "wiederherstellt\n"
                        + "In Portionen von 16384 pro Tick, bis dahin lässt sich die Arena nicht reinigen.\n"
                        + "Der Ausgangszustand ist weg, wenn der Server neu startet",
                "Die 2 Blöcke von y 53 bis y 67, die Kampffläche und die Schichten unter ihrem Boden, werden als "
                        + "der Ausgangszustand eingelesen, den A wiederherstellt\n"
                        + "In Portionen von 16384 pro Tick, bis dahin lässt sich die Arena nicht reinigen.\n"
                        + "Der Ausgangszustand ist weg, wenn der Server neu startet",
                "Umfasst y 53 bis y 67, die Kampffläche und die\n"
                        + "2 Schichten unter ihrem Boden, auch diese werden wiederhergestellt",
                "Die Arena wird gereinigt, 2 Blöcke zwischen y 64 und y 67\nVerarbeitet in Portionen von 4096 pro "
                        + "Tick\nB",
                "Mitte 3/4, Radius 2 Blöcke",
                "2 Blöcke entfernt von seiner E",
                "Eine Berechnung mit einem Limit, das du eingibst, bis zu 2 Zellen",
                "2 Blöcke sind betroffen",
                "Du hast 2 Starttor-Blöcke aus BARRIER bekommen\n"
                        + "Baue die Box um einen Team-Spawn aus dem, woraus deine Map besteht, und schließe sie mit "
                        + "diesen Blöcken\n"
                        + "Jeder davon, den du setzt, gehört sofort zum Starttor, ein abgebauter ist wieder draußen\n"
                        + "Jeder Rundenstart stellt das ganze Starttor wieder her und baut es von oben wieder ab",
                "Erwartet werden 2 durch Leerzeichen getrennte Werte (<x>), eingegeben wurden 3: a b c"),
                renderEveryPluralText(2));
    }

    private List<String> collectThePluralTextsOfTheArena() {
        List<String> texts = new ArrayList<>();
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        texts.add(loreOf(ARENA_DIAGNOSE_SLOT).getFirst());
        texts.add(loreOf(ARENA_CLEAN_SLOT).get(AFFECTED_BLOCKS_LINE));
        fixture.standInsideTheFightArea();
        fixture.click(ARENA_DIAGNOSE_SLOT);
        fixture.messages().clear();
        fixture.typeInChat("60000");
        fixture.runNextTick();
        texts.add(fixture.messages().get(0));
        texts.add(fixture.messages().get(2).lines().findFirst().orElseThrow());
        EffectArea singleCellArea = EffectArea.around(new ArenaShape("event", 64, 67,
                new long[] {HorizontalCellCodec.encode(0, 0)}), 0);
        texts.add(singleCellArea.describeDistanceOf("event", 1.5D, 0.5D));
        texts.add(singleCellArea.describeDistanceOf("event", 2.5D, 0.5D));
        texts.add(ArenaDebugCellLimit.parseLine("1 2").getRejectionMessage().orElseThrow());
        texts.add(ChatInputValues.of("barrier").findCountRejection(2, "<material> <amount>").orElseThrow());
        return texts;
    }

    private List<String> loreOf(int slot) {
        return fixture.lastMenu().findTile(slot).orElseThrow().loreLines();
    }

    private static List<String> renderEveryPluralText(long count) {
        Placeholder action = Placeholder.of("action", "A");
        return List.of(
                Messages.plural(ArenaSetupMessage.DIAGNOSIS_RUN, count, Placeholder.of("maximumCellCount", 50000),
                        Placeholder.of("path", "arena.shape-maximum-cells")),
                Messages.plural(ArenaSetupMessage.BASELINE_TOO_LARGE, count, Placeholder.of("maximum", 4000000),
                        action),
                Messages.plural(ArenaSetupMessage.BASELINE_TOO_LARGE_WITH_LAYERS, count,
                        Placeholder.of("maximum", 4000000), action),
                Messages.plural(ArenaSetupMessage.BASELINE_READING, count, Placeholder.of("floorY", 53),
                        Placeholder.of("topY", 67), action, Placeholder.of("batchSize", 16384)),
                Messages.plural(ArenaSetupMessage.BASELINE_READING_WITH_LAYERS, count, Placeholder.of("floorY", 53),
                        Placeholder.of("topY", 67), action, Placeholder.of("batchSize", 16384)),
                Messages.plural(ArenaSetupMessage.CLEANING_COVERS_LAYERS, count, Placeholder.of("floorY", 53),
                        Placeholder.of("topY", 67)),
                Messages.plural(ArenaSetupMessage.CLEANING_STARTED, count, Placeholder.of("floorY", 64),
                        Placeholder.of("topY", 67), Placeholder.of("batchSize", 4096),
                        Placeholder.of("animation", "B")),
                Messages.plural(ArenaSetupMessage.EFFECT_AREA_DESCRIPTION, count, Placeholder.of("centreX", 3),
                        Placeholder.of("centreZ", 4), Placeholder.of("radius", count)),
                Messages.plural(ArenaSetupMessage.EFFECT_AREA_DISTANCE, count, Placeholder.of("distance", count),
                        Placeholder.of("effectArea", "E")),
                Messages.plural(ArenaMenuMessage.DIAGNOSE_LIMIT, count),
                Messages.plural(ArenaMenuMessage.AFFECTED_BLOCKS, count),
                Messages.plural(ArenaMenuMessage.START_GATE_HANDED_OUT, count, Placeholder.of("material", "BARRIER")),
                Messages.plural(InputMessage.EXPECTED_VALUES, count, Placeholder.of("usage", "<x>"),
                        Placeholder.of("givenCount", 3), Placeholder.of("values", "a b c")));
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
                             String mark) {
        markedForms.put(path, catalog.find(path).orElseThrow().stream().map(line -> mark + line).toList());
    }
}
