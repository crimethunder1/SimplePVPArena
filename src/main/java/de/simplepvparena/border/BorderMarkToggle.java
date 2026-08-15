package de.simplepvparena.border;

public enum BorderMarkToggle {

    MARKED,

    UNMARKED;

    public static BorderMarkToggle decide(boolean alreadyMarked) {
        return alreadyMarked ? UNMARKED : MARKED;
    }

    public boolean marks() {
        return this == MARKED;
    }

    public String describe() {
        return this == MARKED ? "Marked as a border block" : "Border mark removed";
    }
}
