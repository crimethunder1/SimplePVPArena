package de.simplepvparena.visual;

import de.simplepvparena.gate.StartGateCountdown;
import de.simplepvparena.match.CountdownImages;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.VisualMessage;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfiguredVisuals implements CountdownImages {

    private static final int THREE_SECONDS = 3;
    private static final int TWO_SECONDS = 2;
    private static final int ONE_SECOND = 1;

    private final CustomVisuals customVisuals;
    private final Consumer<String> warningLogger;
    private final Set<String> reportedIdentifiers = new HashSet<>();

    private CustomVisualSettings settings = CustomVisualSettings.defaults();

    public ConfiguredVisuals(CustomVisuals customVisuals, Consumer<String> warningLogger) {
        this.customVisuals = Objects.requireNonNull(customVisuals, "customVisuals");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
    }

    public CustomVisuals getCustomVisuals() {
        return customVisuals;
    }

    public CustomVisualSettings getSettings() {
        return settings;
    }

    public void applySettings(CustomVisualSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public Optional<String> findImage(int remainingSeconds) {
        CustomVisualSettings appliedSettings = settings;
        if (StartGateCountdown.isRelease(remainingSeconds)) {
            return findFontImage(CustomVisualSettings.FIGHT_IMAGE_CONFIGURATION_PATH,
                    appliedSettings.fightImageIdentifier());
        }
        return switch (remainingSeconds) {
            case THREE_SECONDS -> findFontImage(CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH,
                    appliedSettings.threeImageIdentifier());
            case TWO_SECONDS -> findFontImage(CustomVisualSettings.TWO_IMAGE_CONFIGURATION_PATH,
                    appliedSettings.twoImageIdentifier());
            case ONE_SECOND -> findFontImage(CustomVisualSettings.ONE_IMAGE_CONFIGURATION_PATH,
                    appliedSettings.oneImageIdentifier());
            default -> Optional.empty();
        };
    }

    public Optional<ItemStack> findTotemItem() {
        return findItem(CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH, settings.totemItemIdentifier());
    }

    Optional<String> findFontImage(String path, String identifier) {
        return find(path, identifier, customVisuals::findFontImage, VisualMessage.UNKNOWN_FONT_IMAGE);
    }

    Optional<ItemStack> findItem(String path, String identifier) {
        return find(path, identifier, customVisuals::findItem, VisualMessage.UNKNOWN_ITEM);
    }

    private <T> Optional<T> find(String path, String identifier, Function<String, Optional<T>> lookup,
                                 VisualMessage unknownMessage) {
        if (!settings.enabled() || identifier.isEmpty() || customVisuals instanceof VanillaVisuals) {
            return Optional.empty();
        }
        try {
            Optional<T> found = Objects.requireNonNullElse(lookup.apply(identifier), Optional.empty());
            if (found.isEmpty() && customVisuals.isReady()) {
                reportOnce(identifier, Messages.text(unknownMessage, Placeholder.of("identifier", identifier),
                        Placeholder.of("path", path)));
            }
            return found;
        } catch (RuntimeException | LinkageError failure) {
            reportOnce(identifier, Messages.text(VisualMessage.LOOKUP_FAILED,
                    Placeholder.of("identifier", identifier), Placeholder.of("path", path),
                    Placeholder.of("reason", describe(failure))));
            return Optional.empty();
        }
    }

    private void reportOnce(String identifier, String warning) {
        if (reportedIdentifiers.add(identifier)) {
            warningLogger.accept(warning);
        }
    }

    private static String describe(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
