package de.simplepvparena.input;

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
        if (values.isEmpty()) {
            return Optional.of("Nothing was typed, expected " + usage);
        }
        String expectedValueCount = minimumValueCount == maximumValueCount
                ? String.valueOf(minimumValueCount)
                : minimumValueCount + " to " + maximumValueCount;
        return Optional.of("Expected " + expectedValueCount + " values separated by spaces (" + usage
                + "), but got " + values.size() + ": " + String.join(" ", values));
    }
}
