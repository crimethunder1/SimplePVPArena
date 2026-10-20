package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Arena {

    private ArenaShape shape;
    private PositionSnapshot firstTeamSpawn;
    private PositionSnapshot secondTeamSpawn;
    private PositionSnapshot tribuneSpawn;

    public void setShape(ArenaShape shape) {
        this.shape = shape;
    }

    public Optional<ArenaShape> getShape() {
        return Optional.ofNullable(shape);
    }

    public boolean hasShape() {
        return shape != null;
    }

    public boolean contains(Location location) {
        return shape != null && shape.contains(location);
    }

    public boolean isFullyConfigured() {
        return shape != null && tribuneSpawn != null;
    }

    public List<String> getMissingConfiguration() {
        List<String> missingParts = new ArrayList<>();
        if (shape == null) {
            missingParts.add(Messages.text(ArenaSetupMessage.MISSING_FIGHT_AREA,
                    Placeholder.of("action", describeCalculateAction())));
        }
        if (tribuneSpawn == null) {
            missingParts.add(Messages.text(ArenaSetupMessage.MISSING_TRIBUNE_SPAWN,
                    Placeholder.of("action", describeTribuneSpawnAction())));
        }
        return missingParts;
    }

    public static String describeCalculateAction() {
        return describeArenaMenuTile(ArenaMenuMessage.CALCULATE_TITLE);
    }

    public static String describeDiagnoseAction() {
        return describeArenaMenuTile(ArenaMenuMessage.DIAGNOSE_TITLE);
    }

    public static String describeTribuneSpawnAction() {
        return describeArenaMenuTile(ArenaMenuMessage.TRIBUNE_SPAWN_TITLE);
    }

    public static String describeMarkHeightAction() {
        return describeArenaMenuTile(ArenaMenuMessage.HEIGHT_MARKER_TITLE);
    }

    public static String describeCleanAction() {
        return describeArenaMenuTile(ArenaMenuMessage.CLEAN_TITLE);
    }

    static String describeArenaMenuTile(MessageKey tileTitle) {
        return Messages.text(ArenaSetupMessage.ARENA_MENU_REFERENCE, Placeholder.of("tile", Messages.text(tileTitle)));
    }

    public Optional<String> setSpawn(ArenaSpawnType spawnType, PositionSnapshot position) {
        Optional<String> rejection = findSpawnPlacementRejection(spawnType, position);
        if (rejection.isPresent()) {
            return rejection;
        }
        switch (spawnType) {
            case FIRST_TEAM -> setFirstTeamSpawn(position);
            case SECOND_TEAM -> setSecondTeamSpawn(position);
            case TRIBUNE -> setTribuneSpawn(position);
        }
        return Optional.empty();
    }

    public Optional<String> findSpawnPlacementRejection(ArenaSpawnType spawnType, PositionSnapshot position) {
        if (fitsShape(spawnType, position)) {
            return Optional.empty();
        }
        return Optional.of(describeMisplacedSpawn(spawnType, position) + TournamentMessages.LINE_BREAK
                + Messages.text(ArenaSetupMessage.SPAWN_SET_AGAIN,
                        Placeholder.of("placement", spawnType.getPlacementHint())));
    }

    public List<String> findSpawnPlacementWarnings() {
        if (shape == null) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        for (ArenaSpawnType spawnType : ArenaSpawnType.values()) {
            getConfiguredSpawn(spawnType)
                    .filter(spawn -> !fitsShape(spawnType, spawn))
                    .ifPresent(spawn -> warnings.add(describeMisplacedSpawn(spawnType, spawn)
                            + TournamentMessages.LINE_BREAK + Messages.text(ArenaSetupMessage.SPAWN_CLICK_TILE_AGAIN,
                                    Placeholder.of("placement", spawnType.getPlacementHint()))));
        }
        return warnings;
    }

    public void setFirstTeamSpawn(PositionSnapshot position) {
        firstTeamSpawn = position;
    }

    public void setSecondTeamSpawn(PositionSnapshot position) {
        secondTeamSpawn = position;
    }

    public void setTribuneSpawn(PositionSnapshot position) {
        tribuneSpawn = position;
    }

    public Optional<PositionSnapshot> getConfiguredFirstTeamSpawn() {
        return Optional.ofNullable(firstTeamSpawn);
    }

    public Optional<PositionSnapshot> getConfiguredSecondTeamSpawn() {
        return Optional.ofNullable(secondTeamSpawn);
    }

    public Optional<PositionSnapshot> getConfiguredTribuneSpawn() {
        return Optional.ofNullable(tribuneSpawn);
    }

    public Optional<PositionSnapshot> getConfiguredSpawn(ArenaSpawnType spawnType) {
        return switch (spawnType) {
            case FIRST_TEAM -> getConfiguredFirstTeamSpawn();
            case SECOND_TEAM -> getConfiguredSecondTeamSpawn();
            case TRIBUNE -> getConfiguredTribuneSpawn();
        };
    }

    public Optional<PositionSnapshot> getFirstTeamSpawn() {
        return getConfiguredFirstTeamSpawn().or(this::getShapeCenter);
    }

    public Optional<PositionSnapshot> getSecondTeamSpawn() {
        return getConfiguredSecondTeamSpawn().or(this::getShapeCenter);
    }

    public Optional<PositionSnapshot> getTribuneSpawn() {
        return getConfiguredTribuneSpawn();
    }

    public Optional<PositionSnapshot> getSpectatorFlightSpawn() {
        return getShapeCenter();
    }

    public void clearConfiguration() {
        shape = null;
        firstTeamSpawn = null;
        secondTeamSpawn = null;
        tribuneSpawn = null;
    }

    private Optional<PositionSnapshot> getShapeCenter() {
        return getShape().map(arenaShape -> arenaShape.getCenterPosition());
    }

    private boolean fitsShape(ArenaSpawnType spawnType, PositionSnapshot position) {
        if (shape == null) {
            return true;
        }
        return shape.contains(position.worldName(), position.x(), position.z())
                == spawnType.belongsIntoFightArea();
    }

    private String describeMisplacedSpawn(ArenaSpawnType spawnType, PositionSnapshot position) {
        MessageKey sentence = spawnType.belongsIntoFightArea()
                ? ArenaSetupMessage.SPAWN_MUST_LIE_INSIDE : ArenaSetupMessage.SPAWN_MUST_LIE_OUTSIDE;
        return Messages.text(sentence, Placeholder.of("spawn", spawnType.getDisplayName()),
                Placeholder.of("position", position.describe()));
    }
}
