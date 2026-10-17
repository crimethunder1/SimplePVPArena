package de.simplepvparena.gui;

public enum ConfirmationRequest {

    CLEAN_ARENA("Clean the arena?"),
    RESET_ARENA("Reset the arena?"),
    SAVE_KIT("Save your inventory as the kit?"),
    REMOVE_TEAM("Remove the team?"),
    IMPORT_TEAMS("Replace every team with event.json?"),
    CANCEL_TOURNAMENT("Cancel the tournament?");

    private final String title;

    ConfirmationRequest(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
