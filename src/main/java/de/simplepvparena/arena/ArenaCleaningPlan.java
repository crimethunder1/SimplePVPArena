package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class ArenaCleaningPlan {

    private final ArenaShape volume;
    private final ArenaBaselineSnapshot baseline;

    private ArenaCleaningPlan(ArenaShape volume, ArenaBaselineSnapshot baseline) {
        this.volume = Objects.requireNonNull(volume, "volume");
        this.baseline = baseline;
    }

    public static ArenaCleaningPlan of(ArenaShape volume, Optional<ArenaBaselineSnapshot> storedBaseline) {
        Objects.requireNonNull(storedBaseline, "storedBaseline");
        findBaselineMismatch(volume, storedBaseline).ifPresent(mismatch -> {
            throw new IllegalStateException(mismatch.replace(TournamentMessages.LINE_BREAK, " "));
        });
        return new ArenaCleaningPlan(volume, storedBaseline.orElse(null));
    }

    public static Optional<String> findBaselineMismatch(ArenaShape volume,
                                                        Optional<ArenaBaselineSnapshot> storedBaseline) {
        return storedBaseline
                .filter(snapshot -> !snapshot.covers(volume))
                .map(snapshot -> Messages.text(ArenaCleaningMessage.BASELINE_MISMATCH,
                        Placeholder.of("baselineArea", snapshot.getShape().describe()),
                        Placeholder.of("area", volume.describe()),
                        Placeholder.of("action", Arena.describeCalculateAction())));
    }

    public ArenaShape getVolume() {
        return volume;
    }

    public ArenaShape getCleanedVolume() {
        return baseline == null ? volume : baseline.getCapturedVolume();
    }

    public ArenaCleaningTarget getTarget() {
        return baseline == null ? ArenaCleaningTarget.EMPTY_ARENA : baseline;
    }

    /**
     * Target of the blocks above the wall top: the blocks recorded there with the baseline, air everywhere else.
     */
    public ArenaHeadroom getHeadroomTarget() {
        return baseline == null ? ArenaHeadroom.ASSUMED_EMPTY : baseline.getHeadroom();
    }

    public boolean restoresABaseline() {
        return baseline != null;
    }

    public Optional<ArenaBaselineSnapshot> findBaseline() {
        return Optional.ofNullable(baseline);
    }

    public long getBlockCount() {
        return getCleanedVolume().getBlockCount();
    }
}
