package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface FighterColours {

    void applySettings(TeamColourSettings settings);

    void colourFighters(Match match);

    void clearColours();
}
