package de.simplepvparena.arena;

import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaItemTextsTest {

    private final FakeServerHarness harness = new FakeServerHarness("event");
    private final SpectatorItemFactory spectatorItems = new SpectatorItemFactory(harness.getPlugin());
    private final ArenaHeightMarkerService markerService = new ArenaHeightMarkerService(harness.getPlugin(),
            new ArenaHeightMarkers());

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    @UseLanguage("en")
    void namesTheSpectatorItemInEnglish() {
        assertEquals("Spectator mode", labelledSpectatorItem().getName());
    }

    @Test
    @UseLanguage("de")
    void namesTheSpectatorItemInGerman() {
        assertEquals("Zuschauermodus", labelledSpectatorItem().getName());
    }

    @Test
    void recognizesTheSpectatorItemByItsMarkerInEitherLanguage() {
        RecordingItemStack english = labelledSpectatorItem();
        Messages.use(MessageCatalogLoader.shippedCatalog("de"));
        RecordingItemStack german = labelledSpectatorItem();

        assertEquals("Spectator mode", english.getName());
        assertEquals("Zuschauermodus", german.getName());
        assertTrue(spectatorItems.isSpectatorToggleItem(english));
        assertTrue(spectatorItems.isSpectatorToggleItem(german), "an item handed out in German works the same");
        assertFalse(spectatorItems.isSpectatorToggleItem(RecordingItemStack.named(Material.COMPASS, "Spectator mode")),
                "a compass that only carries the name is no spectator item");
        assertFalse(spectatorItems.isSpectatorToggleItem(RecordingItemStack.named(Material.COMPASS, "Zuschauermodus")));
        assertFalse(spectatorItems.isSpectatorToggleItem(ArenaGapCompass.labelled(
                new RecordingItemStack(ArenaGapCompass.MATERIAL, 1), null,
                new ArenaGapTarget("event", 1, 64, 1))), "the compass of the gap help is no spectator item");
    }

    @Test
    void recognizesTheHeightMarkerByItsMarkerInEitherLanguage() {
        Player admin = silentAdmin();
        RecordingItemStack english = (RecordingItemStack) markerService.switchMarkedEnd(admin,
                new RecordingItemStack(ArenaHeightMarkerService.MARKER_MATERIAL,
                        ArenaHeightMarkerService.MARKER_AMOUNT));
        Messages.use(MessageCatalogLoader.shippedCatalog("de"));
        RecordingItemStack german = (RecordingItemStack) markerService.switchMarkedEnd(admin, english);

        assertEquals("Height marker: highest point", english.getName());
        assertEquals("Höhenmarke: tiefster Punkt", german.getName());
        assertTrue(markerService.isMarkerItem(english));
        assertTrue(markerService.isMarkerItem(german), "a marker labelled in German works the same");
        english.getItemMeta().displayName(Component.text("Höhenmarke: höchster Punkt"));
        assertTrue(markerService.isMarkerItem(english), "a renamed marker keeps working");
        assertFalse(markerService.isMarkerItem(RecordingItemStack.named(ArenaHeightMarkerService.MARKER_MATERIAL,
                "Height marker: lowest point")), "a block that only carries the name is no height marker");
        assertFalse(markerService.isMarkerItem(RecordingItemStack.named(ArenaHeightMarkerService.MARKER_MATERIAL,
                "Höhenmarke: tiefster Punkt")));
    }

    private RecordingItemStack labelledSpectatorItem() {
        return (RecordingItemStack) spectatorItems.labelled(new RecordingItemStack(Material.COMPASS, 1));
    }

    private static Player silentAdmin() {
        UUID playerId = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxyInstance, method, arguments) -> switch (method.getName()) {
                    case "sendMessage" -> null;
                    case "getUniqueId" -> playerId;
                    case "isOnline" -> true;
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "equals" -> proxyInstance == arguments[0];
                    case "toString" -> "silent admin";
                    default -> throw new UnsupportedOperationException("Player." + method.getName());
                });
    }
}
