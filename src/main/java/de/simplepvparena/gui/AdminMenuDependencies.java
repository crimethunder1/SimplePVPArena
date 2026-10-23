package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.ConfirmedBorderParticleWriter;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.registration.RandomTeamFormation;
import de.simplepvparena.registration.RegistrationService;
import de.simplepvparena.registration.RegistrationStationService;
import de.simplepvparena.registration.TeamChoiceService;
import de.simplepvparena.registration.TeamModeSetting;
import de.simplepvparena.registration.TeamModeWriter;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeWriter;
import de.simplepvparena.tournament.AutoMatchModeWriter;
import de.simplepvparena.tournament.AutoMatchService;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

public record AdminMenuDependencies(Plugin plugin,
                                    ChatInputService chatInputService,
                                    Supplier<ConfigurationReloadReport> configurationReloader,
                                    TournamentServices tournament,
                                    TeamServices teams,
                                    ArenaServices arena,
                                    KitServices kits,
                                    RegistrationServices registration) {

    public record TournamentServices(TournamentManager tournamentManager,
                                     TournamentDryRun tournamentDryRun,
                                     MatchService matchService,
                                     AutoMatchService autoMatchService,
                                     AutoMatchModeWriter autoMatchModeWriter) {
    }

    public record TeamServices(TeamRegistry teamRegistry,
                               TeamMemberLookup teamMemberLookup,
                               Runnable teamConfigurationSaver,
                               TeamSizeWriter teamSizeWriter) {
    }

    public record ArenaServices(ArenaSetupService arenaSetupService,
                                ArenaHeightMarkerService heightMarkerService,
                                StartGateItemService gateItemService,
                                BorderSelectorModeRegistry borderSelectorModes,
                                BorderParticleService borderParticleService,
                                ConfirmedBorderParticleWriter confirmedBorderParticleWriter) {
    }

    public record KitServices(KitLibrary kitLibrary, KitSaveService kitSaveService) {
    }

    public record RegistrationServices(RegistrationService registrationService,
                                       RegistrationStationService registrationStationService,
                                       RandomTeamFormation randomTeamFormation,
                                       TeamModeSetting teamModeSetting,
                                       TeamModeWriter teamModeWriter,
                                       TeamChoiceService teamChoiceService) {
    }
}
