package de.simplepvparena.support;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class InMemoryScoreboard {

    private final Map<String, FakeTeam> teams = new LinkedHashMap<>();
    private final Map<String, FakeObjective> objectives = new LinkedHashMap<>();
    private final Scoreboard scoreboard;

    private int teamWriteCount;

    public InMemoryScoreboard() {
        scoreboard = SharedBukkitServer.proxy(Scoreboard.class, (method, arguments) -> switch (method.getName()) {
            case "getTeam" -> Optional.ofNullable(teams.get((String) arguments[0])).map(team -> team.proxy)
                    .orElse(null);
            case "registerNewTeam" -> registerTeam((String) arguments[0]);
            case "getTeams" -> teams.values().stream().map(team -> team.proxy).collect(LinkedHashSet::new,
                    Set::add, Set::addAll);
            case "getEntryTeam" -> teams.values().stream()
                    .filter(team -> team.entries.contains((String) arguments[0]))
                    .map(team -> team.proxy)
                    .findFirst()
                    .orElse(null);
            case "registerNewObjective" -> registerObjective((String) arguments[0], arguments[2]);
            case "getObjective" -> arguments[0] instanceof DisplaySlot slot
                    ? findObjectiveIn(slot).map(objective -> objective.proxy).orElse(null)
                    : Optional.ofNullable(objectives.get((String) arguments[0])).map(objective -> objective.proxy)
                    .orElse(null);
            case "resetScores" -> {
                for (FakeObjective objective : objectives.values()) {
                    objective.scores.remove((String) arguments[0]);
                }
                yield null;
            }
            default -> throw new UnsupportedOperationException("Scoreboard." + method.getName());
        });
    }

    public Scoreboard scoreboard() {
        return scoreboard;
    }

    public List<String> sidebarLines() {
        return findObjectiveIn(DisplaySlot.SIDEBAR)
                .map(objective -> objective.scores.values().stream()
                        .sorted(Comparator.comparingInt((FakeScore score) -> score.value).reversed())
                        .map(score -> plain(score.customName))
                        .toList())
                .orElse(List.of());
    }

    public List<Component> sidebarComponents() {
        return findObjectiveIn(DisplaySlot.SIDEBAR)
                .map(objective -> objective.scores.values().stream()
                        .sorted(Comparator.comparingInt((FakeScore score) -> score.value).reversed())
                        .map(score -> score.customName)
                        .toList())
                .orElse(List.of());
    }

    public Optional<String> sidebarTitle() {
        return findObjectiveIn(DisplaySlot.SIDEBAR).map(objective -> plain(objective.displayName));
    }

    public Optional<NumberFormat> sidebarNumberFormat() {
        return findObjectiveIn(DisplaySlot.SIDEBAR).map(objective -> objective.numberFormat);
    }

    public List<String> describeTeams() {
        List<String> descriptions = new ArrayList<>();
        for (FakeTeam team : teams.values()) {
            descriptions.add(team.name + " | " + team.colour + " | " + plain(team.displayName) + " | friendly fire "
                    + team.friendlyFire + " | invisibles " + team.seesInvisibles + " | "
                    + team.options.get(Team.Option.NAME_TAG_VISIBILITY) + " | " + new TreeSet<>(team.entries));
        }
        return descriptions;
    }

    public List<String> describeTeamDetails() {
        List<String> descriptions = new ArrayList<>();
        for (FakeTeam team : teams.values()) {
            descriptions.add(team.name + " | prefix " + plain(team.prefix) + " | suffix " + plain(team.suffix)
                    + " | " + team.options);
        }
        return descriptions;
    }

    public Set<String> objectiveNames() {
        return Set.copyOf(objectives.keySet());
    }

    public int teamWriteCount() {
        return teamWriteCount;
    }

    private Optional<FakeObjective> findObjectiveIn(DisplaySlot slot) {
        return objectives.values().stream().filter(objective -> objective.slot == slot).findFirst();
    }

    private Team registerTeam(String teamName) {
        if (teams.containsKey(teamName)) {
            throw new IllegalArgumentException("team " + teamName + " is already registered");
        }
        FakeTeam team = new FakeTeam(teamName);
        teams.put(teamName, team);
        return team.proxy;
    }

    private Objective registerObjective(String objectiveName, Object displayName) {
        if (objectives.containsKey(objectiveName)) {
            throw new IllegalArgumentException("objective " + objectiveName + " is already registered");
        }
        FakeObjective objective = new FakeObjective(objectiveName, (Component) displayName);
        objectives.put(objectiveName, objective);
        return objective.proxy;
    }

    private static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    private final class FakeTeam {

        private final String name;
        private final Set<String> entries = new LinkedHashSet<>();
        private final Map<Team.Option, Team.OptionStatus> options = new EnumMap<>(Team.Option.class);
        private final Team proxy;

        private NamedTextColor colour;
        private Component displayName;
        private Component prefix = Component.empty();
        private Component suffix = Component.empty();
        private boolean friendlyFire = true;
        private boolean seesInvisibles = true;

        private FakeTeam(String name) {
            this.name = name;
            this.displayName = Component.text(name);
            for (Team.Option option : Team.Option.values()) {
                options.put(option, Team.OptionStatus.ALWAYS);
            }
            this.proxy = SharedBukkitServer.proxy(Team.class, (method, arguments) -> switch (method.getName()) {
                case "getName" -> name;
                case "hasColor" -> colour != null;
                case "color" -> {
                    if (arguments == null || arguments.length == 0) {
                        yield colour;
                    }
                    colour = (NamedTextColor) arguments[0];
                    yield written();
                }
                case "displayName" -> {
                    if (arguments == null || arguments.length == 0) {
                        yield displayName;
                    }
                    displayName = (Component) arguments[0];
                    yield written();
                }
                case "prefix" -> {
                    if (arguments == null || arguments.length == 0) {
                        yield prefix;
                    }
                    prefix = (Component) arguments[0];
                    yield written();
                }
                case "suffix" -> {
                    if (arguments == null || arguments.length == 0) {
                        yield suffix;
                    }
                    suffix = (Component) arguments[0];
                    yield written();
                }
                case "allowFriendlyFire" -> friendlyFire;
                case "setAllowFriendlyFire" -> {
                    friendlyFire = (Boolean) arguments[0];
                    yield written();
                }
                case "canSeeFriendlyInvisibles" -> seesInvisibles;
                case "setCanSeeFriendlyInvisibles" -> {
                    seesInvisibles = (Boolean) arguments[0];
                    yield written();
                }
                case "getOption" -> options.get((Team.Option) arguments[0]);
                case "setOption" -> {
                    options.put((Team.Option) arguments[0], (Team.OptionStatus) arguments[1]);
                    yield written();
                }
                case "getEntries" -> Set.copyOf(entries);
                case "hasEntry" -> entries.contains((String) arguments[0]);
                case "addEntry" -> {
                    for (FakeTeam otherTeam : teams.values()) {
                        otherTeam.entries.remove((String) arguments[0]);
                    }
                    entries.add((String) arguments[0]);
                    yield written();
                }
                case "removeEntry" -> {
                    teamWriteCount++;
                    yield entries.remove((String) arguments[0]);
                }
                case "unregister" -> {
                    teams.remove(name);
                    yield written();
                }
                default -> throw new UnsupportedOperationException("Team." + method.getName());
            });
        }

        private Object written() {
            teamWriteCount++;
            return null;
        }
    }

    private static final class FakeObjective {

        private final Map<String, FakeScore> scores = new LinkedHashMap<>();
        private final Objective proxy;

        private Component displayName;
        private DisplaySlot slot;
        private NumberFormat numberFormat;

        private FakeObjective(String name, Component displayName) {
            this.displayName = displayName;
            this.proxy = SharedBukkitServer.proxy(Objective.class, (method, arguments) -> switch (method.getName()) {
                case "getName" -> name;
                case "displayName" -> {
                    if (arguments == null || arguments.length == 0) {
                        yield this.displayName;
                    }
                    this.displayName = (Component) arguments[0];
                    yield null;
                }
                case "setDisplaySlot" -> {
                    slot = (DisplaySlot) arguments[0];
                    yield null;
                }
                case "getDisplaySlot" -> slot;
                case "numberFormat" -> {
                    if (arguments == null || arguments.length == 0) {
                        yield numberFormat;
                    }
                    numberFormat = (NumberFormat) arguments[0];
                    yield null;
                }
                case "getScore" -> scoreOf((String) arguments[0]);
                default -> throw new UnsupportedOperationException("Objective." + method.getName());
            });
        }

        private Score scoreOf(String entry) {
            return SharedBukkitServer.proxy(Score.class, (method, arguments) -> switch (method.getName()) {
                case "getEntry" -> entry;
                case "isScoreSet" -> scores.containsKey(entry);
                case "getScore" -> scores.containsKey(entry) ? scores.get(entry).value : 0;
                case "setScore" -> {
                    scores.computeIfAbsent(entry, unused -> new FakeScore()).value = (Integer) arguments[0];
                    yield null;
                }
                case "customName" -> {
                    if (arguments == null || arguments.length == 0) {
                        yield scores.containsKey(entry) ? scores.get(entry).customName : null;
                    }
                    if (!scores.containsKey(entry)) {
                        throw new IllegalStateException("the score of " + entry + " is not set");
                    }
                    scores.get(entry).customName = (Component) arguments[0];
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Score." + method.getName());
            });
        }
    }

    private static final class FakeScore {

        private int value;
        private Component customName;
    }
}
