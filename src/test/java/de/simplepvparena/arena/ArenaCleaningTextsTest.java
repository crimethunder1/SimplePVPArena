package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.position;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaCleaningTextsTest {

    private static final long ANIMATION_TICKS = 90L;

    @Test
    @UseLanguage("en")
    void describesTheFightAreaWithOneAndWithTwoCellsInEnglish() {
        assertEquals("event with 1 cell between 0/0 and 0/0, from y 64 to y 64, calculated at y 64",
                shape(64, 64, 0, 0).describe());
        assertEquals("event with 2 cells between 0/0 and 1/0, from y 64 to y 65, calculated at y 64",
                shape(64, 65, 0, 0, 1, 0).describe());
    }

    @Test
    @UseLanguage("de")
    void describesTheFightAreaWithOneAndWithTwoCellsInGerman() {
        assertEquals("event mit 1 Zelle zwischen 0/0 und 0/0, von y 64 bis y 64, berechnet auf y 64",
                shape(64, 64, 0, 0).describe());
        assertEquals("event mit 2 Zellen zwischen 0/0 und 1/0, von y 64 bis y 65, berechnet auf y 64",
                shape(64, 65, 0, 0, 1, 0).describe());
    }

    @Test
    @UseLanguage("en")
    void describesTheBaselineInEnglish() {
        assertEquals("1 block reaching 0 blocks below the floor, 1 material in 1 block state, 1 of them is not air, "
                + "0 hold contents such as text, patterns or items", singleStoneBlock().describe());
        assertEquals("2 blocks reaching 1 block below the floor, 2 different materials in 2 different block states, "
                + "1 of them is not air, 0 hold contents such as text, patterns or items",
                singleStoneBlockWithOneLayerBelow().describe());
    }

    @Test
    @UseLanguage("de")
    void describesTheBaselineInGerman() {
        assertEquals("1 Block, der bis 0 Blöcke unter den Boden reicht, 1 Material in 1 Blockzustand, 1 davon ist "
                + "keine Luft, 0 enthalten Inhalte wie Text, Muster oder Items", singleStoneBlock().describe());
        assertEquals("2 Blöcke, die bis 1 Block unter den Boden reichen, 2 verschiedene Materialien in 2 "
                + "verschiedenen Blockzuständen, 1 davon ist keine Luft, 0 enthalten Inhalte wie Text, Muster oder "
                + "Items", singleStoneBlockWithOneLayerBelow().describe());
    }

    @Test
    @UseLanguage("en")
    void reportsTheBaselineInEnglish() {
        assertEquals("The baseline of the fight area was taken\n"
                + "1 block reaching 0 blocks below the floor, 1 material in 1 block state, 1 of them is not air, "
                + "0 hold contents such as text, patterns or items\n"
                + "\"Clean the arena\" in the arena menu of /tournament restores exactly this state instead of "
                + "emptying the area", ArenaBaselineReport.captured(singleStoneBlock()).describe());
        assertEquals("The baseline of the fight area could not be taken: the world event is not loaded\n"
                + "\"Clean the arena\" in the arena menu of /tournament sets every block to air until "
                + "\"Calculate the fight area\" in the arena menu of /tournament takes one",
                ArenaBaselineReport.failed(worldNotLoaded(ArenaCleaningMessage.BASELINE_WORLD_NOT_LOADED))
                        .describe());
    }

    @Test
    @UseLanguage("de")
    void reportsTheBaselineInGerman() {
        assertEquals("Der Ausgangszustand der Kampffläche wurde aufgenommen\n"
                + "1 Block, der bis 0 Blöcke unter den Boden reicht, 1 Material in 1 Blockzustand, 1 davon ist "
                + "keine Luft, 0 enthalten Inhalte wie Text, Muster oder Items\n"
                + "\"Arena reinigen\" im Arena-Menü von /tournament stellt genau diesen Zustand wieder her, statt "
                + "die Fläche zu leeren", ArenaBaselineReport.captured(singleStoneBlock()).describe());
        assertEquals("Der Ausgangszustand der Kampffläche konnte nicht aufgenommen werden: die Welt event ist nicht "
                + "geladen\n"
                + "\"Arena reinigen\" im Arena-Menü von /tournament setzt jeden Block auf Luft, bis "
                + "\"Kampffläche berechnen\" im Arena-Menü von /tournament einen aufnimmt",
                ArenaBaselineReport.failed(worldNotLoaded(ArenaCleaningMessage.BASELINE_WORLD_NOT_LOADED))
                        .describe());
        assertEquals("ihre Blöcke ließen sich nicht lesen (IllegalStateException)",
                Messages.text(ArenaCleaningMessage.BASELINE_BLOCKS_UNREADABLE,
                        Placeholder.of("cause", "IllegalStateException")));
        assertEquals("die Kampffläche enthält mehr als 65535 verschiedene Blockzustände, mehr als ein "
                        + "Ausgangszustand unterscheiden kann",
                Messages.text(ArenaCleaningMessage.BASELINE_TOO_MANY_BLOCK_STATES,
                        Placeholder.of("maximum", ArenaBaselineSnapshotBuilder.MAXIMUM_PALETTE_SIZE)));
    }

    @Test
    @UseLanguage("en")
    void reportsTheCleaningWithOneAndWithTwoInEnglish() {
        assertEquals("not a single block had to be changed, all 4 checked blocks already held what the cleaning "
                + "writes. 1 border mark was removed", emptied(4L, 0L, 1).describe());
        assertEquals("not a single block had to be changed, the 1 checked block already held what the cleaning "
                + "writes. 2 border marks were removed", emptied(1L, 0L, 2).describe());
        assertEquals("1 of 4 checked blocks was set to air, 0 border marks were removed",
                emptied(4L, 1L, 0).describe());
        assertEquals("2 of 4 checked blocks were restored to the baseline the last calculation took, 1 border mark "
                + "was removed", restored(4L, 2L, 1).describe());
        assertEquals("1 of 4 checked blocks was restored to the baseline the last calculation took, 2 border marks "
                + "were removed", restored(4L, 1L, 2).describe());
    }

    @Test
    @UseLanguage("de")
    void reportsTheCleaningWithOneAndWithTwoInGerman() {
        assertEquals("kein einziger Block musste geändert werden, alle 4 geprüften Blöcke enthielten schon, was die "
                + "Reinigung schreibt. 1 Wandmarkierung wurde entfernt", emptied(4L, 0L, 1).describe());
        assertEquals("kein einziger Block musste geändert werden, der 1 geprüfte Block enthielt schon, was die "
                + "Reinigung schreibt. 2 Wandmarkierungen wurden entfernt", emptied(1L, 0L, 2).describe());
        assertEquals("1 von 4 geprüften Blöcken wurde auf Luft gesetzt, 0 Wandmarkierungen wurden entfernt",
                emptied(4L, 1L, 0).describe());
        assertEquals("2 von 4 geprüften Blöcken wurden auf den Ausgangszustand der letzten Berechnung "
                + "zurückgesetzt, 1 Wandmarkierung wurde entfernt", restored(4L, 2L, 1).describe());
        assertEquals("1 von 4 geprüften Blöcken wurde auf den Ausgangszustand der letzten Berechnung "
                + "zurückgesetzt, 2 Wandmarkierungen wurden entfernt", restored(4L, 1L, 2).describe());
    }

    @Test
    @UseLanguage("en")
    void reportsTheEndOfTheCleaningInEnglish() {
        assertEquals("The arena is clean\n1 of 4 checked blocks was set to air, 0 border marks were removed",
                emptied(4L, 1L, 0).describeOutcome());
        assertEquals("The arena cleaning was stopped: the world event is not loaded\n"
                + "1 of 4 checked blocks was set to air, 0 border marks were removed\n"
                + "Run \"Clean the arena\" in the arena menu of /tournament again once the reason is gone, the "
                + "blocks that were already written stay as they are", stopped().describeOutcome());
    }

    @Test
    @UseLanguage("de")
    void reportsTheEndOfTheCleaningInGerman() {
        assertEquals("Die Arena ist gereinigt\n1 von 4 geprüften Blöcken wurde auf Luft gesetzt, 0 Wandmarkierungen "
                + "wurden entfernt", emptied(4L, 1L, 0).describeOutcome());
        assertEquals("Die Reinigung der Arena wurde abgebrochen: die Welt event ist nicht geladen\n"
                + "1 von 4 geprüften Blöcken wurde auf Luft gesetzt, 0 Wandmarkierungen wurden entfernt\n"
                + "Führe \"Arena reinigen\" im Arena-Menü von /tournament erneut aus, sobald der Grund behoben ist, "
                + "die schon geschriebenen Blöcke bleiben, wie sie sind", stopped().describeOutcome());
    }

    @Test
    @UseLanguage("en")
    void refusesABaselineOfAnotherAreaInEnglish() {
        assertEquals(Optional.of("The stored baseline does not belong to the fight area that would be cleaned, so "
                + "nothing was written\n"
                + "It was read of event with 1 cell between 0/0 and 0/0, from y 64 to y 64, calculated at y 64\n"
                + "The area is event with 2 cells between 0/0 and 1/0, from y 64 to y 65, calculated at y 64\n"
                + "Run \"Calculate the fight area\" in the arena menu of /tournament again, that takes a baseline "
                + "of the area as it is now"),
                ArenaCleaningPlan.findBaselineMismatch(shape(64, 65, 0, 0, 1, 0), Optional.of(singleStoneBlock())));
    }

    @Test
    @UseLanguage("de")
    void refusesABaselineOfAnotherAreaInGerman() {
        assertEquals(Optional.of("Der gespeicherte Ausgangszustand gehört nicht zu der Kampffläche, die gereinigt "
                + "würde, deshalb wurde nichts geschrieben\n"
                + "Er wurde von event mit 1 Zelle zwischen 0/0 und 0/0, von y 64 bis y 64, berechnet auf y 64 "
                + "eingelesen\n"
                + "Die Fläche ist event mit 2 Zellen zwischen 0/0 und 1/0, von y 64 bis y 65, berechnet auf y 64\n"
                + "Führe \"Kampffläche berechnen\" im Arena-Menü von /tournament erneut aus, das nimmt einen "
                + "Ausgangszustand der Fläche auf, wie sie jetzt ist"),
                ArenaCleaningPlan.findBaselineMismatch(shape(64, 65, 0, 0, 1, 0), Optional.of(singleStoneBlock())));
    }

    @Test
    @UseLanguage("en")
    void describesTheAnimationInEnglish() {
        assertEquals("Layer by layer from the floor up: the red flood announces the volume, the green clearing "
                + "writes it, 4.5 seconds for both together", cleaningServiceWithAnimation(ANIMATION_TICKS)
                .describeAnimation());
        assertEquals("The two coloured phases are switched off, the layers are cleared as fast as the batches allow",
                cleaningServiceWithAnimation(0L).describeAnimation());
    }

    @Test
    @UseLanguage("de")
    void describesTheAnimationInGerman() {
        assertEquals("Schicht für Schicht vom Boden aufwärts: Die rote Flut kündigt das Volumen an, die grüne "
                        + "Räumung schreibt es, 4.5 Sekunden für beide zusammen",
                cleaningServiceWithAnimation(ANIMATION_TICKS).describeAnimation());
        assertEquals("Die zwei farbigen Phasen sind ausgeschaltet, die Schichten werden so schnell geleert, wie die "
                + "Portionen es erlauben", cleaningServiceWithAnimation(0L).describeAnimation());
    }

    private static ArenaBaselineSnapshot singleStoneBlock() {
        return capture(shape(64, 64, 0, 0), Map.of(position(0, 64, 0), Material.STONE), 1);
    }

    private static ArenaBaselineSnapshot singleStoneBlockWithOneLayerBelow() {
        return capture(shape(64, 64, 0, 0), Map.of(position(0, 64, 0), Material.STONE), 1, 1);
    }

    private static ArenaCleaningReport emptied(long checkedBlockCount, long changedBlockCount, int borderMarkCount) {
        return new ArenaCleaningReport(checkedBlockCount, changedBlockCount, borderMarkCount,
                ArenaCleaningTarget.EMPTY_ARENA.describeChange());
    }

    private static ArenaCleaningReport restored(long checkedBlockCount, long changedBlockCount, int borderMarkCount) {
        return new ArenaCleaningReport(checkedBlockCount, changedBlockCount, borderMarkCount,
                singleStoneBlock().describeChange());
    }

    private static ArenaCleaningReport stopped() {
        return ArenaCleaningReport.failed(4L, 1L, 0, ArenaCleaningTarget.EMPTY_ARENA.describeChange(),
                worldNotLoaded(ArenaCleaningMessage.CLEANING_WORLD_NOT_LOADED));
    }

    private static String worldNotLoaded(ArenaCleaningMessage reason) {
        return Messages.text(reason, Placeholder.of("world", "event"));
    }

    private static ArenaCleaningService cleaningServiceWithAnimation(long animationTicks) {
        ArenaCleaningService cleaningService = new ArenaCleaningService(null, null);
        cleaningService.setAnimationDurationTicks(animationTicks);
        return cleaningService;
    }
}
