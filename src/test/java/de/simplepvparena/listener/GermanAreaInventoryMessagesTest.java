package de.simplepvparena.listener;

import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.message.AreaInventoryMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.entity.Item;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanAreaInventoryMessagesTest {

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private AreaInventoryListener listener;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        listener = new AreaInventoryListener(fixture.harness().getPlugin(), fixture.service());
    }

    @Test
    void tellsThePlayerInGermanAboutHandingInAndGettingBack() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Besucher");

        fixture.service().reconcile(player.player(), fixture.inside());
        fixture.service().reconcile(player.player(), fixture.outside());

        assertEquals(List.of("Dein Inventar wird verwahrt, solange du im Event-Gebiet bist, du bekommst es beim "
                + "Verlassen zurück", "Dein Inventar ist zurück"), player.messages());
    }

    @Test
    void tellsThePlayerAndTheAdminsInGermanAboutAnUnreadableFile() throws IOException {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Pechvogel");
        fixture.service().reconcile(player.player(), fixture.inside());
        Files.writeString(fixture.inventoryFileOf(player), "{{{", StandardCharsets.UTF_8);
        player.forgetReceivedTexts();

        fixture.service().reconcile(player.player(), fixture.outside());

        assertEquals(List.of("Dein Inventar konnte nicht zurückgegeben werden, ein Admin wurde informiert"),
                player.messages());
        assertEquals(List.of("Das gesicherte Inventar von Pechvogel konnte nicht gelesen werden, es wurde nichts "
                + "geleert und nichts zurückgegeben, die Datei bleibt als " + player.uniqueId()
                + ".yml.broken zum Wiederherstellen von Hand erhalten"), fixture.warnings());
    }

    @Test
    void warnsTheAdminsInGermanAboutAFileThatCannotBeWritten() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Voll");
        fixture.failWritesWith(new IOException("kein Platz"));

        fixture.service().reconcile(player.player(), fixture.inside());

        assertEquals(List.of("Das Inventar von Voll konnte nicht auf die Festplatte geschrieben werden (kein Platz), "
                + "es bleibt beim Spieler und die nächste Prüfung versucht es erneut. Diese Warnung kommt höchstens "
                + "einmal pro Minute"), fixture.warnings());
    }

    @Test
    void showsTheLocksInGermanInTheActionBar() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Sammler");
        player.moveTo(fixture.inside());

        PlayerDropItemEvent dropEvent = new PlayerDropItemEvent(player.player(),
                ArenaMatchHarness.proxy(Item.class, (method, arguments) -> {
                    throw new UnsupportedOperationException("Item." + method.getName());
                }));
        listener.onPlayerDropItem(dropEvent);
        listener.onInventoryOpen(new InventoryOpenEvent(enderChestViewOf(player)));

        assertTrue(dropEvent.isCancelled());
        assertEquals(List.of("Im Event-Gebiet kannst du keine Items wegwerfen",
                "Im Event-Gebiet ist die Endertruhe gesperrt"), player.actionBars());
    }

    @Test
    void writesTheRemainingAdminTextsAndTheReloadCategoryInGerman() {
        assertEquals("Das Inventar von Alex war als abgegeben markiert, aber datei.yml gibt es nicht, es wurde "
                        + "nichts geleert und nichts zurückgegeben",
                Messages.text(AreaInventoryMessage.MISSING, Placeholder.of("player", "Alex"),
                        Placeholder.of("file", "datei.yml")));
        assertEquals("Das gesicherte Inventar von Alex konnte nicht zurückgegeben werden (gesperrt), die Datei "
                        + "bleibt erhalten und die nächste Prüfung versucht es erneut. Diese Warnung kommt höchstens "
                        + "einmal pro Minute",
                Messages.text(AreaInventoryMessage.NOT_RESTORED, Placeholder.of("player", "Alex"),
                        Placeholder.of("reason", "gesperrt")));
        assertEquals("Das Inventar von Alex wurde gesichert, konnte aber nicht geleert werden (gesperrt), das "
                        + "gesicherte Inventar wird zurückgegeben, wenn der Spieler den Wirkungsbereich verlässt",
                Messages.text(AreaInventoryMessage.NOT_CLEARED, Placeholder.of("player", "Alex"),
                        Placeholder.of("reason", "gesperrt")));
        assertEquals("Das Inventar von Alex ist zurück, aber die Spielerdaten konnten nicht gespeichert werden "
                        + "(gesperrt), datei.yml bleibt als Kopie erhalten",
                Messages.text(AreaInventoryMessage.PLAYER_DATA_NOT_SAVED, Placeholder.of("player", "Alex"),
                        Placeholder.of("file", "datei.yml"), Placeholder.of("reason", "gesperrt")));
        assertEquals("Das Inventar von Alex ist zurück, aber datei.yml konnte nicht gelöscht werden (gesperrt)",
                Messages.text(AreaInventoryMessage.FILE_NOT_DELETED, Placeholder.of("player", "Alex"),
                        Placeholder.of("file", "datei.yml"), Placeholder.of("reason", "gesperrt")));
        assertTrue(ConfigurationReloadReport.describeReloadedCategories().contains("ob Spieler im Wirkungsbereich "
                + "ihr Inventar abgeben, mit false bekommt jeder sein Inventar sofort zurück"));
    }

    private static InventoryView enderChestViewOf(ArenaMatchHarness.FakePlayer player) {
        Inventory enderChest = ArenaMatchHarness.proxy(Inventory.class, (method, arguments) -> switch (method.getName()) {
            case "getType" -> InventoryType.ENDER_CHEST;
            default -> throw new UnsupportedOperationException("Inventory." + method.getName());
        });
        return ArenaMatchHarness.proxy(InventoryView.class, (method, arguments) -> switch (method.getName()) {
            case "getTopInventory" -> enderChest;
            case "getPlayer" -> player.player();
            default -> throw new UnsupportedOperationException("InventoryView." + method.getName());
        });
    }
}
