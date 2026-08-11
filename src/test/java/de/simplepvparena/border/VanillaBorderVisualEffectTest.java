package de.simplepvparena.border;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaBorderVisualEffectTest {

    @Test
    void coloursEveryConnectivityStateDifferently() {
        assertEquals(Color.fromRGB(255, 60, 60), colourOf(BorderConnectivity.ISOLATED),
                "an isolated column is red");
        assertEquals(Color.fromRGB(255, 235, 40), colourOf(BorderConnectivity.NORMAL),
                "the end of a wall is yellow");
        assertEquals(Color.fromRGB(60, 255, 90), colourOf(BorderConnectivity.WELL_CONNECTED),
                "a column the wall runs through is green");
    }

    @Test
    void givesEveryStateAColourOfItsOwn() {
        Set<Color> colours = new LinkedHashSet<>();
        for (BorderConnectivity connectivity : BorderConnectivity.values()) {
            Particle.DustOptions dust = VanillaBorderVisualEffect.colourOf(connectivity);
            assertNotNull(dust, connectivity + " has no colour");
            colours.add(dust.getColor());
        }

        assertEquals(BorderConnectivity.values().length, colours.size(),
                "two states share a colour and cannot be told apart");
    }

    @Test
    void drawsAWellConnectedColumnInTheGreenOfTheConnectionLines() {
        assertEquals(Color.fromRGB(60, 255, 90), colourOf(BorderConnectivity.WELL_CONNECTED));
    }

    @Test
    void keepsTheThreeColoursFarApartFromEachOther() {
        for (BorderConnectivity first : BorderConnectivity.values()) {
            for (BorderConnectivity second : BorderConnectivity.values()) {
                if (first == second) {
                    continue;
                }
                int distance = channelDistance(colourOf(first), colourOf(second));
                assertTrue(distance >= 150, first + " and " + second + " are only " + distance + " apart");
            }
        }
    }

    @Test
    void drawsAllThreeStatesInTheSameSize() {
        float isolatedSize = VanillaBorderVisualEffect.colourOf(BorderConnectivity.ISOLATED).getSize();
        for (BorderConnectivity connectivity : BorderConnectivity.values()) {
            assertEquals(isolatedSize, VanillaBorderVisualEffect.colourOf(connectivity).getSize(),
                    connectivity + " is drawn in another size");
        }
    }

    private static int channelDistance(Color first, Color second) {
        return Math.abs(first.getRed() - second.getRed())
                + Math.abs(first.getGreen() - second.getGreen())
                + Math.abs(first.getBlue() - second.getBlue());
    }

    private static Color colourOf(BorderConnectivity connectivity) {
        return VanillaBorderVisualEffect.colourOf(connectivity).getColor();
    }
}
