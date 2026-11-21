package de.simplepvparena.visual;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@NullMarked
public final class ItemsAdderContentInstaller {

    public static final String RESOURCE_ROOT = "itemsadder/simplepvparena/";
    public static final String ITEMS_ADDER_FOLDER = "ItemsAdder";
    public static final String NAMESPACE = "simplepvparena";
    public static final List<String> OWNED_FOLDERS = List.of(
            "resourcepack/assets/simplepvparena/models/item/board",
            "resourcepack/assets/simplepvparena/textures/item/board",
            "resourcepack/assets/simplepvparena/models/item/laser",
            "resourcepack/assets/simplepvparena/textures/item/laser");

    private final Path namespaceFolder;

    public ItemsAdderContentInstaller(Path namespaceFolder) {
        this.namespaceFolder = Objects.requireNonNull(namespaceFolder, "namespaceFolder");
    }

    public static Path namespaceFolder(Path pluginsFolder) {
        return pluginsFolder.resolve(ITEMS_ADDER_FOLDER).resolve("contents").resolve(NAMESPACE);
    }

    public static Map<String, byte[]> readBundledFiles(Path pluginJar) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipFile jar = new ZipFile(pluginJar.toFile())) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(RESOURCE_ROOT)) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    files.put(entry.getName().substring(RESOURCE_ROOT.length()), input.readAllBytes());
                }
            }
        }
        return files;
    }

    public boolean install(Map<String, byte[]> bundledFiles) throws IOException {
        if (bundledFiles.isEmpty() || isUpToDate(bundledFiles)) {
            return false;
        }
        for (String owned : OWNED_FOLDERS) {
            deleteTree(namespaceFolder.resolve(owned));
        }
        for (Map.Entry<String, byte[]> file : bundledFiles.entrySet()) {
            Path target = resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.write(target, file.getValue());
        }
        return true;
    }

    public Path getNamespaceFolder() {
        return namespaceFolder;
    }

    private boolean isUpToDate(Map<String, byte[]> bundledFiles) throws IOException {
        for (Map.Entry<String, byte[]> file : bundledFiles.entrySet()) {
            Path target = resolve(file.getKey());
            if (!Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(target), file.getValue())) {
                return false;
            }
        }
        for (String owned : OWNED_FOLDERS) {
            for (Path present : filesBelow(namespaceFolder.resolve(owned))) {
                String relative = namespaceFolder.relativize(present).toString().replace('\\', '/');
                if (!bundledFiles.containsKey(relative)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Path resolve(String relative) {
        Path target = namespaceFolder.resolve(relative).normalize();
        if (!target.startsWith(namespaceFolder.normalize())) {
            throw new IllegalArgumentException("Bundled file outside of the namespace folder: " + relative);
        }
        return target;
    }

    private static List<Path> filesBelow(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            return new ArrayList<>(walk.filter(Files::isRegularFile).toList());
        }
    }

    private static void deleteTree(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
            });
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        }
    }
}
