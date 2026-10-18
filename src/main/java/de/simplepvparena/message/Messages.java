package de.simplepvparena.message;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Messages {

    public static final String SINGULAR_SUFFIX = ".one";
    public static final String PLURAL_SUFFIX = ".other";
    public static final String COUNT_PLACEHOLDER = "count";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(" + Placeholder.NAME_PATTERN.pattern() + ")}");
    private static final String ITEMS_PLACEHOLDER = "items";
    private static final String LAST_ITEM_PLACEHOLDER = "last";
    private static final String ITEM_SEPARATOR = ", ";
    private static final Consumer<String> DEFAULT_WARNING_LOGGER = Logger.getLogger(Messages.class.getName())::warning;

    private static final Set<String> reportedProblems = ConcurrentHashMap.newKeySet();

    private static volatile MessageCatalog catalog = shippedEnglish();
    private static volatile Consumer<String> warningLogger = DEFAULT_WARNING_LOGGER;

    private Messages() {
    }

    public static void use(MessageCatalog newCatalog) {
        catalog = Objects.requireNonNull(newCatalog, "newCatalog");
        reportedProblems.clear();
    }

    public static void useWarningLogger(Consumer<String> newWarningLogger) {
        warningLogger = Objects.requireNonNull(newWarningLogger, "newWarningLogger");
    }

    public static void reset() {
        use(shippedEnglish());
        warningLogger = DEFAULT_WARNING_LOGGER;
    }

    public static MessageCatalog catalog() {
        return catalog;
    }

    public static String text(MessageKey key, Placeholder... placeholders) {
        return String.join(TournamentMessages.LINE_BREAK, lines(key, placeholders));
    }

    public static List<String> lines(MessageKey key, Placeholder... placeholders) {
        return render(key.path(), valuesByName(placeholders));
    }

    public static String plural(MessageKey baseKey, long count, Placeholder... placeholders) {
        Map<String, String> valuesByName = valuesByName(placeholders);
        valuesByName.put(COUNT_PLACEHOLDER, String.valueOf(count));
        String path = baseKey.path() + (count == 1 ? SINGULAR_SUFFIX : PLURAL_SUFFIX);
        return String.join(TournamentMessages.LINE_BREAK, render(path, valuesByName));
    }

    public static String join(List<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        int lastIndex = items.size() - 1;
        return text(CommonMessage.LIST_AND,
                Placeholder.of(ITEMS_PLACEHOLDER, String.join(ITEM_SEPARATOR, items.subList(0, lastIndex))),
                Placeholder.of(LAST_ITEM_PLACEHOLDER, items.get(lastIndex)));
    }

    public static String listLimited(List<String> items, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least one");
        }
        if (items.size() <= limit) {
            return join(items);
        }
        return text(CommonMessage.LIST_MORE,
                Placeholder.of(ITEMS_PLACEHOLDER, String.join(ITEM_SEPARATOR, items.subList(0, limit))),
                Placeholder.of(COUNT_PLACEHOLDER, items.size() - limit));
    }

    private static MessageCatalog shippedEnglish() {
        return MessageCatalogLoader.shippedCatalog(MessageCatalogLoader.DEFAULT_LANGUAGE);
    }

    private static Map<String, String> valuesByName(Placeholder... placeholders) {
        Map<String, String> valuesByName = new LinkedHashMap<>();
        for (Placeholder placeholder : placeholders) {
            valuesByName.put(placeholder.name(), placeholder.value());
        }
        return valuesByName;
    }

    private static List<String> render(String path, Map<String, String> valuesByName) {
        Optional<List<String>> templateLines = catalog.find(path);
        if (templateLines.isEmpty()) {
            reportOnce(path, "No text is known for the message key " + path + ", the key itself is shown");
            return List.of(path);
        }
        return templateLines.get().stream()
                .map(templateLine -> fill(path, templateLine, valuesByName))
                .toList();
    }

    private static String fill(String path, String templateLine, Map<String, String> valuesByName) {
        Matcher matcher = PLACEHOLDER.matcher(templateLine);
        StringBuilder text = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = valuesByName.get(name);
            if (value == null) {
                reportOnce(path + "{" + name + "}", "The text of the message key " + path + " names the placeholder {"
                        + name + "}, but no value was given for it, it is shown as it is");
                value = matcher.group();
            }
            matcher.appendReplacement(text, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(text);
        return text.toString();
    }

    private static void reportOnce(String problem, String warning) {
        if (reportedProblems.add(problem)) {
            warningLogger.accept(warning);
        }
    }
}
