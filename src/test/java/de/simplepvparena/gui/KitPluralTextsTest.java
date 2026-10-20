package de.simplepvparena.gui;

import de.simplepvparena.kit.KitEquipmentSlot;
import de.simplepvparena.kit.KitSaveResult;
import de.simplepvparena.message.KitChoiceMessage;
import de.simplepvparena.message.KitMenuMessage;
import de.simplepvparena.message.KitMessage;
import de.simplepvparena.message.MessageCatalog;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitPluralTextsTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int KITS_INFO_SLOT = 47;
    private static final int CHOICE_INFO_SLOT = 49;
    private static final int TIME_LEFT_LINE = 2;
    private static final String SINGULAR_MARK = "[one] ";
    private static final String PLURAL_MARK = "[other] ";
    private static final List<MessageKey> PLURAL_KEYS = List.of(KitMenuMessage.KIT_COUNT,
            KitMenuMessage.WORN_ITEM_COUNT, KitMenuMessage.INVENTORY_ITEM_COUNT, KitMessage.DEFINITION_WORN_ITEMS,
            KitMessage.DEFINITION_INVENTORY_ITEMS, KitMessage.SAVED, KitMessage.UNREADABLE_ITEMS,
            KitChoiceMessage.INFO_TIME_LEFT, KitChoiceMessage.BEGIN, KitChoiceMessage.REMINDER);

    private final AdminMenuFixture fixture = AdminMenuFixture.withKitChoice();

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void everyPluralTextOfTheKitsTakesTheSingularForOneAndThePluralForTwo() {
        Messages.use(catalogWithMarkedPluralForms());

        assertEquals(List.of(
                "[one] Kits: 1",
                "[one] Worn items: 1 of 5",
                "[one] Inventory items: 1 of 36",
                "[one] Worn items: 1 of 5",
                "[one] Inventory items: 1 of 36 slots",
                "[one] Kit Knight saved: 1 item, 0 worn and 1 in the inventory",
                "[one] 1 item could not be read back and is no part of the kit, the log names it",
                "[one] Time left: 1 second",
                "[other] Kits: 2",
                "[other] Worn items: 2 of 5",
                "[other] Inventory items: 2 of 36",
                "[other] Worn items: 2 of 5",
                "[other] Inventory items: 2 of 36 slots",
                "[other] Kit Knight saved: 2 items, 1 worn and 1 in the inventory",
                "[other] 2 items could not be read back and are no part of the kit, the log names them",
                "[other] Time left: 2 seconds",
                "[other] Match round1-match1 (Alpha against Bravo): choose the kit within 30 seconds, otherwise the "
                        + "default kit Single is used",
                "[other] 10 seconds left to choose the kit for match round1-match1"), collectEveryPluralText());
    }

    @Test
    @UseLanguage("en")
    void theShippedEnglishTextsCountOneAndTwo() {
        assertEquals(List.of(
                "Kits: 1",
                "Worn items: 1 of 5",
                "Inventory items: 1 of 36",
                "Worn items: 1 of 5",
                "Inventory items: 1 of 36 slots",
                "Kit Knight saved: 1 item, 0 worn and 1 in the inventory",
                "1 item could not be read back and is no part of the kit, the log names it",
                "Time left: 1 second",
                "Kits: 2",
                "Worn items: 2 of 5",
                "Inventory items: 2 of 36",
                "Worn items: 2 of 5",
                "Inventory items: 2 of 36 slots",
                "Kit Knight saved: 2 items, 1 worn and 1 in the inventory",
                "2 items could not be read back and are no part of the kit, the log names them",
                "Time left: 2 seconds",
                "Match round1-match1 (Alpha against Bravo): choose the kit within 30 seconds, otherwise the default "
                        + "kit Single is used",
                "10 seconds left to choose the kit for match round1-match1",
                "Match round1-match1 (Alpha against Bravo): choose the kit within 1 second, otherwise the default "
                        + "kit Single is used",
                "1 second left to choose the kit for match round1-match1"), collectEveryShippedText());
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextsCountOneAndTwo() {
        assertEquals(List.of(
                "Kits: 1",
                "Getragene Items: 1 von 5",
                "Items im Inventar: 1 von 36",
                "Getragene Items: 1 von 5",
                "Items im Inventar: 1 von 36 Plätzen",
                "Kit Knight gespeichert: 1 Item, 0 getragen und 1 im Inventar",
                "1 Item ließ sich nicht zurücklesen und gehört nicht zum Kit, das Log nennt es",
                "Restzeit: 1 Sekunde",
                "Kits: 2",
                "Getragene Items: 2 von 5",
                "Items im Inventar: 2 von 36",
                "Getragene Items: 2 von 5",
                "Items im Inventar: 2 von 36 Plätzen",
                "Kit Knight gespeichert: 2 Items, 1 getragen und 1 im Inventar",
                "2 Items ließen sich nicht zurücklesen und gehören nicht zum Kit, das Log nennt sie",
                "Restzeit: 2 Sekunden",
                "Match round1-match1 (Alpha gegen Bravo): Wähle das Kit innerhalb von 30 Sekunden, sonst gilt das "
                        + "Standard-Kit Single",
                "Noch 10 Sekunden, um das Kit für Match round1-match1 zu wählen",
                "Match round1-match1 (Alpha gegen Bravo): Wähle das Kit innerhalb von 1 Sekunde, sonst gilt das "
                        + "Standard-Kit Single",
                "Noch 1 Sekunde, um das Kit für Match round1-match1 zu wählen"), collectEveryShippedText());
    }

    private List<String> collectEveryShippedText() {
        List<String> texts = collectEveryPluralText();
        texts.add(Messages.plural(KitChoiceMessage.BEGIN, 1, Placeholder.of("match", "round1-match1"),
                Placeholder.of("firstTeam", "Alpha"), Placeholder.of("secondTeam", "Bravo"),
                Placeholder.of("kit", "Single")));
        texts.add(Messages.plural(KitChoiceMessage.REMINDER, 1, Placeholder.of("match", "round1-match1")));
        return texts;
    }

    private List<String> collectEveryPluralText() {
        List<String> texts = new ArrayList<>();
        fixture.kitLibrary().createKit("Single", FakeSerializedKits.wearingAndCarrying(
                Map.of(KitEquipmentSlot.HEAD, "helmet"), "sword"));
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        texts.add(loreOf(KITS_INFO_SLOT).get(0));
        texts.addAll(loreOf(0).subList(0, 2));
        texts.addAll(fixture.describeKit());
        texts.addAll(new KitSaveResult("Knight", true, 0, 1, 1).describe().subList(0, 1));
        texts.addAll(new KitSaveResult("Knight", true, 0, 1, 1).describe().subList(2, 3));
        fixture.kitLibrary().createKit("Double", FakeSerializedKits.wearingAndCarrying(
                Map.of(KitEquipmentSlot.HEAD, "helmet", KitEquipmentSlot.FEET, "boots"), "sword", "bow"));
        fixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> { });
        fixture.kitChoiceScheduler().advanceSeconds(29);
        fixture.refillLastMenu();
        texts.add(loreOf(CHOICE_INFO_SLOT).get(TIME_LEFT_LINE));
        fixture.kitChoiceService().cancel();

        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        texts.add(loreOf(KITS_INFO_SLOT).get(0));
        texts.addAll(loreOf(1).subList(0, 2));
        fixture.kitLibrary().setDefaultKit("Double");
        texts.addAll(fixture.describeKit());
        texts.addAll(new KitSaveResult("Knight", true, 1, 1, 2).describe().subList(0, 1));
        texts.addAll(new KitSaveResult("Knight", true, 1, 1, 2).describe().subList(2, 3));
        fixture.kitLibrary().setDefaultKit("Single");
        fixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> { });
        fixture.kitChoiceScheduler().advanceSeconds(28);
        fixture.refillLastMenu();
        texts.add(loreOf(CHOICE_INFO_SLOT).get(TIME_LEFT_LINE));
        texts.addAll(fixture.adminNotifications().subList(fixture.adminNotifications().size() - 2,
                fixture.adminNotifications().size()));
        return texts;
    }

    private List<String> loreOf(int slot) {
        return fixture.lastMenu().findTile(slot).orElseThrow().loreLines();
    }

    private static MessageCatalog catalogWithMarkedPluralForms() {
        MessageCatalog shippedEnglish = MessageCatalogLoader.shippedCatalog(MessageCatalogLoader.DEFAULT_LANGUAGE);
        Map<String, List<String>> markedForms = new HashMap<>();
        for (MessageKey key : PLURAL_KEYS) {
            mark(shippedEnglish, markedForms, key.path() + Messages.SINGULAR_SUFFIX, SINGULAR_MARK);
            mark(shippedEnglish, markedForms, key.path() + Messages.PLURAL_SUFFIX, PLURAL_MARK);
        }
        return new MessageCatalog() {
            @Override
            public Optional<List<String>> find(String path) {
                return Optional.ofNullable(markedForms.get(path)).or(() -> shippedEnglish.find(path));
            }

            @Override
            public Set<String> paths() {
                return shippedEnglish.paths();
            }
        };
    }

    private static void mark(MessageCatalog catalog, Map<String, List<String>> markedForms, String path,
                             String mark) {
        markedForms.put(path, catalog.find(path).orElseThrow().stream().map(line -> mark + line).toList());
    }
}
