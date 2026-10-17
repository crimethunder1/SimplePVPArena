package de.simplepvparena.gui;

public enum ConfirmationRequest {

    CLEAN_ARENA("Clean the arena?"),
    RESET_ARENA("Reset the arena?"),
    REPLACE_KIT("Replace the kit with your inventory?"),
    DELETE_KIT("Delete the kit?"),
    REMOVE_TEAM("Remove the team?"),
    CANCEL_TOURNAMENT("Cancel the tournament?");

    private final String title;

    ConfirmationRequest(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
