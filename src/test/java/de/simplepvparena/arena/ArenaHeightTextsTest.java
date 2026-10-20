package de.simplepvparena.arena;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaHeightTextsTest {

    private static final String WORLD_NAME = "event";

    private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
    private final ArenaHeightMarkers markers = new ArenaHeightMarkers();
    private final ArenaHeightMarkerService markerService = new ArenaHeightMarkerService(harness.getPlugin(), markers);
    private final List<String> messages = new ArrayList<>();
    private final Player admin = recordingPlayer(messages);

    @Test
    @UseLanguage("en")
    void namesTheEndsAndDescribesTheMarksInEnglish() {
        assertEquals("lowest point", ArenaHeightMarkerMode.LOWEST.getDisplayName());
        assertEquals("highest point", ArenaHeightMarkerMode.HIGHEST.getDisplayName());
        assertEquals(List.of("No height is marked", "Lowest point: y 50, highest point: not marked",
                "Lowest point: not marked, highest point: y 80", "Lowest point: y 50, highest point: y 80"),
                describeEveryState());
    }

    @Test
    @UseLanguage("de")
    void namesTheEndsAndDescribesTheMarksInGerman() {
        assertEquals("tiefster Punkt", ArenaHeightMarkerMode.LOWEST.getDisplayName());
        assertEquals("höchster Punkt", ArenaHeightMarkerMode.HIGHEST.getDisplayName());
        assertEquals(List.of("Es ist keine Höhe markiert", "Tiefster Punkt: y 50, höchster Punkt: nicht markiert",
                "Tiefster Punkt: nicht markiert, höchster Punkt: y 80", "Tiefster Punkt: y 50, höchster Punkt: y 80"),
                describeEveryState());
    }

    @Test
    @UseLanguage("en")
    void explainsASingleMarkAndRefusesAWrongOrderInEnglish() {
        assertEquals(List.of("Only the lowest point of the fight area is marked, at y 50\n"
                        + "A single mark is not used, the area keeps the height of the border wall. Mark the highest "
                        + "point as well and calculate again",
                "Only the highest point of the fight area is marked, at y 80\n"
                        + "A single mark is not used, the area keeps the height of the border wall. Mark the lowest "
                        + "point as well and calculate again",
                "The lowest point has to lie below the highest point, which is marked at y 80, and y 90 does not\n"
                        + "Nothing was stored. Mark this end below y 80, or switch to the highest point and mark that "
                        + "one again first",
                "The highest point has to lie above the lowest point, which is marked at y 50, and y 40 does not\n"
                        + "Nothing was stored. Mark this end above y 50, or switch to the lowest point and mark that "
                        + "one again first"), explainEveryHintAndRefusal());
    }

    @Test
    @UseLanguage("de")
    void explainsASingleMarkAndRefusesAWrongOrderInGerman() {
        assertEquals(List.of("Nur der tiefste Punkt der Kampffläche ist markiert, auf y 50\n"
                        + "Eine einzelne Markierung wird nicht verwendet, die Fläche behält die Höhe der Arenawand. "
                        + "Markiere auch den höchsten Punkt und berechne erneut",
                "Nur der höchste Punkt der Kampffläche ist markiert, auf y 80\n"
                        + "Eine einzelne Markierung wird nicht verwendet, die Fläche behält die Höhe der Arenawand. "
                        + "Markiere auch den tiefsten Punkt und berechne erneut",
                "Der tiefste Punkt muss unter dem höchsten Punkt liegen, der auf y 80 markiert ist, und y 90 tut das "
                        + "nicht\n"
                        + "Es wurde nichts gespeichert. Markiere dieses Ende unter y 80, oder wechsle zum "
                        + "höchsten Punkt und markiere zuerst diesen erneut",
                "Der höchste Punkt muss über dem tiefsten Punkt liegen, der auf y 50 markiert ist, und y 40 tut das "
                        + "nicht\n"
                        + "Es wurde nichts gespeichert. Markiere dieses Ende über y 50, oder wechsle zum "
                        + "tiefsten Punkt und markiere zuerst diesen erneut"), explainEveryHintAndRefusal());
    }

    @Test
    @UseLanguage("en")
    void reportsBothMarksInEnglish() {
        markerService.markHeight(admin, 50);
        markerService.markHeight(admin, 80);

        assertEquals(List.of("The lowest point of the fight area is marked at y 50\n"
                        + "Lowest point: y 50, highest point: not marked",
                "The marker now marks the highest point, it moved on by itself. Place a block with it at that "
                        + "height as well\nA single mark changes nothing about a calculation",
                "The highest point of the fight area is marked at y 80\nLowest point: y 50, highest point: y 80",
                "Both heights are set. The next \"Calculate the fight area\" in the arena menu of /tournament takes "
                        + "y 50 and y 80 over as the floor and the ceiling of the fight area instead of the height of "
                        + "the border wall\nThey are used up by that calculation and are gone when the server "
                        + "restarts"), messages);
    }

    @Test
    @UseLanguage("de")
    void reportsBothMarksInGerman() {
        markerService.markHeight(admin, 50);
        markerService.markHeight(admin, 80);

        assertEquals(List.of("Der tiefste Punkt der Kampffläche ist auf y 50 markiert\n"
                        + "Tiefster Punkt: y 50, höchster Punkt: nicht markiert",
                "Die Höhenmarke markiert jetzt den höchsten Punkt, sie ist von selbst weitergewechselt. Setze damit "
                        + "auch auf dieser Höhe einen Block\n"
                        + "Eine einzelne Markierung ändert nichts an einer Berechnung",
                "Der höchste Punkt der Kampffläche ist auf y 80 markiert\nTiefster Punkt: y 50, höchster Punkt: y 80",
                "Beide Höhen sind gesetzt. Die nächste Berechnung mit \"Kampffläche berechnen\" im Arena-Menü von "
                        + "/tournament übernimmt y 50 und y 80 als Boden und Decke der Kampffläche statt der Höhe der "
                        + "Arenawand\nDiese Berechnung verbraucht sie, und sie sind weg, wenn der Server neu startet"),
                messages);
    }

    @Test
    @UseLanguage("en")
    void switchesTheEndAndLabelsTheItemInEnglish() {
        RecordingItemStack highest = switchEnd();
        RecordingItemStack lowest = switchEnd();

        assertEquals(List.of("The marker now marks the highest point of the fight area\n"
                        + "Place a block with it at that height, it becomes the ceiling of the fight area and "
                        + "disappears again\nNo height is marked",
                "The marker now marks the lowest point of the fight area\n"
                        + "Place a block with it at that height, it becomes the floor of the fight area and disappears "
                        + "again\nNo height is marked"), messages);
        assertEquals("Height marker: highest point", highest.getName());
        assertEquals(List.of("Marks the ceiling of the fight area",
                "Place it at that height, the block disappears again",
                "It moves on to the lowest point by itself once this end is marked",
                "Right click in the air marks an end again", "Which end is marked is kept by the plugin, this",
                "name only shows it. Both ends are taken over by", "the next calculation"), highest.getLore());
        assertEquals("Height marker: lowest point", lowest.getName());
        assertEquals(List.of("Marks the floor of the fight area", "Place it at that height, the block disappears again",
                "It moves on to the highest point by itself once this end is marked",
                "Right click in the air marks an end again", "Which end is marked is kept by the plugin, this",
                "name only shows it. Both ends are taken over by", "the next calculation"), lowest.getLore());
    }

    @Test
    @UseLanguage("de")
    void switchesTheEndAndLabelsTheItemInGerman() {
        RecordingItemStack highest = switchEnd();
        RecordingItemStack lowest = switchEnd();

        assertEquals(List.of("Die Höhenmarke markiert jetzt den höchsten Punkt der Kampffläche\n"
                        + "Setze damit auf dieser Höhe einen Block, er wird zur Decke der Kampffläche und verschwindet "
                        + "wieder\nEs ist keine Höhe markiert",
                "Die Höhenmarke markiert jetzt den tiefsten Punkt der Kampffläche\n"
                        + "Setze damit auf dieser Höhe einen Block, er wird zum Boden der Kampffläche und verschwindet "
                        + "wieder\nEs ist keine Höhe markiert"), messages);
        assertEquals("Höhenmarke: höchster Punkt", highest.getName());
        assertEquals(List.of("Markiert die Decke der Kampffläche", "Setze sie auf dieser Höhe, der Block verschwindet "
                        + "wieder", "Sie wechselt von selbst zum tiefsten Punkt, sobald dieses Ende markiert ist",
                "Ein Rechtsklick in die Luft wechselt das Ende, das du markierst",
                "Welches Ende markiert wird, merkt sich das Plugin,", "der Name zeigt es nur an. Beide Enden übernimmt",
                "die nächste Berechnung"), highest.getLore());
        assertEquals("Höhenmarke: tiefster Punkt", lowest.getName());
        assertEquals(List.of("Markiert den Boden der Kampffläche", "Setze sie auf dieser Höhe, der Block verschwindet "
                        + "wieder", "Sie wechselt von selbst zum höchsten Punkt, sobald dieses Ende markiert ist",
                "Ein Rechtsklick in die Luft wechselt das Ende, das du markierst",
                "Welches Ende markiert wird, merkt sich das Plugin,", "der Name zeigt es nur an. Beide Enden übernimmt",
                "die nächste Berechnung"), lowest.getLore());
    }

    @Test
    @UseLanguage("en")
    void describesTheMarksOnTheTileInEnglish() {
        List<String> beforeMarking = markerService.describeMarkers();
        markers.mark(50);
        markers.mark(80);

        assertEquals(List.of("No height is marked", "The next placed block marks the lowest point",
                "Both ends have to be marked, a single mark is not used"), beforeMarking);
        assertEquals(List.of("Lowest point: y 50, highest point: y 80", "The next placed block marks the highest point",
                "The next calculation takes them as floor and ceiling"), markerService.describeMarkers());
    }

    @Test
    @UseLanguage("de")
    void describesTheMarksOnTheTileInGerman() {
        List<String> beforeMarking = markerService.describeMarkers();
        markers.mark(50);
        markers.mark(80);

        assertEquals(List.of("Es ist keine Höhe markiert", "Der nächste gesetzte Block markiert den tiefsten Punkt",
                "Beide Enden müssen markiert sein, eine einzelne Markierung wird nicht verwendet"), beforeMarking);
        assertEquals(List.of("Tiefster Punkt: y 50, höchster Punkt: y 80",
                "Der nächste gesetzte Block markiert den höchsten Punkt",
                "Die nächste Berechnung übernimmt sie als Boden und Decke"), markerService.describeMarkers());
    }

    private List<String> describeEveryState() {
        List<String> descriptions = new ArrayList<>();
        descriptions.add(markers.describe());
        markers.mark(50);
        descriptions.add(markers.describe());
        ArenaHeightMarkers onlyHighest = new ArenaHeightMarkers();
        onlyHighest.switchNextEnd();
        onlyHighest.mark(80);
        descriptions.add(onlyHighest.describe());
        markers.mark(80);
        descriptions.add(markers.describe());
        return descriptions;
    }

    private List<String> explainEveryHintAndRefusal() {
        List<String> texts = new ArrayList<>();
        ArenaHeightMarkers onlyLowest = new ArenaHeightMarkers();
        onlyLowest.mark(50);
        texts.add(onlyLowest.findIncompleteHint().orElseThrow());
        ArenaHeightMarkers onlyHighest = new ArenaHeightMarkers();
        onlyHighest.switchNextEnd();
        onlyHighest.mark(80);
        texts.add(onlyHighest.findIncompleteHint().orElseThrow());
        texts.add(onlyHighest.mark(90).getRejectionMessage().orElseThrow());
        texts.add(onlyLowest.mark(40).getRejectionMessage().orElseThrow());
        return texts;
    }

    private RecordingItemStack switchEnd() {
        return (RecordingItemStack) markerService.switchMarkedEnd(admin,
                new RecordingItemStack(ArenaHeightMarkerService.MARKER_MATERIAL,
                        ArenaHeightMarkerService.MARKER_AMOUNT));
    }

    private static Player recordingPlayer(List<String> messages) {
        UUID playerId = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxyInstance, method, arguments) -> switch (method.getName()) {
                    case "sendMessage" -> {
                        messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                        yield null;
                    }
                    case "getUniqueId" -> playerId;
                    case "getName" -> "admin";
                    case "isOnline" -> true;
                    case "hasPermission" -> true;
                    case "toString" -> "recording player";
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "equals" -> proxyInstance == arguments[0];
                    default -> throw new UnsupportedOperationException("Player." + method.getName());
                });
    }
}
