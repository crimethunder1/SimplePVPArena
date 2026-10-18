package de.simplepvparena.message;

import java.util.Objects;
import java.util.regex.Pattern;

public record Placeholder(String name, String value) {

    public static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    public Placeholder {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("placeholder name " + name
                    + " may only hold letters, digits, hyphens and underscores");
        }
    }

    public static Placeholder of(String name, Object value) {
        return new Placeholder(name, String.valueOf(value));
    }
}
