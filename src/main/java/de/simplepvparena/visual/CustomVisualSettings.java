package de.simplepvparena.visual;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record CustomVisualSettings(boolean enabled,
                                   String threeImageIdentifier,
                                   String twoImageIdentifier,
                                   String oneImageIdentifier,
                                   String fightImageIdentifier,
                                   String totemItemIdentifier,
                                   String stationItemIdentifier,
                                   String stationTitleImageIdentifier,
                                   String boardTitleImageIdentifier) {

    public static final String SECTION_CONFIGURATION_PATH = "items-adder";
    public static final String ENABLED_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".enabled";
    public static final String COUNTDOWN_IMAGES_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".countdown-images";
    public static final String THREE_IMAGE_CONFIGURATION_PATH = COUNTDOWN_IMAGES_CONFIGURATION_PATH + ".three";
    public static final String TWO_IMAGE_CONFIGURATION_PATH = COUNTDOWN_IMAGES_CONFIGURATION_PATH + ".two";
    public static final String ONE_IMAGE_CONFIGURATION_PATH = COUNTDOWN_IMAGES_CONFIGURATION_PATH + ".one";
    public static final String FIGHT_IMAGE_CONFIGURATION_PATH = COUNTDOWN_IMAGES_CONFIGURATION_PATH + ".fight";
    public static final String TOTEM_ITEM_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".totem-item";
    public static final String STATION_ITEM_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".station-item";
    public static final String STATION_TITLE_IMAGE_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH
            + ".station-title-image";
    public static final String BOARD_TITLE_IMAGE_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH
            + ".board-title-image";
    public static final String MACHINE_PARTS_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".machine-parts";
    public static final String MACHINE_PART_IDENTIFIER_PREFIX = "simplepvparena:machine_";

    public static final boolean DEFAULT_ENABLED = true;
    public static final String NO_IDENTIFIER = "";

    public static final String SUGGESTED_THREE_IMAGE_IDENTIFIER = "simplepvparena:countdown_3";
    public static final String SUGGESTED_TWO_IMAGE_IDENTIFIER = "simplepvparena:countdown_2";
    public static final String SUGGESTED_ONE_IMAGE_IDENTIFIER = "simplepvparena:countdown_1";
    public static final String SUGGESTED_FIGHT_IMAGE_IDENTIFIER = "simplepvparena:countdown_fight";
    public static final String SUGGESTED_TOTEM_ITEM_IDENTIFIER = "simplepvparena:totem";
    public static final String SUGGESTED_STATION_ITEM_IDENTIFIER = "simplepvparena:station";
    public static final String SUGGESTED_STATION_TITLE_IMAGE_IDENTIFIER = "simplepvparena:station_title";
    public static final String SUGGESTED_BOARD_TITLE_IMAGE_IDENTIFIER = "simplepvparena:board_title";

    public CustomVisualSettings {
        threeImageIdentifier = normalize(threeImageIdentifier);
        twoImageIdentifier = normalize(twoImageIdentifier);
        oneImageIdentifier = normalize(oneImageIdentifier);
        fightImageIdentifier = normalize(fightImageIdentifier);
        totemItemIdentifier = normalize(totemItemIdentifier);
        stationItemIdentifier = normalize(stationItemIdentifier);
        stationTitleImageIdentifier = normalize(stationTitleImageIdentifier);
        boardTitleImageIdentifier = normalize(boardTitleImageIdentifier);
    }

    public CustomVisualSettings(boolean enabled, String threeImageIdentifier, String twoImageIdentifier,
                                String oneImageIdentifier, String fightImageIdentifier, String totemItemIdentifier,
                                String stationItemIdentifier) {
        this(enabled, threeImageIdentifier, twoImageIdentifier, oneImageIdentifier, fightImageIdentifier,
                totemItemIdentifier, stationItemIdentifier, NO_IDENTIFIER, NO_IDENTIFIER);
    }

    public static CustomVisualSettings defaults() {
        return new CustomVisualSettings(DEFAULT_ENABLED, NO_IDENTIFIER, NO_IDENTIFIER, NO_IDENTIFIER, NO_IDENTIFIER,
                NO_IDENTIFIER, NO_IDENTIFIER, NO_IDENTIFIER, NO_IDENTIFIER);
    }

    private static String normalize(String identifier) {
        return Objects.requireNonNullElse(identifier, NO_IDENTIFIER).strip();
    }
}
