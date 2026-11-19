package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@NullMarked
public record ArenaVolumeUpdate(ArenaShape volume, ArenaVolumeUpdate.Origin origin,
                                @Nullable ArenaShape droppedMarkedVolume) {

    public enum Origin {

        BORDER_WALL,

        MARKS_TAKEN_OVER,

        MARKS_KEPT
    }

    public static ArenaVolumeUpdate marksTakenOver(ArenaShape volume) {
        return new ArenaVolumeUpdate(volume, Origin.MARKS_TAKEN_OVER, null);
    }

    public static ArenaVolumeUpdate marksKept(ArenaShape volume) {
        return new ArenaVolumeUpdate(volume, Origin.MARKS_KEPT, null);
    }

    public static ArenaVolumeUpdate readOffTheBorderWall(ArenaShape volume,
                                                         @Nullable ArenaShape droppedMarkedVolume) {
        return new ArenaVolumeUpdate(volume, Origin.BORDER_WALL, droppedMarkedVolume);
    }

    public boolean isMarked() {
        return origin != Origin.BORDER_WALL;
    }

    public Optional<ArenaShape> findDroppedMarkedVolume() {
        return Optional.ofNullable(droppedMarkedVolume);
    }
}
