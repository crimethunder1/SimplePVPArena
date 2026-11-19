package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface AdminNotifier {

    void notifyAdmins(String message);
}
