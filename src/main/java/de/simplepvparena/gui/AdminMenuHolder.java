package de.simplepvparena.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class AdminMenuHolder implements InventoryHolder {

    private final AdminMenuType menuType;
    private final String subject;
    private final ConfirmationRequest confirmationRequest;
    private final int pageIndex;

    private Inventory inventory;

    private AdminMenuHolder(AdminMenuType menuType, String subject, ConfirmationRequest confirmationRequest,
                            int pageIndex) {
        this.menuType = Objects.requireNonNull(menuType, "menuType");
        this.subject = subject;
        this.confirmationRequest = confirmationRequest;
        this.pageIndex = Math.max(0, pageIndex);
    }

    public static AdminMenuHolder main() {
        return new AdminMenuHolder(AdminMenuType.MAIN, null, null, 0);
    }

    public static AdminMenuHolder arena() {
        return new AdminMenuHolder(AdminMenuType.ARENA, null, null, 0);
    }

    public static AdminMenuHolder kit(int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.KIT, null, null, pageIndex);
    }

    public static AdminMenuHolder kitActions(String kitName, int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.KIT_ACTIONS, Objects.requireNonNull(kitName, "kitName"), null,
                pageIndex);
    }

    public static AdminMenuHolder kitChoice(String matchId, int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.KIT_CHOICE, Objects.requireNonNull(matchId, "matchId"), null,
                pageIndex);
    }

    public static AdminMenuHolder teams(int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.TEAMS, null, null, pageIndex);
    }

    public static AdminMenuHolder teamActions(String teamName, int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.TEAM_ACTIONS, Objects.requireNonNull(teamName, "teamName"),
                null, pageIndex);
    }

    public static AdminMenuHolder teamMembers(String teamName, int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.TEAM_MEMBERS, Objects.requireNonNull(teamName, "teamName"),
                null, pageIndex);
    }

    public static AdminMenuHolder match() {
        return new AdminMenuHolder(AdminMenuType.MATCH, null, null, 0);
    }

    public static AdminMenuHolder forceResult(String matchId) {
        return new AdminMenuHolder(AdminMenuType.FORCE_RESULT, Objects.requireNonNull(matchId, "matchId"), null, 0);
    }

    public static AdminMenuHolder waitingMatches(int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.WAITING_MATCHES, null, null, pageIndex);
    }

    public static AdminMenuHolder waitingMatchResult(String matchId, int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.WAITING_MATCH_RESULT, Objects.requireNonNull(matchId, "matchId"),
                null, pageIndex);
    }

    public static AdminMenuHolder confirmation(ConfirmationRequest confirmationRequest, String subject,
                                               int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.CONFIRMATION, subject,
                Objects.requireNonNull(confirmationRequest, "confirmationRequest"), pageIndex);
    }

    public AdminMenuType getMenuType() {
        return menuType;
    }

    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<ConfirmationRequest> getConfirmationRequest() {
        return Optional.ofNullable(confirmationRequest);
    }

    public int getPageIndex() {
        return pageIndex;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("the inventory of the " + menuType + " menu was not built yet");
        }
        return inventory;
    }
}
