package de.simplepvparena.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

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

    private List<UUID> shuffleActivePlayers(RoundParticipants roundParticipants) {
        List<UUID> damageOrder = new ArrayList<>(roundParticipants.getAllActivePlayers());
        Collections.shuffle(damageOrder, random);
        return damageOrder;
    }
}
