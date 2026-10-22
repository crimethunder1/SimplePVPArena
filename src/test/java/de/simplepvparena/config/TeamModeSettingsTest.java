package de.simplepvparena.config;

import de.simplepvparena.registration.TeamMode;
import de.simplepvparena.registration.TeamModeSetting;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamModeSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void letsThePlayersChooseTheirTeamByDefault() {
        PluginSettings settings = PluginSettings.read(new MemoryConfiguration(), problems::add);

        assertEquals(TeamMode.FREE_CHOICE, settings.teamMode());
        assertEquals(TeamMode.FREE_CHOICE, PluginSettings.defaults().teamMode());
        assertEquals(TeamMode.FREE_CHOICE, new TeamModeSetting().getTeamMode());
        assertEquals(List.of(), problems);
    }

    @Test
    void readsBothTeamModesWhateverTheCaseAndTheSurroundingSpaces() {
        assertEquals(TeamMode.RANDOM, readTeamMode("random"));
        assertEquals(TeamMode.RANDOM, readTeamMode(" Random "));
        assertEquals(TeamMode.FREE_CHOICE, readTeamMode("free-choice"));
        assertEquals(TeamMode.FREE_CHOICE, readTeamMode("FREE-CHOICE"));
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToTheFreeChoiceWithAWarningForAnUnknownValue() {
        assertEquals(TeamMode.FREE_CHOICE, readTeamMode("chosen"));

        assertEquals(List.of("registration.team-mode must be one of free-choice and random, falling back to "
                + "free-choice"), problems);
    }

    @Test
    void fallsBackToTheFreeChoiceWithAWarningForAValueThatIsNoText() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.REGISTRATION_TEAM_MODE_PATH, 3);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(TeamMode.FREE_CHOICE, settings.teamMode());
        assertEquals(1, problems.size());
    }

    @Test
    void appliesTheTeamModeWithEveryReload() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.REGISTRATION_TEAM_MODE_PATH, "random");
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();

        new ConfigurationReloadService(() -> configuration, () -> { }, liveConfiguration, problems::add)
                .reloadFromFile();

        assertEquals(TeamMode.RANDOM, liveConfiguration.valueOf(RecordingLiveConfiguration.TEAM_MODE));
    }

    @Test
    void theReloadReportNamesTheTeamMode() {
        assertTrue(PluginSettings.CONFIGURATION_PATHS.contains(PluginSettings.REGISTRATION_TEAM_MODE_PATH));
        assertTrue(ConfigurationReloadReport.describeReloadedCategories().contains(
                "whether the players choose their team after registering or the teams are formed at random"));
    }

    @Test
    void switchesBetweenTheTwoTeamModes() {
        assertEquals(TeamMode.RANDOM, TeamMode.FREE_CHOICE.switched());
        assertEquals(TeamMode.FREE_CHOICE, TeamMode.RANDOM.switched());
    }

    private TeamMode readTeamMode(String writtenValue) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.REGISTRATION_TEAM_MODE_PATH, writtenValue);
        return PluginSettings.read(configuration, problems::add).teamMode();
    }
}
