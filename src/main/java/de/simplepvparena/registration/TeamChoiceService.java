package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamChoiceMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class TeamChoiceService implements RegistrationObserver {

    public static final int MINIMUM_OFFERED_COLOUR_COUNT = 2;

    private static final String NAME_SUFFIX_SEPARATOR = "-";
    private static final String NUMBERED_TEAM_NAME_PREFIX = "Team-";
    private static final int FIRST_NAME_SUFFIX_NUMBER = 2;

    private final RegistrationService registrationService;
    private final TeamRegistry teamRegistry;
    private final TeamSizeSetting teamSizeSetting;
    private final TeamModeSetting teamModeSetting;
    private final BooleanSupplier tournamentInProgress;
    private final Function<String, Optional<String>> teamChangeRejection;
    private final Runnable teamConfigurationSaver;
    private final Function<UUID, Player> onlinePlayerLookup;

    private TeamChoicePresenter presenter = TeamChoicePresenter.NONE;

    public TeamChoiceService(RegistrationService registrationService, TeamRegistry teamRegistry,
                             TeamSizeSetting teamSizeSetting, TeamModeSetting teamModeSetting,
                             BooleanSupplier tournamentInProgress,
                             Function<String, Optional<String>> teamChangeRejection,
                             Runnable teamConfigurationSaver, Function<UUID, Player> onlinePlayerLookup) {
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.teamModeSetting = Objects.requireNonNull(teamModeSetting, "teamModeSetting");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.teamChangeRejection = Objects.requireNonNull(teamChangeRejection, "teamChangeRejection");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.onlinePlayerLookup = Objects.requireNonNull(onlinePlayerLookup, "onlinePlayerLookup");
    }

    public void setPresenter(TeamChoicePresenter presenter) {
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    public static int countNeededColours(int registeredPlayerCount, int teamSize) {
        return Math.clamp(RandomTeamFormation.countTeams(registeredPlayerCount, teamSize),
                MINIMUM_OFFERED_COLOUR_COUNT, TeamColour.values().length);
    }

    public List<TeamColour> findOfferedColours() {
        int neededColourCount = countNeededColours(registrationService.getCount(), teamSizeSetting.getTeamSize());
        List<TeamColour> offeredColours = new ArrayList<>();
        for (TeamColour colour : TeamColour.values()) {
            if (colour.ordinal() < neededColourCount || findTeamOfColour(colour).isPresent()) {
                offeredColours.add(colour);
            }
        }
        return List.copyOf(offeredColours);
    }

    public Optional<Team> findTeamOfColour(TeamColour colour) {
        return teamRegistry.getTeams().stream()
                .filter(team -> team.getColour().filter(colour::equals).isPresent())
                .findFirst();
    }

    public Optional<Team> findTeamOfPlayer(UUID playerUniqueId) {
        return teamRegistry.findTeamByPlayer(playerUniqueId);
    }

    public int getTeamSize() {
        return teamSizeSetting.getTeamSize();
    }

    public int getRegisteredPlayerCount() {
        return registrationService.getCount();
    }

    public boolean removesColouredTeamsWithTheRegistrations() {
        return teamModeSetting.isFreeChoice() && !tournamentInProgress.getAsBoolean();
    }

    public Optional<String> findAccessRejection(UUID playerUniqueId) {
        if (tournamentInProgress.getAsBoolean()) {
            return Optional.of(Messages.text(TeamChoiceMessage.CLOSED));
        }
        if (!teamModeSetting.isFreeChoice()) {
            return Optional.of(Messages.text(TeamChoiceMessage.RANDOM_MODE));
        }
        if (!registrationService.isRegistered(playerUniqueId)) {
            return Optional.of(Messages.text(TeamChoiceMessage.REGISTER_FIRST));
        }
        return Optional.empty();
    }

    public void join(Player player, TeamColour colour) {
        UUID playerUniqueId = player.getUniqueId();
        Optional<Team> currentTeam = findTeamOfPlayer(playerUniqueId);
        Optional<Team> targetTeam = findTeamOfColour(colour);
        if (currentTeam.isPresent() && targetTeam.isPresent() && isSameTeam(currentTeam.get(), targetTeam.get())) {
            return;
        }
        Optional<String> rejection = findAccessRejection(playerUniqueId)
                .or(() -> currentTeam.filter(team -> team.getColour().isEmpty())
                        .map(team -> Messages.text(TeamChoiceMessage.ASSIGNED_BY_ADMIN,
                                Placeholder.of("team", team.getTeamName()))))
                .or(() -> currentTeam.flatMap(team -> teamChangeRejection.apply(team.getTeamName())))
                .or(() -> targetTeam.filter(this::isFull).map(team -> Messages.text(TeamChoiceMessage.TEAM_FULL)))
                .or(() -> targetTeam.flatMap(team -> teamChangeRejection.apply(team.getTeamName())));
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        currentTeam.ifPresent(team -> removeFromTeam(team, playerUniqueId));
        TeamMember member = new TeamMember(playerUniqueId, player.getName());
        Team joinedTeam = targetTeam
                .map(team -> teamRegistry.addMember(team.getTeamName(), member))
                .orElseGet(() -> teamRegistry.registerTeam(nameNewTeam(colour), List.of(member), colour));
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(player, Messages.text(TeamChoiceMessage.JOINED,
                Placeholder.of("team", joinedTeam.getTeamName())));
        tellTheOtherMembers(joinedTeam, player);
        presenter.refresh();
    }

    public void leave(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        Optional<Team> currentTeam = findTeamOfPlayer(playerUniqueId);
        Optional<String> rejection = findAccessRejection(playerUniqueId)
                .or(() -> currentTeam.isEmpty()
                        ? Optional.of(Messages.text(TeamChoiceMessage.NOT_IN_A_TEAM))
                        : teamChangeRejection.apply(currentTeam.get().getTeamName()));
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        removeFromTeam(currentTeam.orElseThrow(), playerUniqueId);
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(player, Messages.text(TeamChoiceMessage.LEFT,
                Placeholder.of("team", currentTeam.get().getTeamName())));
        presenter.refresh();
    }

    public int removeColouredTeams() {
        if (!removesColouredTeamsWithTheRegistrations()) {
            return 0;
        }
        int removedTeamCount = 0;
        for (Team team : teamRegistry.getTeams()) {
            if (team.getColour().isPresent() && teamChangeRejection.apply(team.getTeamName()).isEmpty()) {
                teamRegistry.removeTeam(team.getTeamName());
                removedTeamCount++;
            }
        }
        if (removedTeamCount > 0) {
            teamConfigurationSaver.run();
        }
        presenter.refresh();
        return removedTeamCount;
    }

    @Override
    public void playerRegistered(Player player) {
        if (teamModeSetting.isFreeChoice()) {
            presenter.show(player);
        }
        presenter.refresh();
    }

    @Override
    public Optional<String> findUnregistrationRejection(UUID playerUniqueId) {
        if (!teamModeSetting.isFreeChoice()) {
            return Optional.empty();
        }
        return findTeamOfPlayer(playerUniqueId)
                .filter(team -> team.getColour().isPresent())
                .flatMap(team -> teamChangeRejection.apply(team.getTeamName()));
    }

    @Override
    public void playerUnregistered(UUID playerUniqueId) {
        if (teamModeSetting.isFreeChoice()) {
            findTeamOfPlayer(playerUniqueId)
                    .filter(team -> team.getColour().isPresent())
                    .ifPresent(team -> leaveColouredTeam(team, playerUniqueId));
        }
        presenter.refresh();
    }

    private void leaveColouredTeam(Team team, UUID playerUniqueId) {
        Optional<String> rejection = tournamentInProgress.getAsBoolean()
                ? Optional.of(Messages.text(TeamChoiceMessage.CLOSED))
                : teamChangeRejection.apply(team.getTeamName());
        if (rejection.isPresent()) {
            Player player = onlinePlayerLookup.apply(playerUniqueId);
            if (player != null) {
                TournamentMessages.sendWarning(player, rejection.get());
            }
            return;
        }
        removeFromTeam(team, playerUniqueId);
        teamConfigurationSaver.run();
    }

    private void removeFromTeam(Team team, UUID playerUniqueId) {
        if (team.getMembers().size() <= Team.MINIMUM_MEMBER_COUNT) {
            teamRegistry.removeTeam(team.getTeamName());
            return;
        }
        teamRegistry.removeMember(team.getTeamName(), playerUniqueId);
    }

    private boolean isFull(Team team) {
        return team.getMembers().size() >= teamSizeSetting.getTeamSize();
    }

    private static boolean isSameTeam(Team firstTeam, Team secondTeam) {
        return firstTeam.getTeamName().equalsIgnoreCase(secondTeam.getTeamName());
    }

    private String nameNewTeam(TeamColour colour) {
        String colourName = colour.getDisplayName();
        if (isFreeTeamName(colourName)) {
            return colourName;
        }
        for (int suffixNumber = FIRST_NAME_SUFFIX_NUMBER; ; suffixNumber++) {
            String numberedName = colourName + NAME_SUFFIX_SEPARATOR + suffixNumber;
            if (!TeamRegistry.isAllowedTeamName(numberedName)) {
                numberedName = NUMBERED_TEAM_NAME_PREFIX + suffixNumber;
            }
            if (isFreeTeamName(numberedName)) {
                return numberedName;
            }
        }
    }

    private boolean isFreeTeamName(String teamName) {
        return TeamRegistry.isAllowedTeamName(teamName) && teamRegistry.findTeam(teamName).isEmpty();
    }

    private void tellTheOtherMembers(Team team, Player joinedPlayer) {
        for (TeamMember member : team.getMembers()) {
            if (member.uniqueId().equals(joinedPlayer.getUniqueId())) {
                continue;
            }
            Player mate = onlinePlayerLookup.apply(member.uniqueId());
            if (mate != null) {
                TournamentMessages.sendInfo(mate, Messages.text(TeamChoiceMessage.MEMBER_JOINED,
                        Placeholder.of("player", joinedPlayer.getName())));
            }
        }
    }
}
