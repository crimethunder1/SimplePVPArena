package de.simplepvparena.gui;

public enum ConfirmationRequest {

    CLEAN_ARENA("Clean the arena?", AdminMenuType.ARENA),
    RESET_ARENA("Reset the arena?", AdminMenuType.ARENA),
    SAVE_KIT("Save your inventory as the kit?", AdminMenuType.KIT),
    REMOVE_TEAM("Remove the team?", AdminMenuType.TEAMS),
    CANCEL_TOURNAMENT("Cancel the tournament?", AdminMenuType.MATCH);

    private final String title;
    private final AdminMenuType returnMenuType;

    ConfirmationRequest(String title, AdminMenuType returnMenuType) {
        this.title = title;
        this.returnMenuType = returnMenuType;
    }

    public String getTitle() {
        return title;
    }

    public AdminMenuType getReturnMenuType() {
        return returnMenuType;
    }
}
