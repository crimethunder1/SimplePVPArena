package de.simplepvparena.gui;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public interface BoardDataReset {

    BoardDataReset NONE = new BoardDataReset() {
        @Override
        public String resetStandings() {
            return "";
        }

        @Override
        public Optional<String> findBracketRejection() {
            return Optional.empty();
        }

        @Override
        public String resetBracket() {
            return "";
        }
    };

    String resetStandings();

    Optional<String> findBracketRejection();

    String resetBracket();
}
