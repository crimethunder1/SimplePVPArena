package de.simplepvparena.tournament;

import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.match.Match;
import de.simplepvparena.message.AutoMatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TeamChoiceMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.registration.RegistrationService;
import de.simplepvparena.registration.TeamModeSetting;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class PlayAgainService implements PlayAgainRule {

    public static final String COMMAND_ARGUMENT = "playagain";

    private static final String CLICK_COMMAND = String.join(" ", "/tournament", COMMAND_ARGUMENT);
    private static final long MILLISECONDS_PER_SECOND = 1000L;

    private final TeamRegistry teamRegistry;
    private final RegistrationService registrationService;
    private final TeamModeSetting teamModeSetting;
    private final BooleanSupplier tournamentInProgress;
    private final Runnable teamConfigurationSaver;
    private final Function<UUID, Player> onlinePlayerLookup;
    private final Supplier<Optional<EffectArea>> effectAreaLookup;
    private final LongSupplier clock;
    private final Map<String, Long> playAgainDeadlineByWaitingTeamKey = new HashMap<>();
    private final Map<UUID, Long> questionDeadlineByPlayer = new HashMap<>();

    public PlayAgainService(AutoMatchService autoMatchService, TeamRegistry teamRegistry,
                            RegistrationService registrationService, TeamModeSetting teamModeSetting,
                            BooleanSupplier tournamentInProgress, Runnable teamConfigurationSaver,
                            Function<UUID, Player> onlinePlayerLookup,
                            Supplier<Optional<EffectArea>> effectAreaLookup, LongSupplier clock) {
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.teamModeSetting = Objects.requireNonNull(teamModeSetting, "teamModeSetting");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.onlinePlayerLookup = Objects.requireNonNull(onlinePlayerLookup, "onlinePlayerLookup");
        this.effectAreaLookup = Objects.requireNonNull(effectAreaLookup, "effectAreaLookup");
        this.clock = Objects.requireNonNull(clock, "clock");
        autoMatchService.setPlayAgainRule(this);
        teamRegistry.addRenameObserver(this::followRenamedTeam);
    }

    public static Component buildQuestion() {
        Component hover = Component.text(Messages.text(AutoMatchMessage.PLAY_AGAIN_HOVER),
                TournamentMessages.DETAIL_COLOUR);
        Component button = Component.text(Messages.text(AutoMatchMessage.PLAY_AGAIN_BUTTON),
                        TournamentMessages.SUCCESS_COLOUR)
                .clickEvent(ClickEvent.runCommand(CLICK_COMMAND))
                .hoverEvent(HoverEvent.showText(hover));
        return TournamentMessages.info(Messages.text(AutoMatchMessage.PLAY_AGAIN_QUESTION))
                .append(Component.space())
                .append(button);
    }

    @Override
    public void autoMatchFinished(Match match, int playAgainSeconds) {
        if (playAgainSeconds <= AutoMatchSettings.MINIMUM_PLAY_AGAIN_SECONDS) {
            return;
        }
        long playAgainDeadline = clock.getAsLong() + playAgainSeconds * MILLISECONDS_PER_SECOND;
        Set<UUID> participantUniqueIds = new LinkedHashSet<>();
        boolean teamsChanged = false;
        for (Team team : match.getTeams()) {
            participantUniqueIds.addAll(team.getMemberUniqueIds());
            teamsChanged |= releaseTheTeam(team, playAgainDeadline);
        }
        if (teamsChanged) {
            teamConfigurationSaver.run();
        }
        for (UUID participantUniqueId : participantUniqueIds) {
            registrationService.removeRegistration(participantUniqueId);
        }
        askToPlayAgain(participantUniqueIds, playAgainDeadline);
    }

    @Override
    public boolean isWaitingForPlayAgain(Team team) {
        String teamKey = toKey(team.getTeamName());
        Long playAgainDeadline = playAgainDeadlineByWaitingTeamKey.get(teamKey);
        if (playAgainDeadline == null) {
            return false;
        }
        if (clock.getAsLong() >= playAgainDeadline) {
            playAgainDeadlineByWaitingTeamKey.remove(teamKey);
            return false;
        }
        return team.getColour().isEmpty();
    }

    public boolean hasOpenQuestion(UUID playerUniqueId) {
        Long questionDeadline = questionDeadlineByPlayer.get(playerUniqueId);
        return questionDeadline != null && clock.getAsLong() < questionDeadline;
    }

    public void playAgain(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        if (!hasOpenQuestion(playerUniqueId)) {
            questionDeadlineByPlayer.remove(playerUniqueId);
            TournamentMessages.sendWarning(player, Messages.text(AutoMatchMessage.PLAY_AGAIN_NO_REMATCH));
            return;
        }
        if (tournamentInProgress.getAsBoolean()) {
            TournamentMessages.sendError(player, Messages.text(RegistrationMessage.CLOSED));
            return;
        }
        questionDeadlineByPlayer.remove(playerUniqueId);
        Optional<Team> teamWithoutAColour = teamRegistry.findTeamByPlayer(playerUniqueId)
                .filter(team -> team.getColour().isEmpty());
        if (teamWithoutAColour.isPresent()) {
            playAgainDeadlineByWaitingTeamKey.remove(toKey(teamWithoutAColour.get().getTeamName()));
            registrationService.joinWithoutTeamChoice(player);
            return;
        }
        registrationService.join(player);
        if (!teamModeSetting.isFreeChoice()) {
            TournamentMessages.sendInfo(player, Messages.text(TeamChoiceMessage.RANDOM_MODE));
        }
    }

    private boolean releaseTheTeam(Team matchTeam, long playAgainDeadline) {
        Optional<Team> registeredTeam = teamRegistry.findTeam(matchTeam.getTeamName());
        if (registeredTeam.isEmpty()) {
            return false;
        }
        if (registeredTeam.get().getColour().isEmpty()) {
            playAgainDeadlineByWaitingTeamKey.put(toKey(matchTeam.getTeamName()), playAgainDeadline);
            return false;
        }
        boolean teamChanged = false;
        for (UUID participantUniqueId : matchTeam.getMemberUniqueIds()) {
            teamChanged |= leaveTheColouredTeam(matchTeam.getTeamName(), participantUniqueId);
        }
        return teamChanged;
    }

    private boolean leaveTheColouredTeam(String teamName, UUID participantUniqueId) {
        Optional<Team> team = teamRegistry.findTeam(teamName).filter(registeredTeam ->
                registeredTeam.containsPlayer(participantUniqueId));
        if (team.isEmpty()) {
            return false;
        }
        if (team.get().getMembers().size() <= Team.MINIMUM_MEMBER_COUNT) {
            teamRegistry.removeTeam(teamName);
            return true;
        }
        teamRegistry.removeMember(teamName, participantUniqueId);
        return true;
    }

    private void askToPlayAgain(Set<UUID> participantUniqueIds, long playAgainDeadline) {
        Optional<EffectArea> effectArea = effectAreaLookup.get();
        if (effectArea.isEmpty()) {
            return;
        }
        Component question = buildQuestion();
        for (UUID participantUniqueId : participantUniqueIds) {
            Player player = onlinePlayerLookup.apply(participantUniqueId);
            if (player != null && player.isOnline() && effectArea.get().contains(player.getLocation())) {
                questionDeadlineByPlayer.put(participantUniqueId, playAgainDeadline);
                player.sendMessage(question);
            }
        }
    }

    private void followRenamedTeam(String formerTeamName, String newTeamName) {
        Long playAgainDeadline = playAgainDeadlineByWaitingTeamKey.remove(toKey(formerTeamName));
        if (playAgainDeadline != null) {
            playAgainDeadlineByWaitingTeamKey.put(toKey(newTeamName), playAgainDeadline);
        }
    }

    private static String toKey(String teamName) {
        return teamName.toLowerCase(Locale.ROOT);
    }
}
