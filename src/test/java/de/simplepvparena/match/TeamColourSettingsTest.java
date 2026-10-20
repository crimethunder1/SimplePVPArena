package de.simplepvparena.match;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamColourSettingsTest {

    @Test
    void shipsTheLightRedAndTheLightBlueOfTheGame() {
        TeamColourSettings settings = TeamColourSettings.defaults();

        assertEquals(NamedTextColor.RED, settings.firstTeamColour());
        assertEquals(NamedTextColor.AQUA, settings.secondTeamColour());
        assertFalse(settings.firstTeamColour().equals(settings.secondTeamColour()),
                "two teams in one colour would be no colouring at all");
    }

    @Test
    void readsAColourNameInEveryFormItIsWrittenIn() {
        assertEquals(NamedTextColor.RED, TeamColourSettings.colourOfName("red").orElseThrow());
        assertEquals(NamedTextColor.RED, TeamColourSettings.colourOfName("RED").orElseThrow());
        assertEquals(NamedTextColor.RED, TeamColourSettings.colourOfName("  Red  ").orElseThrow());
        assertEquals(NamedTextColor.RED, TeamColourSettings.colourOfName("minecraft:red").orElseThrow());
        assertEquals(NamedTextColor.LIGHT_PURPLE,
                TeamColourSettings.colourOfName("light_purple").orElseThrow());
        assertEquals(NamedTextColor.LIGHT_PURPLE,
                TeamColourSettings.colourOfName("LIGHT-PURPLE").orElseThrow());
        assertEquals(NamedTextColor.LIGHT_PURPLE,
                TeamColourSettings.colourOfName("light purple").orElseThrow());
    }

    @Test
    void answersANameThatIsNoColourWithNothing() {
        assertTrue(TeamColourSettings.colourOfName("hellrot").isEmpty());
        assertTrue(TeamColourSettings.colourOfName("#ff0000").isEmpty(), "a free colour is none of the sixteen");
        assertTrue(TeamColourSettings.colourOfName("").isEmpty());
        assertTrue(TeamColourSettings.colourOfName(null).isEmpty());
    }

    @Test
    void writesEveryColourUnderTheNameItIsReadBackWith() {
        for (String colourName : TeamColourSettings.knownColourNames()) {
            NamedTextColor colour = TeamColourSettings.colourOfName(colourName).orElseThrow();
            assertEquals(colourName, TeamColourSettings.nameOf(colour));
        }
    }

    @Test
    void knowsTheSixteenColoursOfTheGame() {
        assertEquals(16, TeamColourSettings.knownColourNames().size());
        assertTrue(TeamColourSettings.knownColourNames()
                .containsAll(List.of("red", "aqua", "dark_red", "blue", "light_purple")));
    }

    @Test
    void namesTheColoursInAlphabeticalOrderInAListThatCannotBeChanged() {
        List<String> colourNames = TeamColourSettings.knownColourNames();

        assertEquals(List.of("aqua", "black", "blue", "dark_aqua", "dark_blue", "dark_gray", "dark_green",
                "dark_purple", "dark_red", "gold", "gray", "green", "light_purple", "red", "white", "yellow"),
                colourNames);
        assertThrows(UnsupportedOperationException.class, () -> colourNames.add("pink"));
    }
}
