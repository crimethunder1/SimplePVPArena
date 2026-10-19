package de.simplepvparena.kit;

import de.simplepvparena.message.KitMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class KitLibrary {

    public static final int MAXIMUM_NAME_LENGTH = 32;

    private final KitLibraryStorage storage;
    private final KitService kitService;
    private final KitSerialization kitSerialization;
    private final Consumer<String> itemProblemReporter;
    private final Consumer<String> writeFailureReporter;
    private final Map<String, LibraryKit> kitsByLowerCaseName = new LinkedHashMap<>();

    private String defaultKitLowerCaseName;
    private String chosenKitLowerCaseName;

    public KitLibrary(KitLibraryStorage storage, KitService kitService, KitItemSerializer kitItemSerializer,
                      Consumer<String> itemProblemReporter, Consumer<String> writeFailureReporter) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.kitSerialization = new KitSerialization(kitItemSerializer);
        this.itemProblemReporter = Objects.requireNonNull(itemProblemReporter, "itemProblemReporter");
        this.writeFailureReporter = Objects.requireNonNull(writeFailureReporter, "writeFailureReporter");
    }

    public static String describeNameRule() {
        return Messages.text(KitMessage.NAME_RULE, Placeholder.of("maximumLength", MAXIMUM_NAME_LENGTH));
    }

    public static String describeUnknownKit(String kitName) {
        return Messages.text(KitMessage.UNKNOWN_KIT, Placeholder.of("kit", kitName));
    }

    public static Optional<String> findNameRejection(String kitName) {
        if (kitName == null || kitName.isBlank()) {
            return Optional.of(Messages.text(KitMessage.NAME_MISSING, Placeholder.of("rule", describeNameRule())));
        }
        if (!isAllowedName(kitName)) {
            return Optional.of(Messages.text(KitMessage.NAME_NOT_ALLOWED, Placeholder.of("kit", kitName),
                    Placeholder.of("rule", describeNameRule())));
        }
        return Optional.empty();
    }

    public List<String> getKitNames() {
        List<String> kitNames = new ArrayList<>();
        kitsByLowerCaseName.values().forEach(kit -> kitNames.add(kit.name()));
        return List.copyOf(kitNames);
    }

    public int getKitCount() {
        return kitsByLowerCaseName.size();
    }

    public Optional<LibraryKit> findKit(String kitName) {
        if (kitName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(kitsByLowerCaseName.get(toLowerCase(kitName)));
    }

    public Optional<LibraryKit> findDefaultKit() {
        return defaultKitLowerCaseName == null
                ? Optional.empty()
                : Optional.ofNullable(kitsByLowerCaseName.get(defaultKitLowerCaseName));
    }

    public Optional<String> getDefaultKitName() {
        return findDefaultKit().map(LibraryKit::name);
    }

    public boolean isDefaultKit(String kitName) {
        return kitName != null && toLowerCase(kitName).equals(defaultKitLowerCaseName);
    }

    public Optional<String> findCreationRejection(String kitName) {
        Optional<String> nameRejection = findNameRejection(kitName);
        if (nameRejection.isPresent()) {
            return nameRejection;
        }
        return findKit(kitName).map(existingKit -> Messages.text(KitMessage.ALREADY_EXISTS,
                Placeholder.of("kit", existingKit.name())));
    }

    public Optional<String> findDefaultRejection(String kitName) {
        Optional<LibraryKit> kit = findKit(kitName);
        if (kit.isEmpty()) {
            return Optional.of(describeUnknownKit(kitName));
        }
        if (isDefaultKit(kitName)) {
            return Optional.of(Messages.text(KitMessage.ALREADY_DEFAULT_KIT, Placeholder.of("kit", kit.get().name())));
        }
        return Optional.empty();
    }

    public Optional<String> findDeletionRejection(String kitName) {
        Optional<LibraryKit> kit = findKit(kitName);
        if (kit.isEmpty()) {
            return Optional.of(describeUnknownKit(kitName));
        }
        if (kitsByLowerCaseName.size() == 1) {
            return Optional.of(Messages.text(KitMessage.LAST_KIT, Placeholder.of("kit", kit.get().name())));
        }
        if (isDefaultKit(kitName)) {
            return Optional.of(Messages.text(KitMessage.DEFAULT_KIT_NOT_DELETABLE,
                    Placeholder.of("kit", kit.get().name())));
        }
        return Optional.empty();
    }

    public LibraryKit createKit(String kitName, SerializedKit serializedKit) {
        Objects.requireNonNull(serializedKit, "serializedKit");
        Optional<String> rejection = findCreationRejection(kitName);
        if (rejection.isPresent()) {
            throw new IllegalArgumentException(rejection.get());
        }
        LibraryKit kit = decode(kitName, serializedKit);
        kitsByLowerCaseName.put(toLowerCase(kitName), kit);
        boolean becameDefault = defaultKitLowerCaseName == null;
        if (becameDefault) {
            defaultKitLowerCaseName = toLowerCase(kitName);
        }
        write();
        if (becameDefault) {
            handOutActiveKit();
        }
        return kit;
    }

    public LibraryKit replaceKit(String kitName, SerializedKit serializedKit) {
        Objects.requireNonNull(serializedKit, "serializedKit");
        LibraryKit existingKit = findKit(kitName)
                .orElseThrow(() -> new IllegalArgumentException(describeUnknownKit(kitName)));
        LibraryKit kit = decode(existingKit.name(), serializedKit);
        kitsByLowerCaseName.put(toLowerCase(kitName), kit);
        write();
        if (isDefaultKit(kitName) || isChosenKit(kitName)) {
            handOutActiveKit();
        }
        return kit;
    }

    public void deleteKit(String kitName) {
        if (findKit(kitName).isEmpty()) {
            throw new IllegalArgumentException(describeUnknownKit(kitName));
        }
        Optional<String> rejection = findDeletionRejection(kitName);
        if (rejection.isPresent()) {
            throw new IllegalStateException(rejection.get());
        }
        kitsByLowerCaseName.remove(toLowerCase(kitName));
        write();
        if (isChosenKit(kitName)) {
            chosenKitLowerCaseName = null;
            handOutActiveKit();
        }
    }

    public void setDefaultKit(String kitName) {
        if (findKit(kitName).isEmpty()) {
            throw new IllegalArgumentException(describeUnknownKit(kitName));
        }
        if (isDefaultKit(kitName)) {
            return;
        }
        defaultKitLowerCaseName = toLowerCase(kitName);
        write();
        handOutActiveKit();
    }

    public void activate(String kitName) {
        LibraryKit kit = findKit(kitName)
                .orElseThrow(() -> new IllegalArgumentException(describeUnknownKit(kitName)));
        chosenKitLowerCaseName = toLowerCase(kit.name());
        kitService.setKitDefinition(kit.kitDefinition());
    }

    public void activateDefaultKit() {
        chosenKitLowerCaseName = null;
        handOutActiveKit();
    }

    public static Optional<String> findInconsistency(SavedKitLibrary savedKitLibrary) {
        List<String> lowerCaseNames = new ArrayList<>();
        for (SavedKit savedKit : savedKitLibrary.kits()) {
            Optional<String> nameRejection = findNameRejection(savedKit.name());
            if (nameRejection.isPresent()) {
                return nameRejection;
            }
            if (lowerCaseNames.contains(toLowerCase(savedKit.name()))) {
                return Optional.of(Messages.text(KitMessage.NAME_USED_TWICE, Placeholder.of("kit", savedKit.name())));
            }
            lowerCaseNames.add(toLowerCase(savedKit.name()));
        }
        Optional<String> defaultKitName = savedKitLibrary.defaultKitName();
        if (defaultKitName.isEmpty()) {
            return lowerCaseNames.isEmpty()
                    ? Optional.empty()
                    : Optional.of(Messages.text(KitMessage.NO_DEFAULT_KIT_NAMED));
        }
        if (!lowerCaseNames.contains(toLowerCase(defaultKitName.get()))) {
            return Optional.of(Messages.text(KitMessage.DEFAULT_KIT_UNKNOWN,
                    Placeholder.of("kit", defaultKitName.get())));
        }
        return Optional.empty();
    }

    public void restore(SavedKitLibrary savedKitLibrary) {
        Optional<String> inconsistency = findInconsistency(savedKitLibrary);
        if (inconsistency.isPresent()) {
            throw new IllegalArgumentException(inconsistency.get());
        }
        kitsByLowerCaseName.clear();
        for (SavedKit savedKit : savedKitLibrary.kits()) {
            kitsByLowerCaseName.put(toLowerCase(savedKit.name()), decode(savedKit.name(), savedKit.serializedKit()));
        }
        defaultKitLowerCaseName = savedKitLibrary.defaultKitName().map(KitLibrary::toLowerCase).orElse(null);
        activateDefaultKit();
    }

    SavedKitLibrary capture() {
        List<SavedKit> savedKits = new ArrayList<>();
        kitsByLowerCaseName.values().forEach(kit -> savedKits.add(new SavedKit(kit.name(), kit.serializedKit())));
        return new SavedKitLibrary(savedKits, getDefaultKitName());
    }

    private LibraryKit decode(String kitName, SerializedKit serializedKit) {
        return new LibraryKit(kitName, serializedKit,
                kitSerialization.toDefinition(serializedKit, problem -> itemProblemReporter.accept(
                        "Kit " + kitName + ": " + problem)));
    }

    private void handOutActiveKit() {
        kitService.setKitDefinition(findChosenKit().or(this::findDefaultKit).map(LibraryKit::kitDefinition)
                .orElse(KitDefinition.empty()));
    }

    private Optional<LibraryKit> findChosenKit() {
        return chosenKitLowerCaseName == null
                ? Optional.empty()
                : Optional.ofNullable(kitsByLowerCaseName.get(chosenKitLowerCaseName));
    }

    private boolean isChosenKit(String kitName) {
        return kitName != null && toLowerCase(kitName).equals(chosenKitLowerCaseName);
    }

    private void write() {
        try {
            storage.save(capture());
        } catch (IOException failure) {
            writeFailureReporter.accept(Messages.text(KitMessage.WRITE_FAILED,
                    Placeholder.of("file", FileKitLibraryStorage.FILE_NAME),
                    Placeholder.of("reason", failure.getMessage())));
        }
    }

    private static boolean isAllowedName(String kitName) {
        int nameLength = kitName.codePointCount(0, kitName.length());
        return nameLength >= 1 && nameLength <= MAXIMUM_NAME_LENGTH
                && kitName.codePoints().allMatch(KitLibrary::isAllowedNameCharacter);
    }

    private static boolean isAllowedNameCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '-' || codePoint == '_';
    }

    private static String toLowerCase(String kitName) {
        return kitName.toLowerCase(Locale.ROOT);
    }
}
