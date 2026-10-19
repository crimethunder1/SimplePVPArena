package de.simplepvparena.kit;

import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitLibraryLoaderNoticesTest {

    private static final String BROKEN_FILE_NAME = "kits.yml.broken";

    private final List<String> adminNotices = new ArrayList<>();

    KitLibraryLoaderNoticesTest() {
        SharedBukkitServer.install();
    }

    @Test
    @UseLanguage("en")
    void everyNoticeOfTheLoaderReadsInEnglish() throws InvalidConfigurationException {
        assertEquals(List.of(
                "The kit of config.yml was taken over into kits.yml as the kit default and is the default kit. The "
                        + "kit section of config.yml is not read any more, the kits are managed in the kit menu of "
                        + "/tournament",
                "The kit section of config.yml holds no item that could be read, so no kit was taken over into "
                        + "kits.yml. There is no kit and every fighter starts a round with an empty kit until a kit "
                        + "is created in the kit menu of /tournament",
                "The kits in kits.yml could not be read: unknown format-version 2. The file was kept as "
                        + "kits.yml.broken, there is no kit and every fighter starts a round with an empty kit until "
                        + "a kit is created in the kit menu of /tournament",
                "The kits in kits.yml could not be read: unknown reason. The file was kept as kits.yml.broken, there "
                        + "is no kit and every fighter starts a round with an empty kit until a kit is created in the "
                        + "kit menu of /tournament",
                "There is no kits.yml, the kits lie in kits.yml.broken because it could not be read at an earlier "
                        + "start. Repair kits.yml.broken and rename it to kits.yml, or delete it if its kits are to be "
                        + "discarded, then the next start takes the kit section of config.yml over"),
                collectEveryNotice());
    }

    @Test
    @UseLanguage("de")
    void everyNoticeOfTheLoaderReadsInGerman() throws InvalidConfigurationException {
        assertEquals(List.of(
                "Das Kit aus config.yml wurde als Kit default in kits.yml übernommen und ist das Standard-Kit. Der "
                        + "Kit-Abschnitt von config.yml wird nicht mehr gelesen, die Kits werden im Kit-Menü von "
                        + "/tournament verwaltet",
                "Der Kit-Abschnitt von config.yml enthält kein lesbares Item, deshalb wurde kein Kit in kits.yml "
                        + "übernommen. Es gibt kein Kit und jeder Kämpfer startet eine Runde mit einem leeren Kit, bis "
                        + "im Kit-Menü von /tournament ein Kit angelegt wird",
                "Die Kits in kits.yml konnten nicht gelesen werden: unknown format-version 2. Die Datei wurde als "
                        + "kits.yml.broken aufbewahrt, es gibt kein Kit und jeder Kämpfer startet eine Runde mit einem "
                        + "leeren Kit, bis im Kit-Menü von /tournament ein Kit angelegt wird",
                "Die Kits in kits.yml konnten nicht gelesen werden: unbekannter Grund. Die Datei wurde als "
                        + "kits.yml.broken aufbewahrt, es gibt kein Kit und jeder Kämpfer startet eine Runde mit einem "
                        + "leeren Kit, bis im Kit-Menü von /tournament ein Kit angelegt wird",
                "Es gibt keine kits.yml, die Kits liegen in kits.yml.broken, weil die Datei bei einem früheren Start "
                        + "nicht gelesen werden konnte. Repariere kits.yml.broken und benenne sie in kits.yml um, oder "
                        + "lösche sie, wenn ihre Kits verworfen werden sollen, dann übernimmt der nächste Start den "
                        + "Kit-Abschnitt von config.yml"),
                collectEveryNotice());
    }

    private List<String> collectEveryNotice() throws InvalidConfigurationException {
        YamlConfiguration savedKitConfiguration = new YamlConfiguration();
        FakeSerializedKits.wearingAndCarrying(Map.of(KitEquipmentSlot.HEAD, "helmet"), "sword")
                .writeInto(savedKitConfiguration, "kit");
        load(KitLibraryLookup.missing(FileKitLibraryStorage.FILE_NAME),
                savedKitConfiguration.getConfigurationSection("kit"));
        YamlConfiguration unreadableConfiguration = new YamlConfiguration();
        unreadableConfiguration.loadFromString("""
                kit:
                  items:
                    - material: NOT_A_SWORD
                """);
        load(KitLibraryLookup.missing(FileKitLibraryStorage.FILE_NAME),
                unreadableConfiguration.getConfigurationSection("kit"));
        load(KitLibraryLookup.broken(BROKEN_FILE_NAME, "unknown format-version 2"), null);
        load(new KitLibraryLookup(KitLibraryLookup.Outcome.BROKEN, Optional.empty(), BROKEN_FILE_NAME,
                Optional.empty()), null);
        load(KitLibraryLookup.setAside(BROKEN_FILE_NAME), null);
        return adminNotices;
    }

    private void load(KitLibraryLookup lookup, ConfigurationSection legacyKitSection) {
        KitLibraryStorage storage = new KitLibraryStorage() {
            @Override
            public void save(SavedKitLibrary savedKitLibrary) {
            }

            @Override
            public KitLibraryLookup find() {
                return lookup;
            }
        };
        KitLibrary kitLibrary = new KitLibrary(storage, new KitService(), new FakeKitItemSerializer(),
                problem -> { }, adminNotices::add);
        new KitLibraryLoader(storage, kitLibrary, new KitConfigurationLoader(problem -> { },
                new FakeKitItemSerializer(), kitItem -> {
                    throw new IllegalStateException("no item is expected to be read");
                }), adminNotices::add, problem -> { }).load(legacyKitSection);
    }
}
