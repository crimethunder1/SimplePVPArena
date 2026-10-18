package de.simplepvparena.message;

import java.util.Set;

public enum InputMessage implements MessageKey {

    CANCEL_WORD("input.cancel-word"),
    PROMPT_FOOTER("input.prompt-footer", "word", "seconds"),
    CANCELLED("input.cancelled", "description"),
    EXPIRED("input.expired", "description"),
    NOTHING_TYPED("input.nothing-typed", "usage"),
    EXPECTED_VALUES("input.expected-values", "count", "usage", "givenCount", "values"),
    EXPECTED_VALUE_RANGE("input.expected-value-range", "minimum", "maximum", "usage", "givenCount", "values"),
    UNKNOWN_MATERIAL("input.unknown-material", "material"),
    NOT_A_BLOCK("input.not-a-block", "material"),
    AMOUNT_NOT_A_NUMBER("input.amount-not-a-number", "amount"),
    AMOUNT_OUT_OF_RANGE("input.amount-out-of-range", "minimum", "maximum");

    private final String path;
    private final Set<String> placeholderNames;

    InputMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}
