package de.simplepvparena.input;

import de.simplepvparena.message.InputMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public record ChatInputValues(List<String> values) {

    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    public ChatInputValues {
        values = List.copyOf(values);
    }

    public static ChatInputValues of(String line) {
        String trimmedLine = line == null ? "" : line.trim();
        if (trimmedLine.isEmpty()) {
            return new ChatInputValues(List.of());
        }
        return new ChatInputValues(List.of(SEPARATOR.split(trimmedLine)));
    }

    public int count() {
        return values.size();
    }

    public String get(int index) {
        return values.get(index);
    }

    public Optional<String> findCountRejection(int expectedValueCount, String usage) {
        return findCountRejection(expectedValueCount, expectedValueCount, usage);
    }

    public Optional<String> findCountRejection(int minimumValueCount, int maximumValueCount, String usage) {
        if (values.size() >= minimumValueCount && values.size() <= maximumValueCount) {
            return Optional.empty();
        }
        Placeholder usagePlaceholder = Placeholder.of("usage", usage);
        if (values.isEmpty()) {
            return Optional.of(Messages.text(InputMessage.NOTHING_TYPED, usagePlaceholder));
        }
        Placeholder givenCount = Placeholder.of("givenCount", values.size());
        Placeholder givenValues = Placeholder.of("values", String.join(" ", values));
        if (minimumValueCount == maximumValueCount) {
            return Optional.of(Messages.plural(InputMessage.EXPECTED_VALUES, minimumValueCount, usagePlaceholder,
                    givenCount, givenValues));
        }
        return Optional.of(Messages.text(InputMessage.EXPECTED_VALUE_RANGE,
                Placeholder.of("minimum", minimumValueCount), Placeholder.of("maximum", maximumValueCount),
                usagePlaceholder, givenCount, givenValues));
    }
}
