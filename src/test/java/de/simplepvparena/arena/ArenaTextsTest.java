package de.simplepvparena.arena;

import de.simplepvparena.input.ChatInputValues;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaTextsTest {

    private static final String WORLD_NAME = "world";

    @Test
    @UseLanguage("en")
    void namesTheSpawnsAndWhereToStandInEnglish() {
        assertEquals(List.of("first team spawn", "second team spawn", "tribune spawn"), displayNames());
        assertEquals(List.of("inside the border wall, where the first team starts",
                "inside the border wall, where the second team starts",
                "outside the border wall, where the spectators wait"), placementHints());
    }

    @Test
    @UseLanguage("de")
    void namesTheSpawnsAndWhereToStandInGerman() {
        assertEquals(List.of("Spawn des ersten Teams", "Spawn des zweiten Teams", "Tribünen-Spawn"), displayNames());
        assertEquals(List.of("innerhalb der Arenawand, wo das erste Team startet",
                "innerhalb der Arenawand, wo das zweite Team startet",
                "außerhalb der Arenawand, wo die Zuschauer warten"), placementHints());
    }

    @Test
    @UseLanguage("en")
    void namesTheTilesOfTheArenaMenuLikeTheConstantsOfTheOtherClasses() {
        assertEquals(Arena.CALCULATE_ACTION, Arena.describeArenaMenuTile(ArenaMenuMessage.CALCULATE_TITLE));
        assertEquals(Arena.DIAGNOSE_ACTION, Arena.describeArenaMenuTile(ArenaMenuMessage.DIAGNOSE_TITLE));
        assertEquals(Arena.TRIBUNE_SPAWN_ACTION, Arena.describeArenaMenuTile(ArenaMenuMessage.TRIBUNE_SPAWN_TITLE));
        assertEquals(Arena.MARK_HEIGHT_ACTION, Arena.describeArenaMenuTile(ArenaMenuMessage.HEIGHT_MARKER_TITLE));
        assertEquals(Arena.CLEAN_ACTION, Arena.describeArenaMenuTile(ArenaMenuMessage.CLEAN_TITLE));
    }

    @Test
    @UseLanguage("en")
    void refusesASpawnOnTheWrongSideInEnglish() {
        Arena arena = arenaWithFightArea();

        assertEquals("The first team spawn must lie inside the calculated fight area, world 50.5/64.0/50.5 lies "
                        + "outside of it\nStand inside the border wall, where the first team starts, then set it "
                        + "again. Nothing was stored",
                arena.setSpawn(ArenaSpawnType.FIRST_TEAM, position(50.5D, 50.5D)).orElseThrow());
        assertEquals("The tribune spawn must lie outside of the calculated fight area, world 1.5/64.0/1.5 lies "
                        + "inside it\nStand outside the border wall, where the spectators wait, then set it again. "
                        + "Nothing was stored",
                arena.setSpawn(ArenaSpawnType.TRIBUNE, position(1.5D, 1.5D)).orElseThrow());
    }

    @Test
    @UseLanguage("de")
    void refusesASpawnOnTheWrongSideInGerman() {
        Arena arena = arenaWithFightArea();

        assertEquals("Der Spawn des ersten Teams muss innerhalb der berechneten Kampffläche liegen, "
                        + "world 50.5/64.0/50.5 liegt außerhalb davon\nSteh innerhalb der Arenawand, wo das erste "
                        + "Team startet, dann setze ihn erneut. Es wurde nichts gespeichert",
                arena.setSpawn(ArenaSpawnType.FIRST_TEAM, position(50.5D, 50.5D)).orElseThrow());
        assertEquals("Der Spawn des zweiten Teams muss innerhalb der berechneten Kampffläche liegen, "
                        + "world 50.5/64.0/50.5 liegt außerhalb davon\nSteh innerhalb der Arenawand, wo das zweite "
                        + "Team startet, dann setze ihn erneut. Es wurde nichts gespeichert",
                arena.setSpawn(ArenaSpawnType.SECOND_TEAM, position(50.5D, 50.5D)).orElseThrow());
        assertEquals("Der Tribünen-Spawn muss außerhalb der berechneten Kampffläche liegen, world 1.5/64.0/1.5 "
                        + "liegt darin\nSteh außerhalb der Arenawand, wo die Zuschauer warten, dann setze ihn erneut. "
                        + "Es wurde nichts gespeichert",
                arena.setSpawn(ArenaSpawnType.TRIBUNE, position(1.5D, 1.5D)).orElseThrow());
    }

    @Test
    @UseLanguage("de")
    void warnsAboutStoredSpawnsOnTheWrongSideInGerman() {
        Arena arena = new Arena();
        arena.setSecondTeamSpawn(position(50.5D, 50.5D));
        arena.setTribuneSpawn(position(2.5D, 2.5D));
        arena.setShape(fightArea());

        assertEquals(List.of("Der Spawn des zweiten Teams muss innerhalb der berechneten Kampffläche liegen, "
                        + "world 50.5/64.0/50.5 liegt außerhalb davon\nSetze ihn erneut: Steh innerhalb der "
                        + "Arenawand, wo das zweite Team startet, dann klicke auf seine Kachel",
                "Der Tribünen-Spawn muss außerhalb der berechneten Kampffläche liegen, world 2.5/64.0/2.5 liegt "
                        + "darin\nSetze ihn erneut: Steh außerhalb der Arenawand, wo die Zuschauer warten, dann "
                        + "klicke auf seine Kachel"), arena.findSpawnPlacementWarnings());
    }

    @Test
    @UseLanguage("de")
    void namesTheMissingPartsOfTheArenaWithTheGermanTilesInGerman() {
        assertEquals(List.of("die berechnete Kampffläche (\"Kampffläche berechnen\" im Arena-Menü von /tournament)",
                        "der Tribünen-Spawn (\"Tribünen-Spawn setzen\" im Arena-Menü von /tournament)"),
                new Arena().getMissingConfiguration());
    }

    @Test
    @UseLanguage("de")
    void describesTheAreaOfEffectAndTheDistanceInGermanWithAPointInTheNumbers() {
        EffectArea area = EffectArea.around(EffectAreaTest.rectangle(0, 5, 0, 7), 10);

        assertEquals("Mitte 3/4, Radius 15 Blöcke", area.describe());
        assertEquals("Mitte 3.5/3.5, Radius 99.5 Blöcke",
                EffectArea.around(EffectAreaTest.rectangle(1, 5, 1, 5), 96).describe());
        assertEquals("27.5 Blöcke entfernt von seiner Mitte 3/4, Radius 15 Blöcke",
                area.describeDistanceOf("event", 30.5D, 4.0D));
        assertEquals("in der Welt lobby, während der Bereich in event liegt, mit seiner Mitte 3/4, Radius 15 Blöcke",
                area.describeDistanceOf("lobby", 3.0D, 4.0D));
    }

    @Test
    @UseLanguage("en")
    void writesOneBlockOfDistanceInTheSingularInEnglish() {
        EffectArea area = EffectArea.around(singleCell(), 0);

        assertEquals("1 block from its centre 0.5/0.5, radius 0.7 blocks",
                area.describeDistanceOf("event", 1.5D, 0.5D));
        assertEquals("2 blocks from its centre 0.5/0.5, radius 0.7 blocks",
                area.describeDistanceOf("event", 2.5D, 0.5D));
        assertEquals("1.5 blocks from its centre 0.5/0.5, radius 0.7 blocks",
                area.describeDistanceOf("event", 2.0D, 0.5D));
    }

    @Test
    @UseLanguage("de")
    void writesOneBlockOfDistanceInTheSingularInGerman() {
        EffectArea area = EffectArea.around(singleCell(), 0);

        assertEquals("1 Block entfernt von seiner Mitte 0.5/0.5, Radius 0.7 Blöcke",
                area.describeDistanceOf("event", 1.5D, 0.5D));
        assertEquals("2 Blöcke entfernt von seiner Mitte 0.5/0.5, Radius 0.7 Blöcke",
                area.describeDistanceOf("event", 2.5D, 0.5D));
    }

    @Test
    @UseLanguage("de")
    void namesATribuneOutsideOfTheAreaOfEffectInGerman() {
        assertEquals(Optional.of("Der Tribünen-Spawn event 30.5/64.0/4.0 liegt außerhalb des Wirkungsbereichs, "
                        + "27.5 Blöcke entfernt von seiner Mitte 3/4, Radius 15 Blöcke"),
                registryWithATribuneOutside().findTribuneOutsideOfTheEffectArea());
    }

    @Test
    @UseLanguage("en")
    void refusesAWrongCellLimitInEnglishWithOneValueInTheSingular() {
        assertEquals("Expected 1 value separated by spaces (<cells>), but got 2: 500 cells",
                ArenaDebugCellLimit.parseLine("500 cells").getRejectionMessage().orElseThrow());
        assertEquals("Nothing was typed, expected <cells>",
                ArenaDebugCellLimit.parseLine(" ").getRejectionMessage().orElseThrow());
        assertEquals("<cells>", ArenaDebugCellLimit.describeUsage());
    }

    @Test
    @UseLanguage("de")
    void refusesAWrongCellLimitInGerman() {
        assertEquals("Erwartet wird 1 Wert (<Zellenlimit>), eingegeben wurden 2: 500 Zellen",
                ArenaDebugCellLimit.parseLine("500 Zellen").getRejectionMessage().orElseThrow());
        assertEquals("Es wurde nichts eingegeben, erwartet wird <Zellenlimit>",
                ArenaDebugCellLimit.parseLine(" ").getRejectionMessage().orElseThrow());
        assertEquals("Das Zellenlimit viele ist keine Zahl",
                ArenaDebugCellLimit.parse("viele").getRejectionMessage().orElseThrow());
        assertEquals("Das Zellenlimit muss zwischen 1 und 5000000 liegen",
                ArenaDebugCellLimit.parse("0").getRejectionMessage().orElseThrow());
    }

    @Test
    @UseLanguage("de")
    void keepsTheLanguageARefusalWasCreatedIn() {
        ArenaDebugCellLimit germanLimit = ArenaDebugCellLimit.parse("viele");
        Optional<String> germanTribune = registryWithATribuneOutside().findTribuneOutsideOfTheEffectArea();

        Messages.use(MessageCatalogLoader.shippedCatalog("en"));

        assertEquals("Das Zellenlimit viele ist keine Zahl", germanLimit.getRejectionMessage().orElseThrow());
        assertEquals("Der Tribünen-Spawn event 30.5/64.0/4.0 liegt außerhalb des Wirkungsbereichs, "
                + "27.5 Blöcke entfernt von seiner Mitte 3/4, Radius 15 Blöcke", germanTribune.orElseThrow());
        assertEquals("The cell limit many is not a number",
                ArenaDebugCellLimit.parse("many").getRejectionMessage().orElseThrow());
    }

    @Test
    @UseLanguage("en")
    void countsTheExpectedValuesWithTheRightNumberInEnglish() {
        assertEquals("Expected 1 value separated by spaces (<name>), but got 2: a b",
                ChatInputValues.of("a b").findCountRejection(1, "<name>").orElseThrow());
        assertEquals("Expected 2 values separated by spaces (<name> <amount>), but got 1: a",
                ChatInputValues.of("a").findCountRejection(2, "<name> <amount>").orElseThrow());
    }

    @Test
    @UseLanguage("de")
    void countsTheExpectedValuesWithTheRightNumberInGerman() {
        assertEquals("Erwartet wird 1 Wert (<Name>), eingegeben wurden 2: a b",
                ChatInputValues.of("a b").findCountRejection(1, "<Name>").orElseThrow());
        assertEquals("Erwartet werden 2 durch Leerzeichen getrennte Werte (<Name> <Menge>), eingegeben wurden 3: "
                + "a b c", ChatInputValues.of("a b c").findCountRejection(2, "<Name> <Menge>").orElseThrow());
    }

    private static List<String> displayNames() {
        return List.of(ArenaSpawnType.values()).stream().map(ArenaSpawnType::getDisplayName).toList();
    }

    private static List<String> placementHints() {
        return List.of(ArenaSpawnType.values()).stream().map(ArenaSpawnType::getPlacementHint).toList();
    }

    private static ArenaRegistry registryWithATribuneOutside() {
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        arenaRegistry.getArena().setShape(EffectAreaTest.rectangle(0, 5, 0, 7));
        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot("event", 30.5D, 64.0D, 4.0D, 0F, 0F));
        arenaRegistry.setEffectMargin(10);
        return arenaRegistry;
    }

    private static Arena arenaWithFightArea() {
        Arena arena = new Arena();
        arena.setShape(fightArea());
        return arena;
    }

    private static PositionSnapshot position(double x, double z) {
        return new PositionSnapshot(WORLD_NAME, x, 64.0D, z, 0.0F, 0.0F);
    }

    private static ArenaShape fightArea() {
        return new ArenaShape(WORLD_NAME, 64, 70, new long[] {
                HorizontalCellCodec.encode(1, 1),
                HorizontalCellCodec.encode(2, 1),
                HorizontalCellCodec.encode(1, 2),
                HorizontalCellCodec.encode(2, 2)});
    }

    private static ArenaShape singleCell() {
        return new ArenaShape("event", 64, 67, new long[] {HorizontalCellCodec.encode(0, 0)});
    }
}
