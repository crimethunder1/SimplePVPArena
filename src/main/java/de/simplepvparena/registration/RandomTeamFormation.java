package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public final class RandomTeamFormation {

    public static final int MINIMUM_TEAM_COUNT = 2;

    private static final String NUMBERED_TEAM_NAME_PREFIX = "Team-";

    private final RegistrationService registrationService;
    private final TeamRegistry teamRegistry;
    private final TeamSizeSetting teamSizeSetting;
    private final BooleanSupplier tournamentInProgress;
    private final BooleanSupplier autoMatchAnnouncedOrRunning;
    private final Runnable teamConfigurationSaver;
    private final Function<UUID, Player> onlinePlayerLookup;
    private final RandomGenerator randomGenerator;

    public RandomTeamFormation(RegistrationService registrationService, TeamRegistry teamRegistry,
                               TeamSizeSetting teamSizeSetting, BooleanSupplier tournamentInProgress,
                               BooleanSupplier autoMatchAnnouncedOrRunning, Runnable teamConfigurationSaver,
                               Function<UUID, Player> onlinePlayerLookup, RandomGenerator randomGenerator) {
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.autoMatchAnnouncedOrRunning = Objects.requireNonNull(autoMatchAnnouncedOrRunning,
                "autoMatchAnnouncedOrRunning");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.onlinePlayerLookup = Objects.requireNonNull(onlinePlayerLookup, "onlinePlayerLookup");
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "randomGenerator");
    }

    public static int countTeams(int playerCount, int teamSize) {
        return (playerCount + teamSize - 1) / teamSize;
    }

    public int countTeams() {
        return countTeams(registrationService.getCount(), teamSizeSetting.getTeamSize());
    }

    public Optional<String> findRejection() {
        if (tournamentInProgress.getAsBoolean()) {
            return Optional.of(Messages.text(RegistrationMessage.FORMING_TOURNAMENT_IN_PROGRESS));
        }
        if (autoMatchAnnouncedOrRunning.getAsBoolean()) {
            return Optional.of(Messages.text(RegistrationMessage.FORMING_AUTO_MATCH));
        }
        if (countTeams() < MINIMUM_TEAM_COUNT) {
            return Optional.of(Messages.plural(RegistrationMessage.FORMING_TOO_FEW_PLAYERS,
                    registrationService.getCount(), Placeholder.of("minimum", MINIMUM_TEAM_COUNT)));
        }
        return Optional.empty();
    }

    public boolean form(Player admin) {
        Optional<String> rejection = findRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(admin, rejection.get());
            return false;
        }
        List<Team> teams = buildTeams();
        teamRegistry.clear();
        for (Team team : teams) {
            teamRegistry.registerTeam(team.getTeamName(), team.getMembers(), team.getColour().orElse(null));
        }
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(admin, Messages.plural(RegistrationMessage.FORMING_FORMED, teams.size(),
                Placeholder.of("players", registrationService.getCount())));
        teams.forEach(this::tellTheMembers);
        return true;
    }

    private List<Team> buildTeams() {
        List<RegistrationEntry> entries = new ArrayList<>(registrationService.getEntries());
        Collections.shuffle(entries, randomGenerator);
        int teamSize = teamSizeSetting.getTeamSize();
        TeamRegistry formedTeams = new TeamRegistry();
        Set<String> usedNames = new HashSet<>();
        for (int firstIndex = 0; firstIndex < entries.size(); firstIndex += teamSize) {
            int teamNumber = formedTeams.getTeamCount() + 1;
            List<TeamMember> members = entries.subList(firstIndex, Math.min(firstIndex + teamSize, entries.size()))
                    .stream()
                    .map(entry -> new TeamMember(entry.uniqueId(), entry.playerName()))
                    .toList();
            TeamColour colour = teamNumber <= TeamColour.values().length ? TeamColour.values()[teamNumber - 1] : null;
            String teamName = nameTeam(teamNumber, colour, usedNames);
            formedTeams.registerTeam(teamName, members, colour);
        }
        return List.copyOf(formedTeams.getTeams());
    }

    private static String nameTeam(int teamNumber, TeamColour colour, Set<String> usedNames) {
        String teamName = colour == null ? null : colour.getDisplayName();
        if (teamName == null || !TeamRegistry.isAllowedTeamName(teamName)
                || usedNames.contains(teamName.toLowerCase(Locale.ROOT))) {
            teamName = NUMBERED_TEAM_NAME_PREFIX + teamNumber;
        }
        usedNames.add(teamName.toLowerCase(Locale.ROOT));
        return teamName;
    }

    private void tellTheMembers(Team team) {
        for (TeamMember member : team.getMembers()) {
            Player player = onlinePlayerLookup.apply(member.uniqueId());
            if (player == null) {
                continue;
            }
            Placeholder teamName = Placeholder.of("team", team.getTeamName());
            List<String> mateNames = team.getMembers().stream()
                    .filter(mate -> !mate.uniqueId().equals(member.uniqueId()))
                    .map(TeamMember::playerName)
                    .toList();
            TournamentMessages.sendInfo(player, mateNames.isEmpty()
                    ? Messages.text(RegistrationMessage.FORMING_PLAYING_ALONE, teamName)
                    : Messages.text(RegistrationMessage.FORMING_PLAYING_WITH_MATES, teamName,
                            Placeholder.of("mates", Messages.join(mateNames))));
        }
    }
}
