package de.simplepvparena.team;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamTextsTest {

    private static final List<String> SIX_TEAM_NAMES = List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo",
            "Foxtrot");

    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final UUID strangerUniqueId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @UseLanguage("en")
    void everyRefusalOfATeamAndTheRegistryReadsInEnglish() {
        assertEquals(List.of(
                "a team has 1 to 8 players",
                "a team cannot hold the same player twice",
                "a team named ALPHA already exists",
                "a team named ALPHA already exists",
                "PlayerOne is already registered in team Alpha",
                "team Octet already has 8 players, a team has at most 8 players",
                "the player 00000000-0000-0000-0000-000000000001 is not in team Alpha",
                "a team keeps at least 1 player, remove the team instead",
                "no team named Delta is registered",
                "A team needs a name, a team name has 1 to 16 letters, digits, hyphens or underscores",
                "The team name Bravo Team is not allowed, a team name has 1 to 16 letters, digits, hyphens or "
                        + "underscores"), collectRefusals());
        assertEquals("1 to 16 letters, digits, hyphens or underscores", TeamRegistry.describeNameRule());
    }

    @Test
    @UseLanguage("de")
    void everyRefusalOfATeamAndTheRegistryReadsInGerman() {
        assertEquals(List.of(
                "ein Team hat 1 bis 8 Spieler",
                "ein Team kann denselben Spieler nicht zweimal enthalten",
                "ein Team namens ALPHA gibt es bereits",
                "ein Team namens ALPHA gibt es bereits",
                "PlayerOne ist bereits in Team Alpha registriert",
                "Team Octet hat bereits 8 Spieler, ein Team hat höchstens 8 Spieler",
                "der Spieler 00000000-0000-0000-0000-000000000001 ist nicht in Team Alpha",
                "ein Team behält mindestens 1 Spieler, entferne stattdessen das Team",
                "kein Team namens Delta ist registriert",
                "Ein Team braucht einen Namen, ein Teamname hat 1 bis 16 Buchstaben, Ziffern, Bindestriche oder "
                        + "Unterstriche",
                "Der Teamname Bravo Team ist nicht erlaubt, ein Teamname hat 1 bis 16 Buchstaben, Ziffern, "
                        + "Bindestriche oder Unterstriche"), collectRefusals());
        assertEquals("1 bis 16 Buchstaben, Ziffern, Bindestriche oder Unterstriche",
                TeamRegistry.describeNameRule());
    }

    @Test
    @UseLanguage("en")
    void theRefusalsOfATeamLineReadInEnglish() {
        assertEquals(List.of(
                "Nothing was typed, expected <teamName> <player> [<player> ...]",
                "Expected 2 to 3 values separated by spaces (<teamName> <player> [<player> ...]), but got 1: Alpha",
                "A team needs 1 to 2 different players, Steve was named twice"), collectTeamLineRefusals());
        assertEquals("<teamName> <player> [<player> ...]", TeamCreationRequest.describeUsage());
    }

    @Test
    @UseLanguage("de")
    void theRefusalsOfATeamLineReadInGermanWithTheGermanUsage() {
        assertEquals(List.of(
                "Es wurde nichts eingegeben, erwartet wird <Teamname> <Spieler> [<Spieler> ...]",
                "Erwartet werden 2 bis 3 durch Leerzeichen getrennte Werte (<Teamname> <Spieler> [<Spieler> ...]), "
                        + "eingegeben wurden 1: Alpha",
                "Ein Team braucht 1 bis 2 verschiedene Spieler, Steve wurde zweimal genannt"),
                collectTeamLineRefusals());
        assertEquals("<Teamname> <Spieler> [<Spieler> ...]", TeamCreationRequest.describeUsage());
    }

    @Test
    @UseLanguage("de")
    void aWrongTeamSizeHandedOverByTheCodeStaysAnEnglishProgrammingError() {
        IllegalArgumentException settingRefusal = assertThrows(IllegalArgumentException.class,
                () -> new TeamSizeSetting().setTeamSize(9));
        IllegalArgumentException parseRefusal = assertThrows(IllegalArgumentException.class,
                () -> TeamCreationRequest.parse("Alpha Steve", 0));

        assertEquals("The team size must lie between 1 and 8 players, not 9", settingRefusal.getMessage());
        assertEquals("The team size must lie between 1 and 8 players, not 0", parseRefusal.getMessage());
    }

    @Test
    @UseLanguage("en")
    void theTeamSizeTextsReadInEnglish() {
        registerTeam("Alpha", 3);

        assertEquals(List.of(
                "2 (2 versus 2)",
                "The team size must lie between 1 and 8 players",
                "The team size cannot be changed while a tournament is in progress"), collectTeamSizeTexts());
    }

    @Test
    @UseLanguage("de")
    void theTeamSizeTextsReadInGerman() {
        registerTeam("Alpha", 3);

        assertEquals(List.of(
                "2 (2 gegen 2)",
                "Die Teamgröße muss zwischen 1 und 8 Spielern liegen",
                "Die Teamgröße lässt sich nicht ändern, solange ein Turnier läuft"), collectTeamSizeTexts());
    }

    @Test
    @UseLanguage("en")
    void theLargerTeamsAreListedWithAndAndCountedBeyondFiveInEnglish() {
        assertEquals(List.of(
                "1 team has more than 2 players: Alpha (3)",
                "2 teams have more than 2 players: Alpha (3) and Bravo (3)",
                "5 teams have more than 2 players: Alpha (3), Bravo (3), Charlie (3), Delta (3) and Echo (3)",
                "6 teams have more than 2 players: Alpha (3), Bravo (3), Charlie (3), Delta (3), Echo (3) and 1 more"),
                describeLargerTeamsOneByOne());
    }

    @Test
    @UseLanguage("de")
    void theLargerTeamsAreListedWithUndAndCountedBeyondFiveInGerman() {
        assertEquals(List.of(
                "1 Team hat mehr als 2 Spieler: Alpha (3)",
                "2 Teams haben mehr als 2 Spieler: Alpha (3) und Bravo (3)",
                "5 Teams haben mehr als 2 Spieler: Alpha (3), Bravo (3), Charlie (3), Delta (3) und Echo (3)",
                "6 Teams haben mehr als 2 Spieler: Alpha (3), Bravo (3), Charlie (3), Delta (3), Echo (3) und 1 "
                        + "weitere"), describeLargerTeamsOneByOne());
    }

    @Test
    @UseLanguage("en")
    void theRefusedTeamSizeNamesOneOrTwoTeamsWithTheirPlayersInEnglish() {
        registerTeam("Alpha", 3);
        Optional<String> oneTeam = TeamSizeSetting.findChangeRejection(3, 2, false, teamRegistry);
        registerTeam("Bravo", 2);
        Optional<String> twoTeams = TeamSizeSetting.findChangeRejection(3, 1, false, teamRegistry);

        assertEquals(Optional.of("1 team has more than 2 players: Alpha (Alpha1, Alpha2, Alpha3). Remove it first"),
                oneTeam);
        assertEquals(Optional.of("2 teams have more than 1 player: Alpha (Alpha1, Alpha2, Alpha3) and "
                + "Bravo (Bravo1, Bravo2). Remove them first"), twoTeams);
    }

    @Test
    @UseLanguage("de")
    void theRefusedTeamSizeNamesOneOrTwoTeamsWithTheirPlayersInGerman() {
        registerTeam("Alpha", 3);
        Optional<String> oneTeam = TeamSizeSetting.findChangeRejection(3, 2, false, teamRegistry);
        registerTeam("Bravo", 2);
        Optional<String> twoTeams = TeamSizeSetting.findChangeRejection(3, 1, false, teamRegistry);

        assertEquals(Optional.of("1 Team hat mehr als 2 Spieler: Alpha (Alpha1, Alpha2, Alpha3). Entferne es zuerst"),
                oneTeam);
        assertEquals(Optional.of("2 Teams haben mehr als 1 Spieler: Alpha (Alpha1, Alpha2, Alpha3) und "
                + "Bravo (Bravo1, Bravo2). Entferne sie zuerst"), twoTeams);
    }

    @Test
    @UseLanguage("en")
    void theSmallerTeamsTakeTheSingularAndThePluralInEnglish() {
        registerTeam("Alpha", 1);
        Optional<String> oneTeam = TeamSizeSetting.describeTeamsSmallerThan(2, teamRegistry);
        registerTeam("Bravo", 2);
        Optional<String> twoTeams = TeamSizeSetting.describeTeamsSmallerThan(3, teamRegistry);

        assertEquals(Optional.of("1 team has fewer than 2 players: Alpha (1)"), oneTeam);
        assertEquals(Optional.of("2 teams have fewer than 3 players: Alpha (1) and Bravo (2)"), twoTeams);
    }

    @Test
    @UseLanguage("de")
    void theSmallerTeamsTakeTheSingularAndThePluralInGerman() {
        registerTeam("Alpha", 1);
        Optional<String> oneTeam = TeamSizeSetting.describeTeamsSmallerThan(2, teamRegistry);
        registerTeam("Bravo", 2);
        Optional<String> twoTeams = TeamSizeSetting.describeTeamsSmallerThan(3, teamRegistry);

        assertEquals(Optional.of("1 Team hat weniger als 2 Spieler: Alpha (1)"), oneTeam);
        assertEquals(Optional.of("2 Teams haben weniger als 3 Spieler: Alpha (1) und Bravo (2)"), twoTeams);
    }

    @Test
    @UseLanguage("en")
    void teamsLargerThanOnePlayerTakeTheSingularPlayerInEnglish() {
        registerTeam("Alpha", 2);
        Optional<String> oneTeam = TeamSizeSetting.describeTeamsLargerThan(1, teamRegistry);
        registerTeam("Bravo", 2);
        Optional<String> twoTeams = TeamSizeSetting.describeTeamsLargerThan(1, teamRegistry);

        assertEquals(Optional.of("1 team has more than 1 player: Alpha (2)"), oneTeam);
        assertEquals(Optional.of("2 teams have more than 1 player: Alpha (2) and Bravo (2)"), twoTeams);
    }

    @Test
    @UseLanguage("de")
    void teamsLargerThanOnePlayerReadInGerman() {
        registerTeam("Alpha", 2);
        Optional<String> oneTeam = TeamSizeSetting.describeTeamsLargerThan(1, teamRegistry);
        registerTeam("Bravo", 2);
        Optional<String> twoTeams = TeamSizeSetting.describeTeamsLargerThan(1, teamRegistry);

        assertEquals(Optional.of("1 Team hat mehr als 1 Spieler: Alpha (2)"), oneTeam);
        assertEquals(Optional.of("2 Teams haben mehr als 1 Spieler: Alpha (2) und Bravo (2)"), twoTeams);
    }

    private List<String> collectRefusals() {
        List<String> refusals = new ArrayList<>();
        Team alpha = registerTeam("Alpha", 2);
        TeamMember firstOfAlpha = alpha.getMembers().get(0);
        Team bravo = registerTeam("Bravo", 1);
        TeamMember stranger = new TeamMember(strangerUniqueId, "Stranger");
        registerTeam("Octet", 8);
        refusals.add(refusalOf(() -> new Team("Empty", List.of())));
        refusals.add(refusalOf(() -> new Team("Twice", List.of(stranger, stranger))));
        refusals.add(refusalOf(() -> teamRegistry.registerTeam("ALPHA", List.of(stranger))));
        refusals.add(refusalOf(() -> teamRegistry.renameTeam("Bravo", "ALPHA")));
        refusals.add(refusalOf(() -> teamRegistry.addMember("Bravo",
                new TeamMember(firstOfAlpha.uniqueId(), "PlayerOne"))));
        refusals.add(refusalOf(() -> teamRegistry.addMember("Octet", stranger)));
        refusals.add(refusalOf(() -> teamRegistry.removeMember("Alpha", strangerUniqueId)));
        refusals.add(refusalOf(() -> teamRegistry.removeMember("Bravo",
                bravo.getMembers().get(0).uniqueId())));
        refusals.add(refusalOf(() -> teamRegistry.addMember("Delta", stranger)));
        refusals.add(refusalOf(() -> teamRegistry.registerTeam(" ", List.of(stranger))));
        refusals.add(refusalOf(() -> teamRegistry.registerTeam("Bravo Team", List.of(stranger))));
        return refusals;
    }

    private static String refusalOf(Supplier<Object> refusedAction) {
        return assertThrows(RuntimeException.class, refusedAction::get).getMessage();
    }

    private static List<String> collectTeamLineRefusals() {
        List<String> refusals = new ArrayList<>();
        for (String line : List.of("", "Alpha", "Alpha Steve steve")) {
            refusals.add(TeamCreationRequest.parse(line, 2).getRejectionMessage().orElseThrow());
        }
        return refusals;
    }

    private List<String> collectTeamSizeTexts() {
        return List.of(
                TeamSizeSetting.describeMatchUp(2),
                TeamSizeSetting.findChangeRejection(2, 9, false, teamRegistry).orElseThrow(),
                TeamSizeSetting.findChangeRejection(2, 3, true, teamRegistry).orElseThrow());
    }

    private List<String> describeLargerTeamsOneByOne() {
        Map<Integer, String> descriptionsByTeamCount = new LinkedHashMap<>();
        for (String teamName : SIX_TEAM_NAMES) {
            registerTeam(teamName, 3);
            int teamCount = teamRegistry.getTeamCount();
            if (teamCount == 1 || teamCount == 2 || teamCount == 5 || teamCount == 6) {
                descriptionsByTeamCount.put(teamCount,
                        TeamSizeSetting.describeTeamsLargerThan(2, teamRegistry).orElseThrow());
            }
        }
        return List.copyOf(descriptionsByTeamCount.values());
    }

    private Team registerTeam(String teamName, int playerCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), teamName + playerNumber));
        }
        return teamRegistry.registerTeam(teamName, members);
    }
}
