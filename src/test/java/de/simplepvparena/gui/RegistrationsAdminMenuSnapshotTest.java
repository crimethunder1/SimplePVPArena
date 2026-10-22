package de.simplepvparena.gui;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationsAdminMenuSnapshotTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_REGISTRATIONS_SLOT = 52;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int INFO_SLOT = 47;
    private static final int FORM_SLOT = 48;
    private static final int NEXT_PAGE_SLOT = 53;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheRegistrationsWithoutAnyPlayer() {
        openTheRegistrations();

        assertEquals("""
                Registrations (page 1) | 54
                46 | BLAZE_ROD | Get the station tool | Puts the registration station tool into your inventory
                47 | BOOK | Registrations | 0 players are registered / At the team size 2 that makes 0 teams / \
                Page 1 of 1
                48 | GRAY_DYE | Form random teams | At least 2 teams are needed, 0 players are registered
                49 | OAK_DOOR | Back | Returns to the team list
                51 | GRAY_DYE | Clear the registrations | No player is registered
                52 | RED_BED | Team mode | Players choose their team / Click to switch
                """, fixture.describeLastMenu());
    }

    @Test
    void showsOneRegisteredPlayer() {
        fixture.registerPlayers("Steve");

        openTheRegistrations();

        assertEquals("""
                Registrations (page 1) | 54
                0 | PLAYER_HEAD | Steve | Click to remove the registration
                46 | BLAZE_ROD | Get the station tool | Puts the registration station tool into your inventory
                47 | BOOK | Registrations | 1 player is registered / At the team size 2 that makes 1 team / \
                Page 1 of 1
                48 | GRAY_DYE | Form random teams | At least 2 teams are needed, 1 player is registered
                49 | OAK_DOOR | Back | Returns to the team list
                51 | BARRIER | Clear the registrations | \
                Removes every registration and every team with a colour, the teams without a colour stay / \
                Asks for a confirmation first
                52 | RED_BED | Team mode | Players choose their team / Click to switch
                """, fixture.describeLastMenu());
    }

    @Test
    void showsThePlayersInTheOrderOfTheirRegistration() {
        fixture.registerPlayers("Steve", "Alex", "Notch", "Jeb_", "Dinnerbone");

        openTheRegistrations();

        assertEquals("""
                Registrations (page 1) | 54
                0 | PLAYER_HEAD | Steve | Click to remove the registration
                1 | PLAYER_HEAD | Alex | Click to remove the registration
                2 | PLAYER_HEAD | Notch | Click to remove the registration
                3 | PLAYER_HEAD | Jeb_ | Click to remove the registration
                4 | PLAYER_HEAD | Dinnerbone | Click to remove the registration
                46 | BLAZE_ROD | Get the station tool | Puts the registration station tool into your inventory
                47 | BOOK | Registrations | 5 players are registered / At the team size 2 that makes 3 teams / \
                Page 1 of 1
                48 | NETHER_STAR | Form random teams | \
                Forms teams of the team size from every registered player at random / Asks for a confirmation first
                49 | OAK_DOOR | Back | Returns to the team list
                51 | BARRIER | Clear the registrations | \
                Removes every registration and every team with a colour, the teams without a colour stay / \
                Asks for a confirmation first
                52 | RED_BED | Team mode | Players choose their team / Click to switch
                """, fixture.describeLastMenu());
    }

    @Test
    void countsTheTeamsAtTheCurrentTeamSize() {
        fixture.registerPlayers("P1", "P2", "P3", "P4", "P5", "P6", "P7");
        fixture.setTeamSize(3);

        openTheRegistrations();

        assertEquals(List.of("7 players are registered", "At the team size 3 that makes 3 teams", "Page 1 of 1"),
                fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void showsTheFirstPageOfFortySixPlayersWithANextPage() {
        fixture.registerPlayers(numberedPlayerNames(46));

        openTheRegistrations();

        assertEquals("Registrations (page 1) | 54\n" + describePlayerSlots(1, 45) + """
                46 | BLAZE_ROD | Get the station tool | Puts the registration station tool into your inventory
                47 | BOOK | Registrations | 46 players are registered / At the team size 2 that makes 23 teams / \
                Page 1 of 2
                48 | NETHER_STAR | Form random teams | \
                Forms teams of the team size from every registered player at random / Asks for a confirmation first
                49 | OAK_DOOR | Back | Returns to the team list
                51 | BARRIER | Clear the registrations | \
                Removes every registration and every team with a colour, the teams without a colour stay / \
                Asks for a confirmation first
                52 | RED_BED | Team mode | Players choose their team / Click to switch
                53 | ARROW | Next page | Page 2 of 2
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheSecondPageOfFortySixPlayersWithAPreviousPage() {
        fixture.registerPlayers(numberedPlayerNames(46));
        openTheRegistrations();

        fixture.click(NEXT_PAGE_SLOT);

        assertEquals("""
                Registrations (page 2) | 54
                0 | PLAYER_HEAD | Player46 | Click to remove the registration
                45 | ARROW | Previous page | Page 1 of 2
                46 | BLAZE_ROD | Get the station tool | Puts the registration station tool into your inventory
                47 | BOOK | Registrations | 46 players are registered / At the team size 2 that makes 23 teams / \
                Page 2 of 2
                48 | NETHER_STAR | Form random teams | \
                Forms teams of the team size from every registered player at random / Asks for a confirmation first
                49 | OAK_DOOR | Back | Returns to the team list
                51 | BARRIER | Clear the registrations | \
                Removes every registration and every team with a colour, the teams without a colour stay / \
                Asks for a confirmation first
                52 | RED_BED | Team mode | Players choose their team / Click to switch
                """, fixture.describeLastMenu());
        fixture.click(PREVIOUS_PAGE_SLOT);
        assertEquals("Registrations (page 1)", fixture.lastMenu().title());
    }

    @Test
    void greysTheFormTileWhileATournamentIsInProgress() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.registerPlayers("P1", "P2", "P3", "P4");
        fixture.startTournament();

        openTheRegistrations();

        MenuTile formTile = fixture.lastMenu().findTile(FORM_SLOT).orElseThrow();
        assertEquals(MenuItems.UNAVAILABLE_MATERIAL, formTile.material());
        assertEquals(List.of("Teams cannot be formed while a tournament is in progress"), formTile.loreLines());
    }

    @Test
    void showsTheRegistrationTileInTheTeamsMenu() {
        fixture.registerPlayers("Steve");

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        MenuTile registrationsTile = fixture.lastMenu().findTile(TEAMS_REGISTRATIONS_SLOT).orElseThrow();
        assertEquals(Material.WRITABLE_BOOK, registrationsTile.material());
        assertEquals("Registrations", registrationsTile.displayName());
        assertEquals(List.of("1 player is registered", "Click to open the registrations"),
                registrationsTile.loreLines());
    }

    @Test
    void showsTheConfirmationOfTheRandomTeams() {
        fixture.registerPlayers("P1", "P2", "P3", "P4", "P5");
        openTheRegistrations();

        fixture.click(FORM_SLOT);

        assertEquals("""
                Form random teams? | 27
                4 | BOOK | Form random teams? | Every existing team is replaced / 3 teams are formed
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheConfirmationOfTheClearing() {
        fixture.registerPlayers("P1", "P2", "P3");
        openTheRegistrations();

        fixture.click(51);

        assertEquals("""
                Clear the registrations? | 27
                4 | BOOK | Clear the registrations? | 3 registrations are removed / \
                Every team with a colour is removed, the teams without a colour stay
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
    }

    private void openTheRegistrations() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_REGISTRATIONS_SLOT);
    }

    private static String[] numberedPlayerNames(int playerCount) {
        String[] playerNames = new String[playerCount];
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            playerNames[playerNumber - 1] = "Player" + playerNumber;
        }
        return playerNames;
    }

    private static String describePlayerSlots(int firstPlayerNumber, int lastPlayerNumber) {
        StringBuilder description = new StringBuilder();
        for (int playerNumber = firstPlayerNumber; playerNumber <= lastPlayerNumber; playerNumber++) {
            description.append(playerNumber - firstPlayerNumber).append(" | PLAYER_HEAD | Player").append(playerNumber)
                    .append(" | Click to remove the registration\n");
        }
        return description.toString();
    }
}
