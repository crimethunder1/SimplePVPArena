package de.simplepvparena.kit;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.PlayerStateMessage;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStateTextsTest {

    private static final Placeholder PLAYER = Placeholder.of("player", "Steve");
    private static final Placeholder FILE = Placeholder.of("file", "1234.yml.broken");
    private static final Placeholder REASON = Placeholder.of("reason", "disk full");

    @Test
    @UseLanguage("en")
    void tellsTheAdminsAboutSavedPlayerStatesInEnglish() {
        assertEquals(List.of(
                "No saved inventory and state of Steve was found, the kit was cleared without restoring anything",
                "The saved inventory and state of Steve could not be read, the kit was cleared without restoring "
                        + "anything, the file is kept as 1234.yml.broken",
                "The saved inventory and state of Steve could not be read, nothing was cleared because no kit is "
                        + "marked as applied, the file is kept as 1234.yml.broken",
                "The saved inventory and state of Steve could not be restored (disk full), the saved state is kept "
                        + "for the next attempt",
                "The inventory and state of Steve could not be written to disk (disk full), they are kept in memory "
                        + "only and are lost if the server stops unexpectedly",
                "The saved inventory and state of Steve were restored, but the file could not be deleted (disk full)",
                "The saved inventory and state of Steve were restored, but the file could not be deleted (disk full), "
                        + "it was set aside as a .restored file and is not restored again"), describeEveryNotice());
    }

    @Test
    @UseLanguage("de")
    void tellsTheAdminsAboutSavedPlayerStatesInGerman() {
        assertEquals(List.of(
                "Für Steve wurde kein gespeichertes Inventar und kein gespeicherter Zustand gefunden, das Kit wurde "
                        + "abgeräumt, ohne etwas wiederherzustellen",
                "Das gespeicherte Inventar und der Zustand von Steve konnten nicht gelesen werden, das Kit wurde "
                        + "abgeräumt, ohne etwas wiederherzustellen, die Datei bleibt als 1234.yml.broken erhalten",
                "Das gespeicherte Inventar und der Zustand von Steve konnten nicht gelesen werden, es wurde nichts "
                        + "abgeräumt, weil kein Kit als angewendet markiert ist, die Datei bleibt als 1234.yml.broken "
                        + "erhalten",
                "Das gespeicherte Inventar und der Zustand von Steve konnten nicht wiederhergestellt werden "
                        + "(disk full), der gespeicherte Zustand bleibt für den nächsten Versuch erhalten",
                "Das Inventar und der Zustand von Steve konnten nicht auf die Festplatte geschrieben werden "
                        + "(disk full), sie liegen nur im Speicher und gehen verloren, wenn der Server unerwartet stoppt",
                "Das gespeicherte Inventar und der Zustand von Steve wurden wiederhergestellt, aber die Datei konnte "
                        + "nicht gelöscht werden (disk full)",
                "Das gespeicherte Inventar und der Zustand von Steve wurden wiederhergestellt, aber die Datei konnte "
                        + "nicht gelöscht werden (disk full), sie wurde als .restored-Datei beiseitegelegt und wird "
                        + "nicht noch einmal wiederhergestellt"), describeEveryNotice());
    }

    private static List<String> describeEveryNotice() {
        return List.of(Messages.text(PlayerStateMessage.NOT_FOUND, PLAYER),
                Messages.text(PlayerStateMessage.UNREADABLE_CLEARED, PLAYER, FILE),
                Messages.text(PlayerStateMessage.UNREADABLE_KEPT, PLAYER, FILE),
                Messages.text(PlayerStateMessage.NOT_RESTORED, PLAYER, REASON),
                Messages.text(PlayerStateMessage.NOT_WRITTEN, PLAYER, REASON),
                Messages.text(PlayerStateMessage.FILE_NOT_DELETED, PLAYER, REASON),
                Messages.text(PlayerStateMessage.SET_ASIDE, PLAYER, REASON));
    }
}
