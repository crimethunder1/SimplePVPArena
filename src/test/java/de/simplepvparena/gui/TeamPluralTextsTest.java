package de.simplepvparena.gui;

import de.simplepvparena.message.MessageCatalog;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamActionsMessage;
import de.simplepvparena.message.TeamMenuMessage;
import de.simplepvparena.message.TeamMessage;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.TeamRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamPluralTextsTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_SMALLER_SLOT = 46;
    private static final int TEAMS_INFO_SLOT = 47;
    private static final int TEAMS_LARGER_SLOT = 48;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAM_ADD_PLAYER_SLOT = 11;
    private static final int TEAM_REMOVE_PLAYER_SLOT = 12;
    private static final int PROMPT_SIZE_LINE = 1;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void theRegisteredTeamsOfTheTeamListTakeTheSingularForOneAndThePluralForTwo() {
        Messages.use(catalogWithDistinctPluralForms());
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        String oneTeam = loreOf(TEAMS_INFO_SLOT).get(0);

        fixture.registerTeams("Bravo");
        fixture.refillLastMenu();

        assertEquals("One registered team: 1", oneTeam);
        assertEquals("Several registered teams: 2", loreOf(TEAMS_INFO_SLOT).get(0));
    }

    @Test
    void thePlayerCountOfTheTeamOptionsTakesTheSingularForOneAndThePluralForTwo() {
        Messages.use(catalogWithDistinctPluralForms());
        fixture.setTeamSize(3);
        fixture.registerTeamOf("Solo", "Steve");
        fixture.registerTeamOf("Duo", "Alex", "Notch");

        openTheOptionsOf("Solo");
        String onePlayer = loreOf(AdminMenuLayout.INFO_SLOT).get(1);
        openTheOptionsOf("Duo");

        assertEquals("One player: 1 of 3", onePlayer);
        assertEquals("Several players: 2 of 3", loreOf(AdminMenuLayout.INFO_SLOT).get(1));
    }

    @Test
    void thePlayerCountOfThePlayersMenuTakesTheSingularForOneAndThePluralForTwo() {
        Messages.use(catalogWithDistinctPluralForms());
        fixture.registerTeamOf("Duo", "Alex", "Notch");
        openTheOptionsOf("Duo");
        fixture.click(TEAM_REMOVE_PLAYER_SLOT);
        String twoPlayers = loreOf(AdminMenuLayout.INFO_SLOT).get(0);

        fixture.teamRegistry().removeMember("Duo", fixture.teamRegistry().findTeam("Duo").orElseThrow()
                .getMembers().get(1).uniqueId());
        fixture.refillLastMenu();

        assertEquals("Several players in the list: 2 of 2", twoPlayers);
        assertEquals("One player in the list: 1 of 2", loreOf(AdminMenuLayout.INFO_SLOT).get(0));
    }

    @Test
    @UseLanguage("en")
    void theShippedEnglishTextsCountOneAndTwo() {
        assertEquals(List.of(
                "Click to make teams of 1 player",
                "Click to make teams of 2 players",
                "Click to make teams of 1 player",
                "Click to make teams of 2 players",
                "A team has 1 player, a team name has 1 to 16 letters, digits, hyphens or underscores",
                "A team has 2 players, a team name has 1 to 16 letters, digits, hyphens or underscores",
                "The team already has 1 player, the team size is 1",
                "Team Solo already has 1 player, the team size is 1",
                "The team already has 2 players, the team size is 2",
                "Team Alpha already has 2 players, the team size is 2",
                "Team Solo has 1 of 1 player",
                "Team Solo has 1 of 2 players",
                "team Octet already has 1 player, a team has at most 1 player",
                "team Octet already has 2 players, a team has at most 2 players",
                "a team keeps at least 1 player, remove the team instead",
                "a team keeps at least 2 players, remove the team instead"), collectEveryPluralTextForOneAndTwo());
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextsCountOneAndTwo() {
        assertEquals(List.of(
                "Klicke, um Teams aus 1 Spieler zu bilden",
                "Klicke, um Teams aus 2 Spielern zu bilden",
                "Klicke, um Teams aus 1 Spieler zu bilden",
                "Klicke, um Teams aus 2 Spielern zu bilden",
                "Ein Team hat 1 Spieler, ein Teamname hat 1 bis 16 Buchstaben, Ziffern, Bindestriche oder "
                        + "Unterstriche",
                "Ein Team hat 2 Spieler, ein Teamname hat 1 bis 16 Buchstaben, Ziffern, Bindestriche oder "
                        + "Unterstriche",
                "Das Team hat bereits 1 Spieler, die Teamgröße ist 1",
                "Team Solo hat bereits 1 Spieler, die Teamgröße ist 1",
                "Das Team hat bereits 2 Spieler, die Teamgröße ist 2",
                "Team Alpha hat bereits 2 Spieler, die Teamgröße ist 2",
                "Team Solo hat 1 von 1 Spieler",
                "Team Solo hat 1 von 2 Spielern",
                "Team Octet hat bereits 1 Spieler, ein Team hat höchstens 1 Spieler",
                "Team Octet hat bereits 2 Spieler, ein Team hat höchstens 2 Spieler",
                "ein Team behält mindestens 1 Spieler, entferne stattdessen das Team",
                "ein Team behält mindestens 2 Spieler, entferne stattdessen das Team"),
                collectEveryPluralTextForOneAndTwo());
    }

    private List<String> collectEveryPluralTextForOneAndTwo() {
        List<String> texts = new ArrayList<>();
        fixture.registerTeamOf("Solo", "Steve");
        fixture.registerTeams("Alpha");
        Placeholder nameRule = Placeholder.of("rule", TeamRegistry.describeNameRule());
        Placeholder solo = Placeholder.of("team", "Solo");
        Placeholder octet = Placeholder.of("team", "Octet");

        fixture.setTeamSize(2);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        texts.add(loreOf(TEAMS_SMALLER_SLOT).get(0));
        fixture.setTeamSize(3);
        fixture.refillLastMenu();
        texts.add(loreOf(TEAMS_SMALLER_SLOT).get(0));
        texts.add(Messages.plural(TeamMenuMessage.LARGER_LORE, 1));
        fixture.setTeamSize(1);
        fixture.refillLastMenu();
        texts.add(loreOf(TEAMS_LARGER_SLOT).get(0));
        fixture.click(TEAMS_ADD_SLOT);
        texts.add(fixture.messages().get(PROMPT_SIZE_LINE));
        cancelTheInput();
        texts.add(Messages.plural(TeamMenuMessage.ADD_INPUT_EXACT_SIZE, 2, nameRule));

        openTheOptionsOf("Solo");
        texts.add(loreOf(TEAM_ADD_PLAYER_SLOT).get(0));
        texts.add(lastMessageAfterClicking(TEAM_ADD_PLAYER_SLOT));
        fixture.setTeamSize(2);
        openTheOptionsOf("Alpha");
        texts.add(loreOf(TEAM_ADD_PLAYER_SLOT).get(0));
        texts.add(lastMessageAfterClicking(TEAM_ADD_PLAYER_SLOT));

        texts.add(Messages.plural(TeamActionsMessage.ADD_PLAYER_INPUT_PLAYER_COUNT, 1, solo,
                Placeholder.of("memberCount", 1)));
        openTheOptionsOf("Solo");
        fixture.click(TEAM_ADD_PLAYER_SLOT);
        texts.add(fixture.messages().get(PROMPT_SIZE_LINE));
        cancelTheInput();

        texts.add(Messages.plural(TeamMessage.TEAM_FULL, 1, octet));
        texts.add(Messages.plural(TeamMessage.TEAM_FULL, 2, octet));
        texts.add(Messages.plural(TeamMessage.LAST_MEMBER, 1));
        texts.add(Messages.plural(TeamMessage.LAST_MEMBER, 2));
        return texts;
    }

    private void openTheOptionsOf(String teamName) {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
        fixture.messages().clear();
    }

    private String lastMessageAfterClicking(int slot) {
        fixture.messages().clear();
        fixture.click(slot);
        return fixture.messages().get(fixture.messages().size() - 1);
    }

    private void cancelTheInput() {
        fixture.typeInChat("cancel");
        fixture.runNextTick();
        fixture.messages().clear();
    }

    private List<String> loreOf(int slot) {
        return fixture.lastMenu().findTile(slot).orElseThrow().loreLines();
    }

    private static MessageCatalog catalogWithDistinctPluralForms() {
        MessageCatalog shippedEnglish = MessageCatalogLoader.shippedCatalog(MessageCatalogLoader.DEFAULT_LANGUAGE);
        Map<String, List<String>> pluralForms = Map.of(
                singularPath(TeamMenuMessage.REGISTERED_TEAMS), List.of("One registered team: {count}"),
                pluralPath(TeamMenuMessage.REGISTERED_TEAMS), List.of("Several registered teams: {count}"),
                singularPath(TeamActionsMessage.INFO_PLAYER_COUNT), List.of("One player: {count} of {teamSize}"),
                pluralPath(TeamActionsMessage.INFO_PLAYER_COUNT), List.of("Several players: {count} of {teamSize}"),
                singularPath(TeamActionsMessage.MEMBERS_INFO_PLAYER_COUNT),
                List.of("One player in the list: {count} of {teamSize}"),
                pluralPath(TeamActionsMessage.MEMBERS_INFO_PLAYER_COUNT),
                List.of("Several players in the list: {count} of {teamSize}"));
        return new MessageCatalog() {
            @Override
            public Optional<List<String>> find(String path) {
                return Optional.ofNullable(pluralForms.get(path)).or(() -> shippedEnglish.find(path));
            }

            @Override
            public Set<String> paths() {
                return shippedEnglish.paths();
            }
        };
    }

    private static String singularPath(MessageKey key) {
        return key.path() + Messages.SINGULAR_SUFFIX;
    }

    private static String pluralPath(MessageKey key) {
        return key.path() + Messages.PLURAL_SUFFIX;
    }
}
