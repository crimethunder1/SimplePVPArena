package de.simplepvparena;

import de.simplepvparena.kit.KitConfigurationLoader;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitLibraryLoader;
import de.simplepvparena.kit.KitLibraryLookup;
import de.simplepvparena.kit.KitLibraryStorage;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.kit.SavedKitLibrary;
import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplePVPArenaPluginKitLoadingTest {

    private static final String LOAD_FAILURE_NOTICE = "The kits could not be loaded (the plugin folder cannot be "
            + "read), there is no kit until one is created in the kit menu of /tournament";

    private final KitService kitService = new KitService();
    private final List<String> adminNotices = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private final List<LogRecord> logRecords = new ArrayList<>();

    SimplePVPArenaPluginKitLoadingTest() {
        SharedBukkitServer.install();
    }

    @Test
    void reportsAFailedKitLoadingAsAWarningAndGoesOnWithoutAKit() {
        IllegalStateException failure = new IllegalStateException("the plugin folder cannot be read");
        KitLibraryStorage failingStorage = storageFailingWith(failure);
        KitLibrary kitLibrary = new KitLibrary(failingStorage, kitService, new FakeKitItemSerializer(),
                problems::add, adminNotices::add);

        SimplePVPArenaPlugin.loadKitLibrary(loaderFor(failingStorage, kitLibrary),
                new YamlConfiguration().createSection("kit"), recordingLogger(), adminNotices::add);

        assertEquals(1, logRecords.size());
        assertEquals(Level.WARNING, logRecords.get(0).getLevel());
        assertEquals(LOAD_FAILURE_NOTICE, logRecords.get(0).getMessage());
        assertSame(failure, logRecords.get(0).getThrown());
        assertEquals(List.of(LOAD_FAILURE_NOTICE), adminNotices);
        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
    }

    @Test
    void namesTheKindOfFailureWhenItCarriesNoMessage() {
        KitLibraryStorage failingStorage = storageFailingWith(new IllegalStateException());
        KitLibrary kitLibrary = new KitLibrary(failingStorage, kitService, new FakeKitItemSerializer(),
                problems::add, adminNotices::add);

        SimplePVPArenaPlugin.loadKitLibrary(loaderFor(failingStorage, kitLibrary), null, recordingLogger(),
                adminNotices::add);

        assertEquals(List.of("The kits could not be loaded (IllegalStateException), there is no kit until one is "
                + "created in the kit menu of /tournament"), adminNotices);
        assertEquals(1, logRecords.size());
    }

    @Test
    void reportsNothingWhenTheKitsLoad() {
        KitLibrary kitLibrary = new KitLibrary(KitLibraryStorage.NONE, kitService, new FakeKitItemSerializer(),
                problems::add, adminNotices::add);

        SimplePVPArenaPlugin.loadKitLibrary(loaderFor(KitLibraryStorage.NONE, kitLibrary), null, recordingLogger(),
                adminNotices::add);

        assertEquals(List.of(), logRecords);
        assertEquals(List.of(), adminNotices);
    }

    private KitLibraryLoader loaderFor(KitLibraryStorage storage, KitLibrary kitLibrary) {
        return new KitLibraryLoader(storage, kitLibrary, new KitConfigurationLoader(problems::add,
                new FakeKitItemSerializer(), kitItem -> new EncodableItemStack(kitItem.material().name())),
                adminNotices::add, problems::add);
    }

    private static KitLibraryStorage storageFailingWith(RuntimeException failure) {
        return new KitLibraryStorage() {

            @Override
            public void save(SavedKitLibrary savedKitLibrary) {
            }

            @Override
            public KitLibraryLookup find() {
                throw failure;
            }
        };
    }

    private Logger recordingLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }
}
