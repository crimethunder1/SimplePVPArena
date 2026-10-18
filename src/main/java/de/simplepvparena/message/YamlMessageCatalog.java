package de.simplepvparena.message;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class YamlMessageCatalog implements MessageCatalog {

    private final Map<String, List<String>> linesByPath;

    private YamlMessageCatalog(Map<String, List<String>> linesByPath) {
        this.linesByPath = Collections.unmodifiableMap(new LinkedHashMap<>(linesByPath));
    }

    public static YamlMessageCatalog read(Reader reader) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(reader);
        Map<String, List<String>> linesByPath = new LinkedHashMap<>();
        for (String path : configuration.getKeys(true)) {
            if (configuration.isConfigurationSection(path)) {
                continue;
            }
            if (configuration.isList(path)) {
                linesByPath.put(path, configuration.getList(path, List.of()).stream()
                        .map(String::valueOf)
                        .toList());
            } else {
                linesByPath.put(path, List.of(String.valueOf(configuration.get(path))));
            }
        }
        return new YamlMessageCatalog(linesByPath);
    }

    public static YamlMessageCatalog readFile(Path file) throws IOException, InvalidConfigurationException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    public static YamlMessageCatalog readResource(String resourcePath) {
        try (InputStream resource = YamlMessageCatalog.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resource == null) {
                throw new IllegalStateException(resourcePath + " is missing from the plugin");
            }
            return read(new InputStreamReader(resource, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException failure) {
            throw new IllegalStateException(resourcePath + " could not be read", failure);
        }
    }

    @Override
    public Optional<List<String>> find(String path) {
        return Optional.ofNullable(linesByPath.get(path));
    }

    @Override
    public Set<String> paths() {
        return linesByPath.keySet();
    }
}
