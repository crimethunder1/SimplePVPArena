package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanTeamChoiceMenuSnapshotTest {

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
    private final AdminMenuFixture.OnlinePlayer alex = fixture.letAnotherPlayerJoin("Alex", false, true);
    private final AdminMenuFixture.OnlinePlayer notch = fixture.letAnotherPlayerJoin("Notch", false, true);

    @Test
    void showsTheTeamChoiceInGerman() {
        for (AdminMenuFixture.OnlinePlayer player : List.of(steve, alex, notch)) {
            fixture.registrationList().register(player.player().getUniqueId(), player.player().getName());
        }
        fixture.registerPlayers("Jeb_", "Dinnerbone");
        fixture.teamRegistry().registerTeam("Rot", List.of(member(alex), member(notch)), TeamColour.RED);
        fixture.teamRegistry().registerTeam("Blau", List.of(member(steve)), TeamColour.BLUE);

        fixture.openTeamChoiceFor(steve.player());

        assertEquals("""
                Wähle dein Team | 27
                0 | GRAY_DYE | Team Rot | Alex / Notch / 2 von 2 Plätzen / Dieses Team ist voll
                1 | BLUE_BED | Team Blau | Steve / 1 von 2 Plätzen / Dein Team
                2 | GREEN_BED | Team Grün | Noch niemand / 0 von 2 Plätzen / Klicke, um beizutreten
                22 | BOOK | Teamwahl | 5 Spieler sind angemeldet / Ein Team hat Platz für 2 Spieler / \
                Wähle ein Bett, um seinem Team beizutreten
                26 | BARRIER | Dein Team verlassen | Klicke, um Team Blau zu verlassen
                """, fixture.describeLastMenuOf(steve.player()));
    }

    @Test
    void namesTheSingularInGerman() {
        fixture.setTeamSize(1);
        fixture.registrationList().register(steve.player().getUniqueId(), "Steve");

        fixture.openTeamChoiceFor(steve.player());

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.findLastMenuOf(steve.player()).orElseThrow();
        assertEquals(List.of("Noch niemand", "0 von 1 Platz", "Klicke, um beizutreten"),
                menu.findTile(0).orElseThrow().loreLines());
        assertEquals(List.of("1 Spieler ist angemeldet", "Ein Team hat Platz für 1 Spieler",
                "Wähle ein Bett, um seinem Team beizutreten"),
                menu.findTile(TeamChoiceMenu.INFO_SLOT).orElseThrow().loreLines());
    }

    private static TeamMember member(AdminMenuFixture.OnlinePlayer player) {
        return new TeamMember(player.player().getUniqueId(), player.player().getName());
    }
}
