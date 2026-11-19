package de.simplepvparena.border;

import de.simplepvparena.message.BorderMessage;
import de.simplepvparena.message.Messages;
import org.jspecify.annotations.NullMarked;

@NullMarked
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
        return Messages.text(this == MARKED ? BorderMessage.MARKED : BorderMessage.MARK_REMOVED);
    }
}
