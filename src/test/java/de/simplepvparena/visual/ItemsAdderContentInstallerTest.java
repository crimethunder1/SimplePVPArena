package de.simplepvparena.visual;

import de.simplepvparena.placement.BracketBoardDisplays;
import de.simplepvparena.placement.PaperPlacementBoardEntities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemsAdderContentInstallerTest {

    private static final String MODEL = "resourcepack/assets/simplepvparena/models/item/board/bracket_board.json";
    private static final String TEXTURE = "resourcepack/assets/simplepvparena/textures/item/board/bracket_board_0.png";
    private static final String CONFIG = "configs/boards.yml";

    @TempDir
    private Path plugins;

    @Test
    void writesEveryBundledFileIntoTheNamespaceFolderOfItemsAdder() throws IOException {
        ItemsAdderContentInstaller installer = installer();

        assertTrue(installer.install(bundle("new")));

        Path folder = plugins.resolve("ItemsAdder/contents/simplepvparena");
        assertEquals(folder, installer.getNamespaceFolder());
        assertEquals("new " + MODEL, Files.readString(folder.resolve(MODEL)));
        assertEquals("new " + TEXTURE, Files.readString(folder.resolve(TEXTURE)));
        assertEquals("new " + CONFIG, Files.readString(folder.resolve(CONFIG)));
    }

    @Test
    void reportsNoChangeWhenTheInstalledFilesAlreadyMatch() throws IOException {
        ItemsAdderContentInstaller installer = installer();
        installer.install(bundle("same"));

        assertFalse(installer.install(bundle("same")));
    }

    @Test
    void replacesOldBoardFilesAndRemovesBoardFilesTheJarNoLongerShips() throws IOException {
        Path folder = plugins.resolve("ItemsAdder/contents/simplepvparena");
        Path stale = folder.resolve("resourcepack/assets/simplepvparena/textures/item/board/old_board.png");
        Files.createDirectories(stale.getParent());
        Files.writeString(stale, "old");
        Files.createDirectories(folder.resolve(MODEL).getParent());
        Files.writeString(folder.resolve(MODEL), "old model");

        assertTrue(installer().install(bundle("new")));

        assertFalse(Files.exists(stale));
        assertEquals("new " + MODEL, Files.readString(folder.resolve(MODEL)));
    }

    @Test
    void leavesFilesOutsideOfTheBoardFoldersAlone() throws IOException {
        Path folder = plugins.resolve("ItemsAdder/contents/simplepvparena");
        Path ownConfig = folder.resolve("configs/simplepvparena.yml");
        Path ownTexture = folder.resolve("resourcepack/assets/simplepvparena/textures/item/machine_metal.png");
        for (Path own : new Path[] {ownConfig, ownTexture}) {
            Files.createDirectories(own.getParent());
            Files.writeString(own, "kept");
        }

        installer().install(bundle("new"));

        assertEquals("kept", Files.readString(ownConfig));
        assertEquals("kept", Files.readString(ownTexture));
    }

    @Test
    void installsAgainWhenAnInstalledFileWasDeleted() throws IOException {
        ItemsAdderContentInstaller installer = installer();
        installer.install(bundle("new"));
        Files.delete(installer.getNamespaceFolder().resolve(TEXTURE));

        assertTrue(installer.install(bundle("new")));
        assertTrue(Files.exists(installer.getNamespaceFolder().resolve(TEXTURE)));
    }

    @Test
    void refusesAFileThatWouldLandOutsideOfTheNamespaceFolder() {
        Map<String, byte[]> escaping = Map.of("../other/evil.yml", new byte[] {1});

        assertThrows(IllegalArgumentException.class, () -> installer().install(escaping));
    }

    @Test
    void theShippedBoardModelsAreBundledForEveryModelThePluginLooksUp() throws IOException {
        for (String model : new String[] {"standings_board", "standings_board_glow", "bracket_board",
                "bracket_board_glow"}) {
            String path = ItemsAdderContentInstaller.RESOURCE_ROOT
                    + "resourcepack/assets/simplepvparena/models/item/board/" + model + ".json";
            try (InputStream shipped = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(shipped, path);
            }
        }
        try (InputStream config = getClass().getClassLoader().getResourceAsStream(
                ItemsAdderContentInstaller.RESOURCE_ROOT + CONFIG)) {
            assertNotNull(config);
            String text = new String(config.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("namespace: simplepvparena"));
            assertTrue(text.contains("  " + PaperPlacementBoardEntities.MODEL + ":"));
            assertTrue(text.contains("  " + BracketBoardDisplays.MODEL + ":"));
        }
    }

    private ItemsAdderContentInstaller installer() {
        return new ItemsAdderContentInstaller(ItemsAdderContentInstaller.namespaceFolder(plugins));
    }

    private static Map<String, byte[]> bundle(String version) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (String path : new String[] {MODEL, TEXTURE, CONFIG}) {
            files.put(path, (version + " " + path).getBytes(StandardCharsets.UTF_8));
        }
        return files;
    }
}
