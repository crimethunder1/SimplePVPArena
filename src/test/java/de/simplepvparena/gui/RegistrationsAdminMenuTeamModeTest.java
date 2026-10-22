package de.simplepvparena.gui;

import de.simplepvparena.registration.TeamMode;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationsAdminMenuTeamModeTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_REGISTRATIONS_SLOT = 52;
    private static final int TEAM_MODE_SLOT = 52;
    private static final int FORM_SLOT = 48;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheFreeChoiceAsAWholeSentence() {
        openTheRegistrations();

        MenuTile teamModeTile = fixture.lastMenu().findTile(TEAM_MODE_SLOT).orElseThrow();
        assertEquals(Material.RED_BED, teamModeTile.material());
        assertEquals("Team mode", teamModeTile.displayName());
        assertEquals(List.of("Players choose their team", "Click to switch"), teamModeTile.loreLines());
    }

    @Test
    void showsTheRandomTeamsAsAWholeSentence() {
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);

        openTheRegistrations();

        assertEquals(List.of("Teams are formed at random", "Click to switch"),
                fixture.lastMenu().findTile(TEAM_MODE_SLOT).orElseThrow().loreLines());
    }

    @Test
    void switchesToRandomTeamsAndWritesTheValue() {
        openTheRegistrations();

        fixture.click(TEAM_MODE_SLOT);

        assertEquals(TeamMode.RANDOM, fixture.teamModeSetting().getTeamMode());
        assertEquals(List.of(TeamMode.RANDOM), fixture.writtenTeamModes());
        assertEquals(List.of("Teams are formed at random"), fixture.messages());
        assertEquals(List.of("Teams are formed at random", "Click to switch"),
                fixture.lastMenu().findTile(TEAM_MODE_SLOT).orElseThrow().loreLines());
        assertEquals(1, fixture.lastMenu().refreshCount());
    }

    @Test
    void switchesBackToTheFreeChoice() {
        openTheRegistrations();

        fixture.click(TEAM_MODE_SLOT);
        fixture.click(TEAM_MODE_SLOT);

        assertEquals(TeamMode.FREE_CHOICE, fixture.teamModeSetting().getTeamMode());
        assertEquals(List.of(TeamMode.RANDOM, TeamMode.FREE_CHOICE), fixture.writtenTeamModes());
        assertEquals(List.of("Teams are formed at random", "Players choose their team"), fixture.messages());
    }

    @Test
    void keepsTheSwitchedModeAndWarnsWhenConfigYmlCannotBeWritten() {
        fixture.failTeamModeWrites(new IOException("disk full"));
        openTheRegistrations();

        fixture.click(TEAM_MODE_SLOT);

        assertEquals(TeamMode.RANDOM, fixture.teamModeSetting().getTeamMode());
        assertEquals(List.of(), fixture.writtenTeamModes());
        assertEquals(List.of("Teams are formed at random, but config.yml could not be written: disk full"),
                fixture.messages());
    }

    @Test
    void namesTheKindOfFailureWhenItCarriesNoMessage() {
        fixture.failTeamModeWrites(new IOException());
        openTheRegistrations();

        fixture.click(TEAM_MODE_SLOT);

        assertEquals(List.of("Teams are formed at random, but config.yml could not be written: IOException"),
                fixture.messages());
    }

    @Test
    void keepsTheRandomTeamsUsableInBothModes() {
        fixture.registerPlayers("P1", "P2", "P3", "P4");
        openTheRegistrations();
        assertEquals(Material.NETHER_STAR, fixture.lastMenu().findTile(FORM_SLOT).orElseThrow().material());

        fixture.click(TEAM_MODE_SLOT);

        assertEquals(Material.NETHER_STAR, fixture.lastMenu().findTile(FORM_SLOT).orElseThrow().material());
    }

    @Test
    @UseLanguage("de")
    void showsAndSwitchesTheTeamModeInGerman() {
        openTheRegistrations();

        MenuTile teamModeTile = fixture.lastMenu().findTile(TEAM_MODE_SLOT).orElseThrow();
        assertEquals("Teammodus", teamModeTile.displayName());
        assertEquals(List.of("Die Spieler wählen ihr Team", "Klicke zum Umschalten"), teamModeTile.loreLines());

        fixture.failTeamModeWrites(new IOException("Datenträger voll"));
        fixture.click(TEAM_MODE_SLOT);

        assertEquals(List.of("Die Teams werden zufällig gebildet, aber config.yml konnte nicht geschrieben "
                + "werden: Datenträger voll"), fixture.messages());
    }

    private void openTheRegistrations() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_REGISTRATIONS_SLOT);
        fixture.messages().clear();
    }
}
