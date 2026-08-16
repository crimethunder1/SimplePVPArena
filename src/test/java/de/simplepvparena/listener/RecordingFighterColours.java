package de.simplepvparena.listener;

import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchTeamColour;
import de.simplepvparena.match.MatchTeamColours;
import de.simplepvparena.match.TeamColourSettings;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class RecordingFighterColours implements FighterColours {

    private final Map<String, List<String>> fightersByScoreboardTeam = new LinkedHashMap<>();
    private final Map<String, NamedTextColor> colourByScoreboardTeam = new LinkedHashMap<>();

    private TeamColourSettings settings = TeamColourSettings.defaults();
    private int colourCallCount;
    private int clearCallCount;

    @Override
    public void applySettings(TeamColourSettings settings) {
        this.settings = settings;
    }

    @Override
    public void colourFighters(Match match) {
        colourCallCount++;
        clearBoard();
        for (MatchTeamColour matchTeamColour : MatchTeamColours.of(match, settings)) {
            fightersByScoreboardTeam.put(matchTeamColour.scoreboardTeamName(), matchTeamColour.fighterNames());
            colourByScoreboardTeam.put(matchTeamColour.scoreboardTeamName(), matchTeamColour.colour());
        }
    }

    @Override
    public void clearColours() {
        clearCallCount++;
        clearBoard();
    }

    Optional<NamedTextColor> findColourOf(String playerName) {
        return fightersByScoreboardTeam.entrySet().stream()
                .filter(scoreboardTeam -> scoreboardTeam.getValue().contains(playerName))
                .findFirst()
                .map(scoreboardTeam -> colourByScoreboardTeam.get(scoreboardTeam.getKey()));
    }

    List<String> colouredFighters() {
        return fightersByScoreboardTeam.values().stream().flatMap(List::stream).toList();
    }

    int getColourCallCount() {
        return colourCallCount;
    }

    int getClearCallCount() {
        return clearCallCount;
    }

    private void clearBoard() {
        fightersByScoreboardTeam.clear();
        colourByScoreboardTeam.clear();
    }
}
