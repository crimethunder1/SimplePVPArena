package de.simplepvparena.config;

import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.ConfigMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.PluginMessage;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanConfigTextsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void reportsEveryBrokenEntryInGerman() {
        PluginSettings.read(ConfigTextsTest.configurationWithEveryValueBroken(), problems::add);

        assertEquals(List.of(
                "match.round-timeout-seconds muss mindestens eine Sekunde sein, stattdessen gelten 300 Sekunden",
                "match.start-gate-duration-seconds muss mindestens 1 Sekunde sein, stattdessen gelten 10 Sekunden",
                "match.team-colours.first-team ist keine Farbe, in der ein Name gezeigt werden kann, stattdessen gilt "
                        + "red. Nutzbare Farben: " + Messages.join(TeamColourSettings.knownColourNames()),
                "match.team-size muss eine ganze Zahl zwischen 1 und 8 Spielern sein, stattdessen gelten 2 Spieler",
                "arena.baseline-depth-below-floor muss zwischen 0 und 64 Blöcken liegen, stattdessen gelten 11 Blöcke",
                "arena.clean-particle.total-duration-seconds darf nicht negativ sein, stattdessen gelten 6.0 "
                        + "Sekunden. Mit null reinigst du ohne die beiden farbigen Phasen",
                "arena.clean-particle.columns-per-tick darf nicht negativ sein, stattdessen gelten 24 Spalten pro "
                        + "Tick. Mit null wird keine gezeichnet",
                "arena.shape-maximum-cells muss mindestens eine Zelle sein, stattdessen gilt 50000",
                "arena.safety-zone-radius darf nicht negativ sein, stattdessen gelten 64 Blöcke. Mit null schaltest "
                        + "du die Schutzzone ab",
                "arena.effect-margin muss eine ganze Zahl zwischen 0 und 512 Blöcken sein, stattdessen gelten 96 "
                        + "Blöcke",
                "border.confirmed-particle.height darf nicht negativ sein, stattdessen gelten 0.25 Blöcke. Mit null "
                        + "wird die Markierung so kurz wie möglich",
                "language fr ist keine mitgelieferte Sprache, stattdessen gilt en. Mitgelieferte Sprachen: en und de"),
                problems);
    }

    @Test
    void reportsTwoTeamsInTheSameColourInGerman() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.FIRST_TEAM_COLOUR_PATH, "gold");
        configuration.set(PluginSettings.SECOND_TEAM_COLOUR_PATH, "gold");

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("match.team-colours gibt beiden Teams die Farbe gold, das wäre gar keine Färbung, "
                + "stattdessen gelten red und aqua"), problems);
    }

    @Test
    void writesTheReloadAnswerInGerman() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -1);
        ConfigurationReloadService reloadService = new ConfigurationReloadService(() -> configuration, () -> { },
                new RecordingLiveConfiguration(), problem -> { });

        assertEquals(List.of("config.yml wurde neu geladen und angewendet",
                "  die Sprache und die Sprachdateien im Ordner lang, für jeden Text, der ab jetzt gezeigt wird",
                "  die Wartezeit bis zum Zurücksetzen nach einer Runde, das Rundenzeitlimit, der Schaden im Sudden "
                        + "Death und ob das Finale ohne Zeitlimit ausgetragen wird",
                "  die Sekunden, die das Starttor zu Beginn einer Runde braucht",
                "  die Sekunden des Countdowns direkt vor dem Kampf, ab der nächsten Runde",
                "  die Sekunden, die ein Kämpfer nach einem Verbindungsabbruch zum Zurückkommen hat, für jeden "
                        + "Verbindungsabbruch ab jetzt",
                "  ob das größere Team abstimmt, wer kämpft, wenn die Teams unterschiedlich viele Spieler online "
                        + "haben, und die Sekunden der Abstimmung, ab dem nächsten Match",
                "  die Teamgröße, außer ein Turnier läuft oder ein Team hat mehr Spieler als eine kleinere neue Größe",
                "  ob der Auto-Match-Modus an ist, die Sekunden bis zum Beginn eines Auto-Matches und die Pause "
                        + "zwischen zwei Auto-Matches",
                "  ob die Spieler nach der Anmeldung ihr Team wählen oder die Teams zufällig gebildet werden",
                "  die Blockmengen der Reinigung und des Ausgangszustands und die Partikel einer laufenden Reinigung",
                "  die Tiefe unter dem Boden, bis zu der der nächste Ausgangszustand aufgenommen wird",
                "  das Zellenlimit der Arena-Berechnung",
                "  der Radius der Schutzzone",
                "  der Rand des Wirkungsbereichs um die Kampffläche",
                "  ob Spieler, die dem Server beitreten, auf die Tribüne geschickt werden",
                "  ob Spieler im Wirkungsbereich ihr Inventar abgeben, mit false bekommt jeder sein Inventar sofort "
                        + "zurück",
                "  die Wandpartikel, ihre Grenzen und die Markierung der bestätigten Arenawand",
                "  die IDs der Bilder und Items aus ItemsAdder und ob sie genutzt werden, ein Wechsel zwischen "
                        + "ItemsAdder und den eigenen Anzeigen des Plugins braucht einen Neustart",
                "  ob die Platzierung als Seitenleiste im Wirkungsbereich und als Tafel an der Tribüne gezeigt wird",
                "Die berechnete Kampffläche, die Spawns, die Wandblöcke, die Teams und ein laufendes Match bleiben "
                        + "unberührt",
                "Einträge, die nicht genutzt werden konnten und auf ihren Standardwert zurückgefallen sind:",
                "  arena.safety-zone-radius darf nicht negativ sein, stattdessen gelten 64 Blöcke. Mit null schaltest "
                        + "du die Schutzzone ab"), reloadService.reloadFromFile().describe());
    }

    @Test
    void warnsAboutATeamSizeThatIsNotTakenOverInGerman() {
        TeamRegistry teamRegistry = new TeamRegistry();
        String rejection = TeamSizeSetting.findChangeRejection(3, 5, true, teamRegistry).orElseThrow();

        assertEquals("Die Teamgröße lässt sich nicht ändern, solange ein Turnier läuft. Die Teamgröße 5 aus "
                        + "match.team-size in config.yml wurde nicht übernommen, die Teamgröße bleibt 3 (3 gegen 3)",
                Messages.text(ConfigMessage.TEAM_SIZE_NOT_TAKEN_OVER, Placeholder.of("rejection", rejection),
                        Placeholder.of("teamSize", 5), Placeholder.of("path", PluginSettings.TEAM_SIZE_PATH),
                        Placeholder.of("matchUp", TeamSizeSetting.describeMatchUp(3))));
    }

    @Test
    void saysThatTheLargerTeamVotesInTheModeVoteInGerman() {
        Placeholder path = Placeholder.of("path", PluginSettings.TEAM_SIZE_PATH);
        Placeholder teamSize = Placeholder.of("teamSize", 2);

        assertEquals("x. Die Teamgröße 2 aus match.team-size in config.yml gilt seit dem Start trotzdem, dieses "
                        + "Team behält seine Spieler und das größere Team stimmt ab, wer kämpft",
                Messages.plural(ConfigMessage.LARGER_TEAMS_AT_START_VOTING, 1, Placeholder.of("teams", "x"),
                        teamSize, path));
        assertEquals("x. Die Teamgröße 2 aus match.team-size in config.yml gilt seit dem Start trotzdem, diese "
                        + "Teams behalten ihre Spieler und das größere Team stimmt ab, wer kämpft",
                Messages.plural(ConfigMessage.LARGER_TEAMS_AT_START_VOTING, 2, Placeholder.of("teams", "x"),
                        teamSize, path));
    }

    @Test
    void warnsAboutLargerTeamsAtTheStartInTheSingularAndThePluralInGerman() {
        TeamRegistry teamRegistry = new TeamRegistry();
        registerTeam(teamRegistry, "Bravo", 3);
        Placeholder path = Placeholder.of("path", PluginSettings.TEAM_SIZE_PATH);
        Placeholder teamSize = Placeholder.of("teamSize", 2);

        assertEquals("1 Team hat mehr als 2 Spieler: Bravo (3). Die Teamgröße 2 aus match.team-size in config.yml "
                        + "gilt seit dem Start trotzdem, dieses Team behält seine Spieler und jeder Spieler davon, "
                        + "der online ist, kämpft",
                Messages.plural(ConfigMessage.LARGER_TEAMS_AT_START, 1, Placeholder.of("teams",
                        TeamSizeSetting.describeTeamsLargerThan(2, teamRegistry).orElseThrow()), teamSize, path));

        registerTeam(teamRegistry, "Charlie", 4);

        assertEquals("2 Teams haben mehr als 2 Spieler: Bravo (3) und Charlie (4). Die Teamgröße 2 aus "
                        + "match.team-size in config.yml gilt seit dem Start trotzdem, diese Teams behalten ihre "
                        + "Spieler und jeder Spieler davon, der online ist, kämpft",
                Messages.plural(ConfigMessage.LARGER_TEAMS_AT_START, 2, Placeholder.of("teams",
                        TeamSizeSetting.describeTeamsLargerThan(2, teamRegistry).orElseThrow()), teamSize, path));
    }

    @Test
    void warnsAboutATribuneOutsideOfTheEffectAreaInGerman() {
        String warning = Messages.text(ArenaSetupMessage.TRIBUNE_OUTSIDE_OF_THE_EFFECT_AREA,
                Placeholder.of("position", "event 30.5/64.0/4.0"),
                Placeholder.of("distance", "27.5 Blöcke von seiner Mitte 3/4, Radius 15 Blöcke"));

        assertEquals(warning + ". Es wurde nichts verworfen, setze den Tribünen-Spawn näher an der Kampffläche neu "
                        + "oder erhöhe arena.effect-margin in config.yml",
                Messages.text(ConfigMessage.TRIBUNE_OUTSIDE_OF_THE_EFFECT_AREA, Placeholder.of("warning", warning),
                        Placeholder.of("path", EffectArea.MARGIN_CONFIGURATION_PATH)));
    }

    @Test
    void writesAFallbackOfOneInTheSingularInGerman() {
        Placeholder path = Placeholder.of("path", "some.entry");
        Placeholder minimum = Placeholder.of("minimum", 0);
        Placeholder maximum = Placeholder.of("maximum", 9);

        assertEquals("some.entry muss zwischen 0 und 9 Blöcken liegen, stattdessen gilt 1 Block",
                Messages.plural(ConfigMessage.BASELINE_DEPTH_OUT_OF_RANGE, 1, path, maximum));
        assertEquals("some.entry darf nicht negativ sein, stattdessen gilt 1 Block. Mit null schaltest du die "
                + "Schutzzone ab", Messages.plural(ConfigMessage.SAFETY_ZONE_RADIUS_NEGATIVE, 1, path));
        assertEquals("some.entry muss eine ganze Zahl zwischen 0 und 9 Blöcken sein, stattdessen gilt 1 Block",
                Messages.plural(ConfigMessage.EFFECT_MARGIN_OUT_OF_RANGE, 1, path, minimum, maximum));
        assertEquals("some.entry muss eine ganze Zahl zwischen 0 und 9 Spielern sein, stattdessen gilt 1 Spieler",
                Messages.plural(ConfigMessage.TEAM_SIZE_OUT_OF_RANGE, 1, path, minimum, maximum));
        assertEquals("some.entry darf nicht negativ sein, stattdessen gilt 1 Spalte pro Tick. Mit null wird keine "
                + "gezeichnet", Messages.plural(ConfigMessage.CLEANING_COLUMNS_NEGATIVE, 1, path));
        assertEquals("some.entry muss mindestens 1 Sekunde sein, stattdessen gilt 1 Sekunde",
                Messages.plural(ConfigMessage.START_GATE_DURATION_TOO_SHORT, 1, path, Placeholder.of("minimum", 1)));
        assertEquals("some.entry muss mindestens eine Sekunde sein, stattdessen gilt 1 Sekunde",
                Messages.plural(ConfigMessage.ROUND_TIMEOUT_TOO_SHORT, 1, path));
    }

    @Test
    void tellsTheAdminsThatTheKitsCouldNotBeLoadedInGerman() {
        assertEquals("Die Kits konnten nicht geladen werden (the plugin folder cannot be read), es gibt kein Kit, bis "
                        + "im Kit-Menü von /tournament eines angelegt wird",
                Messages.text(PluginMessage.KITS_NOT_LOADED,
                        Placeholder.of("reason", "the plugin folder cannot be read")));
    }

    private static void registerTeam(TeamRegistry teamRegistry, String teamName, int memberCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int memberNumber = 1; memberNumber <= memberCount; memberNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), teamName + memberNumber));
        }
        teamRegistry.registerTeam(teamName, members);
    }
}
