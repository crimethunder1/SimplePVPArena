package de.simplepvparena.team;

import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamColourTest {

    @Test
    void listsTheSixteenColoursInTheirFixedOrder() {
        assertEquals(List.of(DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN, DyeColor.YELLOW, DyeColor.ORANGE,
                DyeColor.PURPLE, DyeColor.LIGHT_BLUE, DyeColor.PINK, DyeColor.WHITE, DyeColor.GRAY, DyeColor.BLACK,
                DyeColor.BROWN, DyeColor.CYAN, DyeColor.LIME, DyeColor.MAGENTA, DyeColor.LIGHT_GRAY),
                Arrays.stream(TeamColour.values()).map(TeamColour::getDyeColour).toList());
    }

    @Test
    void usesEveryBedAndWoolColourOnce() {
        assertEquals(EnumSet.allOf(DyeColor.class), EnumSet.copyOf(Arrays.stream(TeamColour.values())
                .map(TeamColour::getDyeColour).toList()));
    }

    @Test
    void givesEveryColourAReadableTextColour() {
        assertEquals(List.of(NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.DARK_GREEN,
                NamedTextColor.YELLOW, NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE, NamedTextColor.AQUA,
                NamedTextColor.LIGHT_PURPLE, NamedTextColor.WHITE, NamedTextColor.DARK_GRAY, NamedTextColor.DARK_GRAY,
                NamedTextColor.DARK_RED, NamedTextColor.DARK_AQUA, NamedTextColor.GREEN, NamedTextColor.LIGHT_PURPLE,
                NamedTextColor.GRAY), Arrays.stream(TeamColour.values()).map(TeamColour::getTextColour).toList());
        for (TeamColour colour : TeamColour.values()) {
            assertFalse(colour.getTextColour() == NamedTextColor.BLACK
                    || colour.getTextColour() == NamedTextColor.DARK_BLUE, colour + " is hard to read");
        }
    }

    @Test
    void writesAndReadsTheStorageNames() {
        assertEquals("light-blue", TeamColour.LIGHT_BLUE.getStorageName());
        assertEquals("red", TeamColour.RED.getStorageName());
        for (TeamColour colour : TeamColour.values()) {
            assertEquals(Optional.of(colour), TeamColour.ofStorageName(colour.getStorageName()));
        }
        assertEquals(Optional.empty(), TeamColour.ofStorageName("LIGHT_BLUE"));
        assertEquals(Optional.empty(), TeamColour.ofStorageName("violet"));
        assertEquals(Optional.empty(), TeamColour.ofStorageName(null));
    }

    @Test
    @UseLanguage("en")
    void namesTheColoursInEnglishAsAllowedTeamNames() {
        assertEquals(List.of("Red", "Blue", "Green", "Yellow", "Orange", "Purple", "LightBlue", "Pink", "White",
                "Gray", "Black", "Brown", "Cyan", "Lime", "Magenta", "LightGray"), displayNames());
        displayNames().forEach(name -> assertTrue(TeamRegistry.isAllowedTeamName(name), name));
    }

    @Test
    @UseLanguage("de")
    void namesTheColoursInGermanAsAllowedTeamNames() {
        assertEquals(List.of("Rot", "Blau", "Grün", "Gelb", "Orange", "Lila", "Hellblau", "Rosa", "Weiß", "Grau",
                "Schwarz", "Braun", "Cyan", "Lindgrün", "Magenta", "Hellgrau"), displayNames());
        displayNames().forEach(name -> assertTrue(TeamRegistry.isAllowedTeamName(name), name));
    }

    private static List<String> displayNames() {
        return Arrays.stream(TeamColour.values()).map(TeamColour::getDisplayName).toList();
    }
}
