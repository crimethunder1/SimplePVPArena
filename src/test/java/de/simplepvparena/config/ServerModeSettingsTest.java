package de.simplepvparena.config;

import de.simplepvparena.tournament.AutoMatchSettings;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerModeSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheEventMode() {
        assertEquals(ServerMode.EVENT, PluginSettings.defaults().serverMode());
        assertEquals(ServerMode.EVENT, PluginSettings.read(new MemoryConfiguration(), problems::add).serverMode());
        assertEquals(List.of(), problems);
    }

    @Test
    void readsBothModesWithoutMindingTheCase() {
        assertEquals(ServerMode.AUTO, PluginSettings.read(configurationWith(" Auto "), problems::add).serverMode());
        assertEquals(ServerMode.EVENT, PluginSettings.read(configurationWith("EVENT"), problems::add).serverMode());
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToTheEventModeForAnUnknownValue() {
        assertEquals(ServerMode.EVENT, PluginSettings.read(configurationWith("lobby"), problems::add).serverMode());

        assertEquals(List.of("server-mode must be one of event and auto, falling back to event"), problems);
    }

    @Test
    void theEventModeSwitchesOffAutoMatchesAndInventoriesAndSendsJoiningPlayersToTheTribune() {
        MemoryConfiguration configuration = configurationWith("event");
        configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, true);
        configuration.set(PluginSettings.ARENA_AREA_INVENTORY_ENABLED_PATH, true);
        configuration.set(PluginSettings.ARENA_TELEPORT_ON_JOIN_PATH, true);
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();

        PluginSettings.read(configuration, problems::add).applyTo(liveConfiguration);

        assertEquals(ServerMode.EVENT, liveConfiguration.valueOf(RecordingLiveConfiguration.SERVER_MODE));
        assertFalse(liveConfiguration.autoMatchSettings().enabled());
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.AREA_INVENTORY_ENABLED));
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.TELEPORT_ON_JOIN));
    }

    @Test
    void theAutoModeFollowsTheSwitchesAndLeavesJoiningPlayersWhereTheyAre() {
        MemoryConfiguration configuration = configurationWith("auto");
        configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, true);
        configuration.set(PluginSettings.ARENA_AREA_INVENTORY_ENABLED_PATH, true);
        configuration.set(PluginSettings.ARENA_TELEPORT_ON_JOIN_PATH, true);
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();

        PluginSettings.read(configuration, problems::add).applyTo(liveConfiguration);

        assertEquals(ServerMode.AUTO, liveConfiguration.valueOf(RecordingLiveConfiguration.SERVER_MODE));
        assertEquals(AutoMatchSettings.defaults().withEnabled(true), liveConfiguration.autoMatchSettings());
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.AREA_INVENTORY_ENABLED));
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.TELEPORT_ON_JOIN));
    }

    @Test
    void theAutoModeKeepsSwitchesThatAreOff() {
        MemoryConfiguration configuration = configurationWith("auto");
        configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, false);
        configuration.set(PluginSettings.ARENA_AREA_INVENTORY_ENABLED_PATH, false);
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();

        PluginSettings.read(configuration, problems::add).applyTo(liveConfiguration);

        assertFalse(liveConfiguration.autoMatchSettings().enabled());
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.AREA_INVENTORY_ENABLED));
    }

    @Test
    void namesTheModesAndSwitchesBetweenThem() {
        assertEquals(List.of("event", "auto"), ServerMode.configurationNames());
        assertEquals(ServerMode.AUTO, ServerMode.EVENT.switched());
        assertEquals(ServerMode.EVENT, ServerMode.AUTO.switched());
        assertEquals(Optional.empty(), ServerMode.fromConfigurationName(""));
    }

    private static MemoryConfiguration configurationWith(String serverMode) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.SERVER_MODE_PATH, serverMode);
        return configuration;
    }
}
