package de.simplepvparena.voice;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.RoundParticipants;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

@NullMarked
public record VoiceRoster(Map<UUID, String> fighterTeams, Set<UUID> eliminatedFighters, Set<UUID> bypassPlayers) {

    public static final VoiceRoster EMPTY = new VoiceRoster(Map.of(), Set.of(), Set.of());

    public VoiceRoster {
        fighterTeams = Map.copyOf(fighterTeams);
        eliminatedFighters = Set.copyOf(eliminatedFighters);
        bypassPlayers = Set.copyOf(bypassPlayers);
    }

    public static VoiceRoster of(Optional<Match> activeMatch, BiPredicate<Match, UUID> leftOutOfTheFighters,
                                 Set<UUID> bypassPlayers) {
        if (activeMatch.isEmpty() || !isolates(activeMatch.get().getState())) {
            return EMPTY;
        }
        Match match = activeMatch.get();
        Map<UUID, String> fighterTeams = new HashMap<>();
        for (Team team : match.getTeams()) {
            for (TeamMember member : team.getMembers()) {
                if (!leftOutOfTheFighters.test(match, member.uniqueId())) {
                    fighterTeams.put(member.uniqueId(), team.getTeamName());
                }
            }
        }
        Set<UUID> eliminatedFighters = new HashSet<>();
        Optional<RoundParticipants> participants = match.getRoundParticipants();
        if (match.getState() == MatchState.RUNNING && participants.isPresent()) {
            for (UUID fighter : fighterTeams.keySet()) {
                if (!participants.get().isActive(fighter)) {
                    eliminatedFighters.add(fighter);
                }
            }
        }
        return new VoiceRoster(fighterTeams, eliminatedFighters, bypassPlayers);
    }

    private static boolean isolates(MatchState state) {
        return state == MatchState.PREPARING || state == MatchState.RUNNING || state == MatchState.BETWEEN_ROUNDS;
    }

    public boolean isEmpty() {
        return fighterTeams.isEmpty();
    }

    public boolean isFighter(@Nullable UUID player) {
        return player != null && fighterTeams.containsKey(player);
    }

    public boolean isEliminated(@Nullable UUID player) {
        return player != null && eliminatedFighters.contains(player);
    }

    public boolean canHear(@Nullable UUID speaker, @Nullable UUID listener) {
        if (speaker == null || listener == null || speaker.equals(listener)) {
            return true;
        }
        String listenerTeam = fighterTeams.get(listener);
        if (listenerTeam == null) {
            return true;
        }
        boolean listenerOut = eliminatedFighters.contains(listener);
        String speakerTeam = fighterTeams.get(speaker);
        if (speakerTeam == null) {
            return listenerOut || bypassPlayers.contains(speaker);
        }
        if (speakerTeam.equals(listenerTeam)) {
            return true;
        }
        return !listenerOut && !eliminatedFighters.contains(speaker);
    }
}
