package de.simplepvparena.visual;

import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfiguredVisualsTest {

    private static final String TOTEM_PATH = CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH;
    private static final String THREE_PATH = CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH;

    private final List<String> warnings = new ArrayList<>();
    private final FakeItemStack totem = new FakeItemStack("custom totem");
    private final RecordingCustomVisuals customVisuals = new RecordingCustomVisuals()
            .withFontImage("simplepvparena:countdown_3", "")
            .withItem("simplepvparena:totem", totem);
    private final ConfiguredVisuals configuredVisuals = new ConfiguredVisuals(customVisuals, warnings::add);

    @Test
    void startsWithTheShippedSettingsAndHandsOutTheChosenVisuals() {
        assertEquals(CustomVisualSettings.defaults(), configuredVisuals.getSettings());
        assertSame(customVisuals, configuredVisuals.getCustomVisuals());
    }

    @Test
    void handsOutAKnownFontImageAndACopyOfAKnownItem() {
        assertEquals(Optional.of(""), configuredVisuals.findFontImage(THREE_PATH, "simplepvparena:countdown_3"));

        ItemStack found = configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:totem").orElseThrow();
        assertEquals(totem, found);
        assertNotSame(totem, found);
        assertEquals(List.of(), warnings);
    }

    @Test
    void anEmptyIdentifierFallsBackWithoutAskingItemsAdder() {
        assertEquals(Optional.empty(), configuredVisuals.findFontImage(THREE_PATH, ""));
        assertEquals(Optional.empty(), configuredVisuals.findItem(TOTEM_PATH, ""));

        assertEquals(List.of(), customVisuals.lookups());
        assertEquals(List.of(), warnings);
    }

    @Test
    void theSwitchedOffSettingFallsBackForEveryIdentifierWithoutAskingItemsAdder() {
        configuredVisuals.applySettings(new CustomVisualSettings(false, "simplepvparena:countdown_3", "", "", "",
                "simplepvparena:totem"));

        assertEquals(Optional.empty(), configuredVisuals.findFontImage(THREE_PATH, "simplepvparena:countdown_3"));
        assertEquals(Optional.empty(), configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:totem"));
        assertEquals(List.of(), customVisuals.lookups());

        configuredVisuals.applySettings(CustomVisualSettings.defaults());
        assertEquals(Optional.of(""), configuredVisuals.findFontImage(THREE_PATH, "simplepvparena:countdown_3"));
    }

    @Test
    void anUnknownIdentifierFallsBackAndIsWarnedAboutOnlyOnce() {
        for (int lookup = 0; lookup < 3; lookup++) {
            assertEquals(Optional.empty(), configuredVisuals.findFontImage(THREE_PATH, "simplepvparena:missing"));
            assertEquals(Optional.empty(), configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:lost_totem"));
        }

        assertEquals(List.of(
                "ItemsAdder has no font image simplepvparena:missing, set in items-adder.countdown-images.three of "
                        + "config.yml, the own title of the plugin is shown instead",
                "ItemsAdder has no item simplepvparena:lost_totem, set in items-adder.totem-item of config.yml, the "
                        + "own effect of the plugin is used instead"), warnings);
    }

    @Test
    void aFailingLookupFallsBackAndIsWarnedAboutOnlyOnceWithTheReason() {
        customVisuals.failingFor("simplepvparena:totem");

        for (int lookup = 0; lookup < 3; lookup++) {
            assertEquals(Optional.empty(), configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:totem"));
        }

        assertEquals(List.of("ItemsAdder could not hand out simplepvparena:totem, set in items-adder.totem-item of "
                + "config.yml (ItemsAdder is broken for simplepvparena:totem), the own title or effect of the plugin "
                + "is used instead"), warnings);
    }

    @Test
    void aMissingClassOfItemsAdderFallsBackLikeAnyOtherFailure() {
        ConfiguredVisuals brokenVisuals = new ConfiguredVisuals(new CustomVisuals() {

            @Override
            public Optional<String> findFontImage(String identifier) {
                throw new NoClassDefFoundError("dev/lone/itemsadder/api/FontImages/FontImageWrapper");
            }

            @Override
            public Optional<ItemStack> findItem(String identifier) {
                return null;
            }

            @Override
            public boolean isReady() {
                return true;
            }
        }, warnings::add);

        assertEquals(Optional.empty(), brokenVisuals.findFontImage(THREE_PATH, "simplepvparena:countdown_3"));
        assertEquals(Optional.empty(), brokenVisuals.findItem(TOTEM_PATH, "simplepvparena:totem"));
        assertEquals(2, warnings.size(), warnings.toString());
    }

    @Test
    void staysQuietWhileItemsAdderHasNotLoadedItsContentYet() {
        customVisuals.setReady(false);

        assertEquals(Optional.empty(), configuredVisuals.findFontImage(THREE_PATH, "simplepvparena:countdown_3"));
        assertEquals(Optional.empty(), configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:totem"));
        assertEquals(List.of(), warnings);

        customVisuals.setReady(true);
        assertEquals(Optional.of(""), configuredVisuals.findFontImage(THREE_PATH, "simplepvparena:countdown_3"));
        assertEquals(List.of(), warnings);
    }

    @Test
    void anIdentifierStaysWarnedAboutAfterAReload() {
        configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:lost_totem");
        configuredVisuals.applySettings(new CustomVisualSettings(true, "", "", "", "", "simplepvparena:lost_totem"));
        configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:lost_totem");

        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void mapsTheLastThreeSecondsAndTheFightToTheirOwnIdentifiers() {
        RecordingCustomVisuals everyImage = new RecordingCustomVisuals()
                .withFontImage("three", "3 image").withFontImage("two", "2 image")
                .withFontImage("one", "1 image").withFontImage("fight", "fight image");
        ConfiguredVisuals countdownVisuals = new ConfiguredVisuals(everyImage, warnings::add);
        countdownVisuals.applySettings(new CustomVisualSettings(true, "three", "two", "one", "fight", ""));

        assertEquals(Optional.of("3 image"), countdownVisuals.findImage(3));
        assertEquals(Optional.of("2 image"), countdownVisuals.findImage(2));
        assertEquals(Optional.of("1 image"), countdownVisuals.findImage(1));
        assertEquals(Optional.of("fight image"), countdownVisuals.findImage(0));
        assertEquals(Optional.empty(), countdownVisuals.findImage(4));
        assertEquals(Optional.empty(), countdownVisuals.findImage(10));
        assertEquals(List.of("font image three", "font image two", "font image one", "font image fight"),
                everyImage.lookups(), "a second above three asks ItemsAdder nothing");
    }

    @Test
    void withoutItemsAdderNothingIsLookedUpOrWarnedAboutEvenWithIdentifiers() {
        ConfiguredVisuals vanilla = new ConfiguredVisuals(new VanillaVisuals(), warnings::add);
        vanilla.applySettings(new CustomVisualSettings(true, "simplepvparena:countdown_3", "", "", "",
                "simplepvparena:totem"));

        assertEquals(Optional.empty(), vanilla.findImage(3));
        assertEquals(Optional.empty(), vanilla.findItem(TOTEM_PATH, "simplepvparena:totem"));
        assertEquals(List.of(), warnings);
    }

    @Test
    @UseLanguage("de")
    void warnsInGerman() {
        configuredVisuals.findFontImage(THREE_PATH, "simplepvparena:missing");
        configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:lost_totem");
        customVisuals.failingFor("simplepvparena:totem");
        configuredVisuals.findItem(TOTEM_PATH, "simplepvparena:totem");

        assertEquals(List.of(
                "ItemsAdder kennt kein Schriftbild simplepvparena:missing aus items-adder.countdown-images.three in "
                        + "config.yml, stattdessen erscheint der eigene Titel des Plugins",
                "ItemsAdder kennt kein Item simplepvparena:lost_totem aus items-adder.totem-item in config.yml, "
                        + "stattdessen gilt der eigene Effekt des Plugins",
                "ItemsAdder konnte simplepvparena:totem aus items-adder.totem-item in config.yml nicht liefern "
                        + "(ItemsAdder is broken for simplepvparena:totem), stattdessen gilt der eigene Titel oder "
                        + "Effekt des Plugins"), warnings);
    }
}
