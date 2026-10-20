package de.simplepvparena.gate;

import de.simplepvparena.input.BlockItemRequest;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateTextsTest {

    private static final String WORLD = "event";

    private final GateServerHarness harness = new GateServerHarness(WORLD);

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    @UseLanguage("en")
    void countsTheGateDownInEnglish() {
        assertEquals(List.of("3", "The gate comes down layer by layer", "Fight!", "The gate is open"), countdown());
        assertEquals(List.of("1 second", "2 seconds"), durations());
    }

    @Test
    @UseLanguage("de")
    void countsTheGateDownInGerman() {
        assertEquals(List.of("3", "Das Starttor wird Schicht für Schicht abgebaut", "Kämpft!",
                "Das Starttor ist offen"), countdown());
        assertEquals(List.of("1 Sekunde", "2 Sekunden"), durations());
    }

    @Test
    @UseLanguage("en")
    void describesTheGateOnTheTileInEnglish() {
        assertEquals(List.of("The gate holds no block yet", "It comes down over 1 second", "The gate holds 1 block",
                "It comes down over 1 second", "The gate holds 2 blocks", "It comes down over 1 second"),
                describeTheGateWithZeroOneAndTwoBlocks());
    }

    @Test
    @UseLanguage("de")
    void describesTheGateOnTheTileInGerman() {
        assertEquals(List.of("Das Starttor hat noch keinen Block", "Es wird über 1 Sekunde abgebaut",
                "Das Starttor hat 1 Block", "Es wird über 1 Sekunde abgebaut", "Das Starttor hat 2 Blöcke",
                "Es wird über 1 Sekunde abgebaut"), describeTheGateWithZeroOneAndTwoBlocks());
    }

    @Test
    @UseLanguage("en")
    void labelsTheGateBlocksInEnglish() {
        RecordingItemStack gateBlocks = labelledGateBlocks(new StartGateDuration());

        assertEquals("Start gate block", gateBlocks.getName());
        assertEquals(List.of("Place these blocks as the wall piece that closes", "the box around a team spawn",
                "Every round start puts them back and takes them", "apart again from the top, in 10 seconds",
                "Breaking a placed one takes it out of the gate again"), gateBlocks.getLore());
        assertEquals("<material> <amount>", BlockItemRequest.describeUsage());
        assertEquals("Expected 2 values separated by spaces (<material> <amount>), but got 1: barrier",
                BlockItemRequest.parseLine("barrier", material -> true).getRejectionMessage().orElseThrow());
    }

    @Test
    @UseLanguage("de")
    void labelsTheGateBlocksInGerman() {
        RecordingItemStack gateBlocks = labelledGateBlocks(new StartGateDuration(1));

        assertEquals("Starttor-Block", gateBlocks.getName());
        assertEquals(List.of("Setze diese Blöcke als das Wandstück, das die", "Box um einen Team-Spawn schließt",
                "Jeder Rundenstart stellt sie wieder her und baut", "sie in 1 Sekunde von oben wieder ab",
                "Baust du einen gesetzten ab, gehört er nicht mehr zum Starttor"), gateBlocks.getLore());
        assertEquals("<Material> <Menge>", BlockItemRequest.describeUsage());
        assertEquals("Erwartet werden 2 durch Leerzeichen getrennte Werte (<Material> <Menge>), eingegeben: 1 "
                + "(barrier)", BlockItemRequest.parseLine("barrier", material -> true).getRejectionMessage()
                .orElseThrow());
    }

    @Test
    void recognizesTheGateBlocksByTheirMarkerInEitherLanguage() {
        StartGateItemService itemService = itemService(new StartGateDuration());
        RecordingItemStack english = labelledGateBlocks(new StartGateDuration());
        Messages.use(MessageCatalogLoader.shippedCatalog("de"));
        RecordingItemStack german = labelledGateBlocks(new StartGateDuration());

        assertEquals("Start gate block", english.getName());
        assertEquals("Starttor-Block", german.getName());
        assertTrue(itemService.isGateItem(english));
        assertTrue(itemService.isGateItem(german), "gate blocks handed out in German work the same");
        assertFalse(itemService.isGateItem(RecordingItemStack.named(Material.BARRIER, "Start gate block")),
                "a block that only carries the name is no gate block");
        assertFalse(itemService.isGateItem(RecordingItemStack.named(Material.BARRIER, "Starttor-Block")));
    }

    private static List<String> countdown() {
        return List.of(StartGateCountdown.describeTitle(3), StartGateCountdown.describeSubtitle(3),
                StartGateCountdown.describeTitle(StartGateCountdown.RELEASE_SECOND),
                StartGateCountdown.describeSubtitle(StartGateCountdown.RELEASE_SECOND));
    }

    private static List<String> durations() {
        return List.of(new StartGateDuration(1).describe(), new StartGateDuration(2).describe());
    }

    private List<String> describeTheGateWithZeroOneAndTwoBlocks() {
        StartGateItemService itemService = itemService(new StartGateDuration(1));
        List<String> lines = new ArrayList<>(itemService.describeGate());
        harness.setBlock(10, 65, 20, Material.BARRIER);
        itemService.markPlacedBlock(harness.blockAt(10, 65, 20));
        lines.addAll(itemService.describeGate());
        harness.setBlock(11, 65, 20, Material.BARRIER);
        itemService.markPlacedBlock(harness.blockAt(11, 65, 20));
        lines.addAll(itemService.describeGate());
        return lines;
    }

    private RecordingItemStack labelledGateBlocks(StartGateDuration duration) {
        return (RecordingItemStack) itemService(duration).labelled(new RecordingItemStack(Material.BARRIER, 32));
    }

    private StartGateItemService itemService(StartGateDuration duration) {
        return new StartGateItemService(harness.getPlugin(), new StartGateMarks(), duration, () -> { });
    }
}
