package de.simplepvparena.tournament;

public record TournamentOperationResult(boolean successful, String message) {

    public static TournamentOperationResult success(String message) {
        return new TournamentOperationResult(true, message);
    }

    public static TournamentOperationResult failure(String message) {
        return new TournamentOperationResult(false, message);
    }
}
