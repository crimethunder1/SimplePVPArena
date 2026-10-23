package de.simplepvparena.gui;

import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.FailureDescriptions;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.RegistrationMenuMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.registration.RandomTeamFormation;
import de.simplepvparena.registration.RegistrationEntry;
import de.simplepvparena.registration.RegistrationService;
import de.simplepvparena.registration.RegistrationStationService;
import de.simplepvparena.registration.TeamChoiceService;
import de.simplepvparena.registration.TeamMode;
import de.simplepvparena.registration.TeamModeSetting;
import de.simplepvparena.registration.TeamModeWriter;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

final class RegistrationsAdminMenu implements AdminMenu {

    private static final int REGISTRATIONS_MENU_SIZE = 54;

    private static final int REGISTRATIONS_PREVIOUS_PAGE_SLOT = 45;
    private static final int REGISTRATIONS_TOOL_SLOT = 46;
    private static final int REGISTRATIONS_INFO_SLOT = 47;
    private static final int REGISTRATIONS_FORM_SLOT = 48;
    private static final int REGISTRATIONS_BACK_SLOT = 49;
    private static final int REGISTRATIONS_TEAM_MODE_SLOT = 52;
    private static final int REGISTRATIONS_CLEAR_SLOT = 51;
    private static final int REGISTRATIONS_NEXT_PAGE_SLOT = 53;

    private static final Material FORM_MATERIAL = Material.NETHER_STAR;
    private static final Material TEAM_MODE_MATERIAL = Material.RED_BED;

    private final AdminMenuNavigation navigation;
    private final RegistrationService registrationService;
    private final RegistrationStationService stationService;
    private final RandomTeamFormation teamFormation;
    private final TeamSizeSetting teamSizeSetting;
    private final TeamModeSetting teamModeSetting;
    private final TeamModeWriter teamModeWriter;
    private final TeamChoiceService teamChoiceService;

    RegistrationsAdminMenu(AdminMenuNavigation navigation, RegistrationService registrationService,
                           RegistrationStationService stationService, RandomTeamFormation teamFormation,
                           TeamSizeSetting teamSizeSetting, TeamModeSetting teamModeSetting,
                           TeamModeWriter teamModeWriter, TeamChoiceService teamChoiceService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.stationService = Objects.requireNonNull(stationService, "stationService");
        this.teamFormation = Objects.requireNonNull(teamFormation, "teamFormation");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.teamModeSetting = Objects.requireNonNull(teamModeSetting, "teamModeSetting");
        this.teamModeWriter = Objects.requireNonNull(teamModeWriter, "teamModeWriter");
        this.teamChoiceService = Objects.requireNonNull(teamChoiceService, "teamChoiceService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(RegistrationMenuMessage.TITLE, Placeholder.of("page",
                MenuPagination.of(registrationService.getCount(), holder.getPageIndex()).getPageNumber()));
    }

    @Override
    public int size() {
        return REGISTRATIONS_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        List<RegistrationEntry> entries = registrationService.getEntries();
        MenuPagination pagination = MenuPagination.of(entries.size(), holder.getPageIndex());
        for (int slot = 0; slot < pagination.getItemCountOnPage(); slot++) {
            RegistrationEntry entry = entries.get(pagination.getFirstItemIndex() + slot);
            canvas.set(slot, MenuItems.create(Material.PLAYER_HEAD, entry.playerName(),
                    Messages.text(RegistrationMenuMessage.PLAYER_LORE)));
        }
        canvas.set(REGISTRATIONS_INFO_SLOT, MenuItems.info(Messages.text(RegistrationMenuMessage.INFO_TITLE), List.of(
                describeRegisteredPlayers(entries.size()),
                Messages.plural(RegistrationMenuMessage.TEAM_COUNT, teamFormation.countTeams(),
                        Placeholder.of("teamSize", teamSizeSetting.getTeamSize())),
                Messages.text(MenuMessage.PAGE_OF_PAGES, Placeholder.of("page", pagination.getPageNumber()),
                        Placeholder.of("pageCount", pagination.getPageCount())))));
        canvas.set(REGISTRATIONS_TOOL_SLOT, MenuItems.create(RegistrationStationService.TOOL_MATERIAL,
                Messages.text(RegistrationMenuMessage.TOOL_TITLE), Messages.text(RegistrationMenuMessage.TOOL_LORE)));
        canvas.set(REGISTRATIONS_FORM_SLOT, buildFormItem());
        canvas.set(REGISTRATIONS_TEAM_MODE_SLOT, MenuItems.create(TEAM_MODE_MATERIAL,
                Messages.text(RegistrationMenuMessage.TEAM_MODE_TITLE),
                describeTeamMode(teamModeSetting.getTeamMode()),
                Messages.text(RegistrationMenuMessage.TEAM_MODE_SWITCH)));
        canvas.set(REGISTRATIONS_CLEAR_SLOT, buildClearItem(entries.size()));
        if (pagination.hasPreviousPage()) {
            canvas.set(REGISTRATIONS_PREVIOUS_PAGE_SLOT, MenuItems.previousPage(pagination));
        }
        if (pagination.hasNextPage()) {
            canvas.set(REGISTRATIONS_NEXT_PAGE_SLOT, MenuItems.nextPage(pagination));
        }
        canvas.set(REGISTRATIONS_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_TEAM_LIST));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        List<RegistrationEntry> entries = registrationService.getEntries();
        MenuPagination pagination = MenuPagination.of(entries.size(), holder.getPageIndex());
        switch (slot) {
            case REGISTRATIONS_PREVIOUS_PAGE_SLOT -> {
                if (pagination.hasPreviousPage()) {
                    navigation.open(player, AdminMenuHolder.registrations(pagination.getPreviousPageIndex()));
                }
            }
            case REGISTRATIONS_NEXT_PAGE_SLOT -> {
                if (pagination.hasNextPage()) {
                    navigation.open(player, AdminMenuHolder.registrations(pagination.getNextPageIndex()));
                }
            }
            case REGISTRATIONS_TOOL_SLOT -> handOutTool(player, holder);
            case REGISTRATIONS_FORM_SLOT -> askForFormingConfirmation(player, pagination.pageIndex());
            case REGISTRATIONS_TEAM_MODE_SLOT -> switchTeamMode(player, holder);
            case REGISTRATIONS_CLEAR_SLOT -> askForClearingConfirmation(player, entries.size(),
                    pagination.pageIndex());
            case REGISTRATIONS_INFO_SLOT -> { }
            case REGISTRATIONS_BACK_SLOT -> navigation.open(player, AdminMenuHolder.teams(0));
            default -> removeRegistration(player, entries, pagination, slot);
        }
    }

    AdminMenuConfirmation formConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return List.of(Messages.text(RegistrationMenuMessage.FORM_CONFIRMATION_REPLACES),
                        Messages.plural(RegistrationMenuMessage.FORM_CONFIRMATION_TEAMS, teamFormation.countTeams()));
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                teamFormation.form(player);
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.registrations(holder.getPageIndex());
            }
        };
    }

    AdminMenuConfirmation clearConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return List.of(Messages.plural(RegistrationMenuMessage.CLEAR_CONFIRMATION_REGISTRATIONS,
                                registrationService.getCount()),
                        Messages.text(teamChoiceService.removesColouredTeamsWithTheRegistrations()
                                ? RegistrationMenuMessage.CLEAR_CONFIRMATION_COLOURED_TEAMS
                                : RegistrationMenuMessage.CLEAR_CONFIRMATION_TEAMS_STAY));
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                registrationService.clearRegistrations();
                int removedTeamCount = teamChoiceService.removeColouredTeams();
                TournamentMessages.sendSuccess(player, Messages.text(RegistrationMenuMessage.REGISTRATIONS_CLEARED));
                if (removedTeamCount > 0) {
                    TournamentMessages.sendSuccess(player, Messages.plural(
                            RegistrationMenuMessage.COLOURED_TEAMS_REMOVED, removedTeamCount));
                }
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.registrations(0);
            }
        };
    }

    static String describeRegisteredPlayers(int registeredPlayerCount) {
        return Messages.plural(RegistrationMenuMessage.REGISTERED_PLAYERS, registeredPlayerCount);
    }

    static String describeTeamMode(TeamMode teamMode) {
        return Messages.text(teamMode == TeamMode.FREE_CHOICE
                ? RegistrationMenuMessage.TEAM_MODE_FREE_CHOICE
                : RegistrationMenuMessage.TEAM_MODE_RANDOM);
    }

    private MenuTile buildFormItem() {
        String title = Messages.text(RegistrationMenuMessage.FORM_TITLE);
        Optional<String> rejection = teamFormation.findRejection();
        if (rejection.isPresent()) {
            return MenuItems.unavailable(title, rejection.get());
        }
        return MenuItems.create(FORM_MATERIAL, title, Messages.text(RegistrationMenuMessage.FORM_LORE),
                Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION));
    }

    private MenuTile buildClearItem(int registeredPlayerCount) {
        String title = Messages.text(RegistrationMenuMessage.CLEAR_TITLE);
        if (registeredPlayerCount == 0) {
            return MenuItems.unavailable(title, Messages.text(RegistrationMenuMessage.NOBODY_REGISTERED));
        }
        return MenuItems.create(MenuItems.DESTRUCTIVE_MATERIAL, title,
                Messages.text(teamChoiceService.removesColouredTeamsWithTheRegistrations()
                        ? RegistrationMenuMessage.CLEAR_LORE_FREE_CHOICE
                        : RegistrationMenuMessage.CLEAR_LORE),
                Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION));
    }

    private void handOutTool(Player player, AdminMenuHolder holder) {
        Map<Integer, ItemStack> itemsThatDidNotFit = player.getInventory().addItem(stationService.createTool());
        if (!itemsThatDidNotFit.isEmpty()) {
            TournamentMessages.sendWarning(player, Messages.text(RegistrationMenuMessage.TOOL_INVENTORY_FULL));
            return;
        }
        TournamentMessages.sendSuccess(player, Messages.text(RegistrationMenuMessage.TOOL_HANDED_OUT));
        navigation.refresh(player, holder);
    }

    private void switchTeamMode(Player player, AdminMenuHolder holder) {
        TeamMode teamMode = teamModeSetting.getTeamMode().switched();
        teamModeSetting.setTeamMode(teamMode);
        try {
            teamModeWriter.write(teamMode);
            TournamentMessages.sendSuccess(player, describeTeamMode(teamMode));
        } catch (IOException failure) {
            TournamentMessages.sendWarning(player, Messages.text(RegistrationMenuMessage.TEAM_MODE_NOT_WRITTEN,
                    Placeholder.of("mode", describeTeamMode(teamMode)),
                    Placeholder.of("reason", FailureDescriptions.describe(failure))));
        }
        navigation.refresh(player, holder);
    }

    private void askForFormingConfirmation(Player player, int pageIndex) {
        Optional<String> rejection = teamFormation.findRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.FORM_RANDOM_TEAMS, null, pageIndex));
    }

    private void askForClearingConfirmation(Player player, int registeredPlayerCount, int pageIndex) {
        if (registeredPlayerCount == 0) {
            TournamentMessages.sendError(player, Messages.text(RegistrationMenuMessage.NOBODY_REGISTERED));
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.CLEAR_REGISTRATIONS, null,
                pageIndex));
    }

    private void removeRegistration(Player player, List<RegistrationEntry> entries, MenuPagination pagination,
                                    int slot) {
        OptionalInt entryIndex = pagination.findItemIndex(slot);
        if (entryIndex.isEmpty()) {
            return;
        }
        RegistrationEntry entry = entries.get(entryIndex.getAsInt());
        if (registrationService.removeRegistration(entry.uniqueId())) {
            TournamentMessages.sendSuccess(player, Messages.text(RegistrationMenuMessage.REGISTRATION_REMOVED,
                    Placeholder.of("player", entry.playerName())));
        }
        navigation.open(player, AdminMenuHolder.registrations(
                MenuPagination.of(registrationService.getCount(), pagination.pageIndex()).pageIndex()));
    }
}
