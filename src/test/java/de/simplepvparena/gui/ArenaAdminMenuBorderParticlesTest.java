package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaAdminMenuBorderParticlesTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int BORDER_PARTICLES_SLOT = 21;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheParticlesShown() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("21 | BLAZE_POWDER | Border particles | The particles are shown / Only the particles at the "
                + "confirmed border wall / Click to hide them", describeBorderParticlesTile());
    }

    @Test
    void showsTheParticlesHidden() {
        fixture.borderParticleService().setConfirmedParticleEnabled(false);

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("21 | BLAZE_POWDER | Border particles | The particles are hidden / Only the particles at the "
                + "confirmed border wall / Click to show them", describeBorderParticlesTile());
    }

    @Test
    void aClickHidesTheParticlesWritesItReportsItAndRefreshesTheMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(BORDER_PARTICLES_SLOT);

        assertFalse(fixture.borderParticleService().isConfirmedParticleEnabled());
        assertEquals(List.of(false), fixture.writtenBorderParticleSwitches());
        assertEquals(List.of("Border particles are hidden"), fixture.messages());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
        assertEquals("21 | BLAZE_POWDER | Border particles | The particles are hidden / Only the particles at the "
                + "confirmed border wall / Click to show them", describeBorderParticlesTile());

        fixture.messages().clear();
        fixture.click(BORDER_PARTICLES_SLOT);

        assertTrue(fixture.borderParticleService().isConfirmedParticleEnabled());
        assertEquals(List.of(false, true), fixture.writtenBorderParticleSwitches());
        assertEquals(List.of("Border particles are shown"), fixture.messages());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
        assertTrue(describeBorderParticlesTile().endsWith(" / Click to hide them"), describeBorderParticlesTile());
    }

    @Test
    void theTileCanBeClickedWhileAMatchStandsOnTheArena() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(BORDER_PARTICLES_SLOT);

        assertFalse(fixture.borderParticleService().isConfirmedParticleEnabled());
        assertEquals(List.of(false), fixture.writtenBorderParticleSwitches());
        assertEquals(List.of("Border particles are hidden"), fixture.messages());
    }

    @Test
    void aFailureToWriteKeepsTheSwitchInMemoryAndNamesTheReason() {
        fixture.failBorderParticleSwitchWrites(new IOException("config.yml is read only"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(BORDER_PARTICLES_SLOT);

        assertFalse(fixture.borderParticleService().isConfirmedParticleEnabled());
        assertEquals(List.of(), fixture.writtenBorderParticleSwitches());
        assertEquals(List.of("Border particles are hidden, but config.yml could not be written: config.yml is "
                + "read only"), fixture.messages());
        assertTrue(describeBorderParticlesTile().contains(" | The particles are hidden / "),
                describeBorderParticlesTile());

        fixture.messages().clear();
        fixture.click(BORDER_PARTICLES_SLOT);

        assertTrue(fixture.borderParticleService().isConfirmedParticleEnabled());
        assertEquals(List.of("Border particles are shown, but config.yml could not be written: config.yml is "
                + "read only"), fixture.messages());
    }

    @Test
    void aFailureWithoutAMessageNamesTheKindOfFailure() {
        fixture.failBorderParticleSwitchWrites(new IOException());
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(BORDER_PARTICLES_SLOT);

        assertEquals(List.of("Border particles are hidden, but config.yml could not be written: IOException"),
                fixture.messages());
    }

    @Test
    @UseLanguage("de")
    void showsTheWholeArenaMenuWithTheParticlesHiddenInGerman() {
        fixture.borderParticleService().setConfirmedParticleEnabled(false);

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("""
                Arena | 27
                4 | BOOK | Arena | Arena unvollständig /   Kampffläche: nicht berechnet / \
                  Spawn des ersten Teams: nicht gesetzt /   Spawn des zweiten Teams: nicht gesetzt / \
                  Tribünen-Spawn: nicht gesetzt /   Wirkungsbereich: keiner, bis die Kampffläche berechnet ist / \
                  Laufendes Match: keins
                10 | SPYGLASS | Kampffläche berechnen | Berechnet die Fläche ab der Stelle, an der du stehst / \
                Steh vor dem Klick innerhalb der geschlossenen Arenawand / \
                Das Menü schließt sich, damit du die erkannte Wand sehen kannst
                11 | REDSTONE_TORCH | Diagnose mit höherem Zellenlimit | Eine Berechnung mit einem Limit, das du \
                eingibst, bis zu 5000000 Zellen / Zeigt, wie weit eine Füllung durch eine Lücke wirklich kommt / \
                config.yml bleibt unberührt
                12 | BLUE_BED | Spawn des ersten Teams setzen | Setzt ihn auf die Stelle, an der du stehst / \
                Steh innerhalb der Arenawand, wo das erste Team startet / \
                Eine Stelle auf der falschen Seite der Kampffläche / wird abgelehnt, sobald die Fläche berechnet ist
                13 | RED_BED | Spawn des zweiten Teams setzen | Setzt ihn auf die Stelle, an der du stehst / \
                Steh innerhalb der Arenawand, wo das zweite Team startet / \
                Eine Stelle auf der falschen Seite der Kampffläche / wird abgelehnt, sobald die Fläche berechnet ist
                14 | YELLOW_BED | Tribünen-Spawn setzen | Setzt ihn auf die Stelle, an der du stehst / \
                Steh außerhalb der Arenawand, wo die Zuschauer warten / \
                Eine Stelle auf der falschen Seite der Kampffläche / wird abgelehnt, sobald die Fläche berechnet ist
                15 | TNT | Arena reinigen | Es ist keine Kampffläche berechnet, es gibt nichts zu reinigen
                16 | BARRIER | Arena zurücksetzen | Verwirft die Kampffläche, alle drei Spawns und die \
                Platzierungstafel / \
                Die Blöcke und die Wandblöcke bleiben unberührt / Fragt vorher nach einer Bestätigung
                19 | TARGET | Höhe der Kampffläche markieren | Gibt dir das Item, das den tiefsten und den / \
                höchsten Punkt der Kampffläche markiert / Setze damit einen Block, nur seine Höhe wird gemerkt / \
                und der Block verschwindet wieder / Die nächste Berechnung übernimmt sie als Boden / \
                und Decke der Kampffläche, auch unterhalb der / Arenawand, und verbraucht beide Markierungen / \
                Es ist keine Höhe markiert / Der nächste gesetzte Block markiert den tiefsten Punkt / \
                Beide Enden müssen markiert sein, eine einzelne Markierung wird nicht verwendet
                20 | IRON_BARS | Starttor-Blöcke geben | Fragt im Chat nach einem Material und einer Menge und / \
                gibt dir so viele Starttor-Blöcke / Baue um jeden Team-Spawn eine Box und schließe sie / \
                mit ihnen: Jeder gesetzte gehört zum Starttor, / ein abgebauter ist wieder draußen / \
                Jeder Rundenstart stellt das ganze Starttor wieder her / \
                und baut es mit einem Countdown von oben wieder ab / \
                Ein nicht gebautes Starttor wird übersprungen, die Runde startet trotzdem / \
                Das Starttor hat noch keinen Block / Es wird über 10 Sekunden abgebaut
                21 | BLAZE_POWDER | Wandpartikel | Die Partikel sind ausgeblendet / \
                Nur die Partikel an der bestätigten Arenawand / Klicke, um sie anzuzeigen
                22 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                23 | OAK_SIGN | Platzierungstafel hier aufstellen | Stellt die Tafel zwei Blöcke über dem Block auf, / \
                auf dem du stehst, am besten an der Tribüne / Sie zeigt die Turnierrunde, das Match und die Platzierung
                """, fixture.describeLastMenu());
    }

    @Test
    @UseLanguage("de")
    void reportsTheClicksAndAFailureToWriteInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(BORDER_PARTICLES_SLOT);
        fixture.click(BORDER_PARTICLES_SLOT);
        fixture.failBorderParticleSwitchWrites(new IOException("kein Platz"));
        fixture.click(BORDER_PARTICLES_SLOT);
        fixture.click(BORDER_PARTICLES_SLOT);

        assertEquals(List.of("Die Wandpartikel sind ausgeblendet", "Die Wandpartikel werden angezeigt",
                "Die Wandpartikel sind ausgeblendet, aber config.yml konnte nicht geschrieben werden: kein Platz",
                "Die Wandpartikel werden angezeigt, aber config.yml konnte nicht geschrieben werden: kein Platz"),
                fixture.messages());
        assertEquals("21 | BLAZE_POWDER | Wandpartikel | Die Partikel werden angezeigt / Nur die Partikel an der "
                + "bestätigten Arenawand / Klicke, um sie auszublenden", describeBorderParticlesTile());
    }

    private String describeBorderParticlesTile() {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(BORDER_PARTICLES_SLOT + " | "))
                .findFirst()
                .orElseThrow();
    }
}
