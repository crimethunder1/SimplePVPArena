package de.simplepvparena.gui;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamChoiceMessage;
import de.simplepvparena.registration.TeamChoiceService;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TeamChoiceMenu implements AdminMenu {

    static final int INFO_SLOT = 22;
    static final int LEAVE_SLOT = 26;

    private final AdminMenuNavigation navigation;
    private final TeamChoiceService teamChoiceService;

    TeamChoiceMenu(AdminMenuNavigation navigation, TeamChoiceService teamChoiceService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamChoiceService = Objects.requireNonNull(teamChoiceService, "teamChoiceService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(TeamChoiceMessage.MENU_TITLE);
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        Optional<Team> ownTeam = teamChoiceService.findTeamOfPlayer(player.getUniqueId());
        for (TeamColour colour : teamChoiceService.findOfferedColours()) {
            canvas.set(slotOf(colour), buildBedTile(colour, ownTeam));
        }
        canvas.set(INFO_SLOT, MenuItems.info(Messages.text(TeamChoiceMessage.INFO_TITLE), List.of(
                Messages.plural(TeamChoiceMessage.INFO_REGISTERED_PLAYERS,
                        teamChoiceService.getRegisteredPlayerCount()),
                Messages.plural(TeamChoiceMessage.INFO_TEAM_SIZE, teamChoiceService.getTeamSize()),
                Messages.text(TeamChoiceMessage.INFO_HINT))));
        ownTeam.ifPresent(team -> canvas.set(LEAVE_SLOT, MenuItems.create(MenuItems.DESTRUCTIVE_MATERIAL,
                Messages.text(TeamChoiceMessage.LEAVE_TITLE),
                Messages.text(TeamChoiceMessage.LEAVE_LORE, Placeholder.of("team", team.getTeamName())))));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (slot == LEAVE_SLOT) {
            if (teamChoiceService.findTeamOfPlayer(player.getUniqueId()).isPresent()) {
                teamChoiceService.leave(player);
                navigation.refresh(player, holder);
            }
            return;
        }
        Optional<TeamColour> colour = colourOf(slot);
        if (colour.isEmpty()) {
            return;
        }
        if (teamChoiceService.findOfferedColours().contains(colour.get())) {
            teamChoiceService.join(player, colour.get());
        }
        navigation.refresh(player, holder);
    }

    static int slotOf(TeamColour colour) {
        return colour.ordinal();
    }

    static Optional<TeamColour> colourOf(int slot) {
        if (slot < 0 || slot >= TeamColour.values().length) {
            return Optional.empty();
        }
        return Optional.of(TeamColour.values()[slot]);
    }

    static Material bedOf(TeamColour colour) {
        return switch (colour) {
            case RED -> Material.RED_BED;
            case BLUE -> Material.BLUE_BED;
            case GREEN -> Material.GREEN_BED;
            case YELLOW -> Material.YELLOW_BED;
            case ORANGE -> Material.ORANGE_BED;
            case PURPLE -> Material.PURPLE_BED;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_BED;
            case PINK -> Material.PINK_BED;
            case WHITE -> Material.WHITE_BED;
            case GRAY -> Material.GRAY_BED;
            case BLACK -> Material.BLACK_BED;
            case BROWN -> Material.BROWN_BED;
            case CYAN -> Material.CYAN_BED;
            case LIME -> Material.LIME_BED;
            case MAGENTA -> Material.MAGENTA_BED;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_BED;
        };
    }

    private MenuTile buildBedTile(TeamColour colour, Optional<Team> ownTeam) {
        Optional<Team> team = teamChoiceService.findTeamOfColour(colour);
        List<TeamMember> members = team.map(Team::getMembers).orElse(List.of());
        int teamSize = teamChoiceService.getTeamSize();
        List<String> lore = new ArrayList<>();
        if (members.isEmpty()) {
            lore.add(Messages.text(TeamChoiceMessage.BED_NO_MEMBERS));
        }
        members.forEach(member -> lore.add(member.playerName()));
        lore.add(Messages.plural(TeamChoiceMessage.BED_PLACES, teamSize, Placeholder.of("taken", members.size())));
        String title = Messages.text(TeamChoiceMessage.BED_TITLE, Placeholder.of("colour", colour.getDisplayName()));
        if (team.isPresent() && ownTeam.isPresent()
                && team.get().getTeamName().equalsIgnoreCase(ownTeam.get().getTeamName())) {
            lore.add(Messages.text(TeamChoiceMessage.BED_YOUR_TEAM));
            return MenuItems.create(bedOf(colour), title, lore);
        }
        if (members.size() >= teamSize) {
            lore.add(Messages.text(TeamChoiceMessage.TEAM_FULL));
            return MenuItems.create(MenuItems.UNAVAILABLE_MATERIAL, title, lore);
        }
        lore.add(Messages.text(TeamChoiceMessage.BED_CLICK_TO_JOIN));
        return MenuItems.create(bedOf(colour), title, lore);
    }
}
