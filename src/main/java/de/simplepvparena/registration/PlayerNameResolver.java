package de.simplepvparena.registration;

import de.simplepvparena.team.TeamMember;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@NullMarked
@FunctionalInterface
public interface PlayerNameResolver {

    CompletableFuture<Optional<TeamMember>> resolve(String playerName);
}
