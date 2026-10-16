package de.simplepvparena.tournament;

@FunctionalInterface
public interface AdminNotifier {

    void notifyAdmins(String message);
}
