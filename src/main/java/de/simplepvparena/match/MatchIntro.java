package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MatchIntro {

    MatchIntro NONE = new MatchIntro() {
        @Override
        public int play(Match match) {
            return 0;
        }

        @Override
        public void stop() {
        }
    };

    int play(Match match);

    void stop();
}
