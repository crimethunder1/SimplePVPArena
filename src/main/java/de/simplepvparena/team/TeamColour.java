package de.simplepvparena.team;

import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TeamMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum TeamColour {

    RED(DyeColor.RED, NamedTextColor.RED, TeamMessage.COLOUR_RED),
    BLUE(DyeColor.BLUE, NamedTextColor.BLUE, TeamMessage.COLOUR_BLUE),
    GREEN(DyeColor.GREEN, NamedTextColor.DARK_GREEN, TeamMessage.COLOUR_GREEN),
    YELLOW(DyeColor.YELLOW, NamedTextColor.YELLOW, TeamMessage.COLOUR_YELLOW),
    ORANGE(DyeColor.ORANGE, NamedTextColor.GOLD, TeamMessage.COLOUR_ORANGE),
    PURPLE(DyeColor.PURPLE, NamedTextColor.DARK_PURPLE, TeamMessage.COLOUR_PURPLE),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE, NamedTextColor.AQUA, TeamMessage.COLOUR_LIGHT_BLUE),
    PINK(DyeColor.PINK, NamedTextColor.LIGHT_PURPLE, TeamMessage.COLOUR_PINK),
    WHITE(DyeColor.WHITE, NamedTextColor.WHITE, TeamMessage.COLOUR_WHITE),
    GRAY(DyeColor.GRAY, NamedTextColor.DARK_GRAY, TeamMessage.COLOUR_GRAY),
    BLACK(DyeColor.BLACK, NamedTextColor.DARK_GRAY, TeamMessage.COLOUR_BLACK),
    BROWN(DyeColor.BROWN, NamedTextColor.DARK_RED, TeamMessage.COLOUR_BROWN),
    CYAN(DyeColor.CYAN, NamedTextColor.DARK_AQUA, TeamMessage.COLOUR_CYAN),
    LIME(DyeColor.LIME, NamedTextColor.GREEN, TeamMessage.COLOUR_LIME),
    MAGENTA(DyeColor.MAGENTA, NamedTextColor.LIGHT_PURPLE, TeamMessage.COLOUR_MAGENTA),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY, NamedTextColor.GRAY, TeamMessage.COLOUR_LIGHT_GRAY);

    private static final char ENUM_SEPARATOR = '_';
    private static final char STORAGE_SEPARATOR = '-';

    private final DyeColor dyeColour;
    private final NamedTextColor textColour;
    private final MessageKey nameKey;

    TeamColour(DyeColor dyeColour, NamedTextColor textColour, MessageKey nameKey) {
        this.dyeColour = dyeColour;
        this.textColour = textColour;
        this.nameKey = nameKey;
    }

    public DyeColor getDyeColour() {
        return dyeColour;
    }

    public NamedTextColor getTextColour() {
        return textColour;
    }

    public String getDisplayName() {
        return Messages.text(nameKey);
    }

    public String getStorageName() {
        return name().toLowerCase(Locale.ROOT).replace(ENUM_SEPARATOR, STORAGE_SEPARATOR);
    }

    public static Optional<TeamColour> ofStorageName(String storageName) {
        return Arrays.stream(values())
                .filter(colour -> colour.getStorageName().equals(storageName))
                .findFirst();
    }
}
