package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

@NullMarked
public final class SuddenDeathTick {

    private final Random random;

    public SuddenDeathTick(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public Optional<RoundOutcome> apply(RoundParticipants roundParticipants,
                                        double damageAmount,
                                        Consumer<UUID> damageAction) {
        Objects.requireNonNull(roundParticipants, "roundParticipants");
        Objects.requireNonNull(damageAction, "damageAction");
        if (damageAmount <= 0.0D) {
            return Optional.empty();
        }
        for (UUID activePlayerUniqueId : shuffleActivePlayers(roundParticipants)) {
            damageAction.accept(activePlayerUniqueId);
        }
        return roundParticipants.determineOutcome().map(outcome -> outcome.markedAsSuddenDeath());
    }

    /**
     * Damages only the given fighters, for example those who touch the lasers of the sudden death zone. Fighters
     * who are no longer active are skipped.
     */
    public Optional<RoundOutcome> applyTo(RoundParticipants roundParticipants,
                                          Collection<UUID> targets,
                                          Consumer<UUID> damageAction) {
        Objects.requireNonNull(roundParticipants, "roundParticipants");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(damageAction, "damageAction");
        List<UUID> damageOrder = new ArrayList<>();
        for (UUID target : targets) {
            if (roundParticipants.isActive(target)) {
                damageOrder.add(target);
            }
        }
        if (damageOrder.isEmpty()) {
            return Optional.empty();
        }
        Collections.shuffle(damageOrder, random);
        for (UUID target : damageOrder) {
            damageAction.accept(target);
        }
        return roundParticipants.determineOutcome().map(outcome -> outcome.markedAsSuddenDeath());
    }

    private List<UUID> shuffleActivePlayers(RoundParticipants roundParticipants) {
        List<UUID> damageOrder = new ArrayList<>(roundParticipants.getAllActivePlayers());
        Collections.shuffle(damageOrder, random);
        return damageOrder;
    }
}
