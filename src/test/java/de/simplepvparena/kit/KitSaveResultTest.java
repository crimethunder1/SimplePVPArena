package de.simplepvparena.kit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitSaveResultTest {

    @Test
    void countsTheWornAndTheCarriedItemsApart() {
        KitSaveResult result = new KitSaveResult(5, 12, 0);

        assertEquals(17, result.itemCount());
        assertTrue(result.describe().get(0).contains("17 items"));
        assertTrue(result.describe().get(0).contains("5 worn"));
        assertTrue(result.describe().get(0).contains("12 in the inventory"));
    }

    @Test
    void saysWhenTheSavedKitIsHandedOut() {
        assertTrue(new KitSaveResult(0, 1, 0).describe().get(1).contains("next round"));
    }

    @Test
    void staysReadableForASingleItem() {
        assertTrue(new KitSaveResult(0, 1, 0).describe().get(0).contains("1 item,"));
    }

    @Test
    void saysNothingAboutDroppedItemsWhenEveryItemSurvived() {
        assertEquals(2, new KitSaveResult(4, 8, 0).describe().size());
    }

    @Test
    void namesTheItemsThatCouldNotBeReadBack() {
        List<String> lines = new KitSaveResult(4, 8, 2).describe();

        assertEquals(3, lines.size());
        assertTrue(lines.get(2).contains("2 items"));
    }

    @Test
    void countsTheItemsOfTheKitThatIsReallyHandedOut() {
        SerializedKit storedKit = new SerializedKit(Map.of(KitEquipmentSlot.HEAD, "helmet"),
                Map.of(0, "sword", 1, "bow"));

        KitSaveResult result = KitSaveResult.of(storedKit, KitDefinition.empty());

        assertEquals(0, result.itemCount());
        assertEquals(3, result.unreadableItemCount());
    }
}
