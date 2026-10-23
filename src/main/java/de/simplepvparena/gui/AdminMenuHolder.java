package de.simplepvparena.gui;

import de.simplepvparena.match.Match;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AdminMenuHolder implements InventoryHolder {

    private final AdminMenuType menuType;
    private final String subject;
    private final ConfirmationRequest confirmationRequest;
    private final int pageIndex;
    private final boolean openedFromMainMenu;
    private final Match match;
    private final Map<Integer, String> matchIdsBySlot = new HashMap<>();

    private Inventory inventory;

    private AdminMenuHolder(AdminMenuType menuType, String subject, ConfirmationRequest confirmationRequest,
                            int pageIndex) {
        this(menuType, subject, confirmationRequest, pageIndex, false);
    }

    private AdminMenuHolder(AdminMenuType menuType, String subject, ConfirmationRequest confirmationRequest,
                            int pageIndex, boolean openedFromMainMenu) {
        this(menuType, subject, confirmationRequest, pageIndex, openedFromMainMenu, null);
    }

    private AdminMenuHolder(AdminMenuType menuType, String subject, ConfirmationRequest confirmationRequest,
                            int pageIndex, boolean openedFromMainMenu, Match match) {
        this.menuType = Objects.requireNonNull(menuType, "menuType");
        this.subject = subject;
        this.confirmationRequest = confirmationRequest;
        this.pageIndex = Math.max(0, pageIndex);
        this.openedFromMainMenu = openedFromMainMenu;
        this.match = match;
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

    public static AdminMenuHolder registrations(int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.REGISTRATIONS, null, null, pageIndex);
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

    public static AdminMenuHolder decidedMatches(int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.DECIDED_MATCHES, null, null, pageIndex);
    }

    public static AdminMenuHolder confirmation(ConfirmationRequest confirmationRequest, String subject,
                                               int pageIndex) {
        return new AdminMenuHolder(AdminMenuType.CONFIRMATION, subject,
                Objects.requireNonNull(confirmationRequest, "confirmationRequest"), pageIndex);
    }

    public static AdminMenuHolder restartConfirmation(Match match) {
        Objects.requireNonNull(match, "match");
        return new AdminMenuHolder(AdminMenuType.CONFIRMATION, match.getMatchId(), ConfirmationRequest.RESTART_MATCH,
                0, false, match);
    }

    public static AdminMenuHolder bracketView(int pageIndex, boolean openedFromMainMenu) {
        return new AdminMenuHolder(AdminMenuType.BRACKET_VIEW, null, null, pageIndex, openedFromMainMenu);
    }

    public static AdminMenuHolder unequalTeamVote(String matchId) {
        return new AdminMenuHolder(AdminMenuType.UNEQUAL_TEAM_VOTE, Objects.requireNonNull(matchId, "matchId"), null,
                0);
    }

    public static AdminMenuHolder teamChoice() {
        return new AdminMenuHolder(AdminMenuType.TEAM_CHOICE, null, null, 0);
    }

    public AdminMenuType getMenuType() {
        return menuType;
    }

    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<Match> getMatch() {
        return Optional.ofNullable(match);
    }

    public Optional<ConfirmationRequest> getConfirmationRequest() {
        return Optional.ofNullable(confirmationRequest);
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public boolean isOpenedFromMainMenu() {
        return openedFromMainMenu;
    }

    void rememberMatchIdOfSlot(int slot, String matchId) {
        matchIdsBySlot.put(slot, Objects.requireNonNull(matchId, "matchId"));
    }

    void forgetMatchIdsOfSlots() {
        matchIdsBySlot.clear();
    }

    Optional<String> findMatchIdOfSlot(int slot) {
        return Optional.ofNullable(matchIdsBySlot.get(slot));
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
