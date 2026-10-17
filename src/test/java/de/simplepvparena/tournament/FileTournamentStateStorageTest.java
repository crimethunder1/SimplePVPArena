package de.simplepvparena.tournament;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTournamentStateStorageTest {

    private static final Instant STARTED_AT = Instant.parse("2026-10-17T18:00:00Z");
    private static final Instant SAVED_AT = Instant.parse("2026-10-17T18:42:13.250Z");
    private static final String BROKEN_FILE_NAME = FileTournamentStateStorage.FILE_NAME + ".broken";

    @Test
    void readsBackWhatItSaved(@TempDir Path pluginFolder) throws IOException {
        SavedTournament savedTournament = fiveTeamTournament();
        FileTournamentStateStorage storage = storageIn(pluginFolder);

        storage.save(savedTournament);
        TournamentStateLookup lookup = storage.find();

        assertEquals(TournamentStateLookup.Outcome.FOUND, lookup.outcome());
        assertEquals(FileTournamentStateStorage.FILE_NAME, lookup.fileName());
        assertEquals(Optional.of(savedTournament), lookup.savedTournament());
        assertEquals(Optional.empty(), lookup.failureReason());
    }

    @Test
    void leavesNoTemporaryFileBehind(@TempDir Path pluginFolder) throws IOException {
        FileTournamentStateStorage storage = storageIn(pluginFolder);

        storage.save(fiveTeamTournament());
        storage.save(fiveTeamTournament());

        assertEquals(List.of(FileTournamentStateStorage.FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void writesTheEmptySlotOfAByeExplicitly(@TempDir Path pluginFolder) throws IOException {
        storageIn(pluginFolder).save(fiveTeamTournament());

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME).toFile());
        List<Map<?, ?>> firstRound = configuration.getMapList("first-round");

        assertEquals(4, firstRound.size());
        assertEquals(Map.of("match-id", "round1-match1", "first", "Team01", "second", ""), firstRound.get(0));
        assertEquals(1, configuration.getInt("format-version"));
        assertEquals("PAUSED", configuration.getString("state"));
        assertEquals(STARTED_AT.toString(), configuration.getString("started-at"));
    }

    @Test
    void reportsAMissingFile(@TempDir Path pluginFolder) {
        TournamentStateLookup lookup = storageIn(pluginFolder).find();

        assertEquals(TournamentStateLookup.Outcome.MISSING, lookup.outcome());
        assertEquals(FileTournamentStateStorage.FILE_NAME, lookup.fileName());
        assertTrue(lookup.savedTournament().isEmpty());
    }

    @Test
    void movesAFileItCannotParseAside(@TempDir Path pluginFolder) throws IOException {
        writeStateFile(pluginFolder, "format-version: 1\nfirst-round: [\n");

        TournamentStateLookup lookup = storageIn(pluginFolder).find();

        assertEquals(TournamentStateLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(BROKEN_FILE_NAME, lookup.fileName());
        assertTrue(lookup.failureReason().isPresent());
        assertEquals(List.of(BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void movesAFileWithAnUnknownFormatVersionAside(@TempDir Path pluginFolder) throws IOException {
        FileTournamentStateStorage storage = storageIn(pluginFolder);
        storage.save(fiveTeamTournament());
        Path stateFile = pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME);
        writeStateFile(pluginFolder, Files.readString(stateFile).replace("format-version: 1", "format-version: 2"));

        TournamentStateLookup lookup = storage.find();

        assertEquals(TournamentStateLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(BROKEN_FILE_NAME, lookup.fileName());
        assertEquals(Optional.of("unknown format-version 2"), lookup.failureReason());
    }

    @Test
    void movesAFileWhoseResultsDoNotFitTheBracketAside(@TempDir Path pluginFolder) throws IOException {
        writeStateFile(pluginFolder, """
                format-version: 1
                started-at: '2026-10-17T18:00:00Z'
                saved-at: '2026-10-17T18:42:13Z'
                state: RUNNING
                first-round:
                - match-id: round1-match1
                  first: Alpha
                  second: Bravo
                - match-id: round1-match2
                  first: Charlie
                  second: Delta
                results:
                - match-id: round1-match1
                  winner: Charlie
                """);

        TournamentStateLookup lookup = storageIn(pluginFolder).find();

        assertEquals(TournamentStateLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(Optional.of("Charlie does not take part in match round1-match1"), lookup.failureReason());
        assertTrue(Files.isRegularFile(pluginFolder.resolve(BROKEN_FILE_NAME)));
    }

    @Test
    void movesAFileWithAStateOtherThanRunningOrPausedAside(@TempDir Path pluginFolder) throws IOException {
        FileTournamentStateStorage storage = storageIn(pluginFolder);
        storage.save(fiveTeamTournament());
        Path stateFile = pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME);
        writeStateFile(pluginFolder, Files.readString(stateFile).replace("state: PAUSED", "state: FINISHED"));

        TournamentStateLookup lookup = storage.find();

        assertEquals(TournamentStateLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(Optional.of("state must be RUNNING or PAUSED, not FINISHED"), lookup.failureReason());
    }

    @Test
    void overwritesABrokenFileThatIsAlreadyThere(@TempDir Path pluginFolder) throws IOException {
        Files.writeString(pluginFolder.resolve(BROKEN_FILE_NAME), "the older broken file", StandardCharsets.UTF_8);
        writeStateFile(pluginFolder, "the newer broken file");

        TournamentStateLookup lookup = storageIn(pluginFolder).find();

        assertEquals(TournamentStateLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals("the newer broken file", Files.readString(pluginFolder.resolve(BROKEN_FILE_NAME)));
        assertEquals(List.of(BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void readsAnEmptySlotThatIsMissingOrNullAndIgnoresAnUnknownDecisionKind(@TempDir Path pluginFolder)
            throws IOException {
        writeStateFile(pluginFolder, """
                format-version: 1
                started-at: '2026-10-17T18:00:00Z'
                saved-at: '2026-10-17T18:42:13Z'
                state: RUNNING
                first-round:
                - match-id: round1-match1
                  first: Alpha
                - match-id: round1-match2
                  first: Bravo
                  second: null
                results: []
                """);

        TournamentStateLookup lookup = storageIn(pluginFolder).find();

        assertEquals(TournamentStateLookup.Outcome.FOUND, lookup.outcome(), lookup.failureReason().toString());
        SavedTournament savedTournament = lookup.savedTournament().orElseThrow();
        assertEquals(Optional.empty(), savedTournament.firstRound().get(0).secondTeamName());
        assertEquals(Optional.empty(), savedTournament.firstRound().get(1).secondTeamName());
        assertTrue(savedTournament.restoreBracket().getFinalMatch().isPlayable());

        writeStateFile(pluginFolder, """
                format-version: 1
                started-at: '2026-10-17T18:00:00Z'
                saved-at: '2026-10-17T18:42:13Z'
                state: RUNNING
                first-round:
                - match-id: round1-match1
                  first: Alpha
                  second: Bravo
                - match-id: round1-match2
                  first: Charlie
                  second: Delta
                results:
                - match-id: round1-match1
                  winner: Alpha
                  decided-by: lucky
                """);

        SavedMatchResult result = storageIn(pluginFolder).find().savedTournament().orElseThrow().results().get(0);
        assertEquals(new SavedMatchResult("round1-match1", "Alpha", Optional.empty()), result);
    }

    @Test
    void deletesTheFile(@TempDir Path pluginFolder) throws IOException {
        FileTournamentStateStorage storage = storageIn(pluginFolder);
        storage.save(fiveTeamTournament());

        storage.delete();
        storage.delete();

        assertEquals(List.of(), fileNamesIn(pluginFolder));
        assertEquals(TournamentStateLookup.Outcome.MISSING, storage.find().outcome());
    }

    @Test
    void throwsWhenTheFolderCannotBeWritten(@TempDir Path pluginFolder) throws IOException {
        Path folderThatIsAFile = pluginFolder.resolve("plugin-folder");
        Files.writeString(folderThatIsAFile, "a file in place of the folder", StandardCharsets.UTF_8);
        FileTournamentStateStorage storage = new FileTournamentStateStorage(
                folderThatIsAFile.resolve(FileTournamentStateStorage.FILE_NAME));

        assertThrows(IOException.class, () -> storage.save(fiveTeamTournament()));
        assertEquals(List.of("plugin-folder"), fileNamesIn(pluginFolder));
    }

    @Test
    void createsTheFolderWhenItIsNotThereYet(@TempDir Path pluginFolder) throws IOException {
        FileTournamentStateStorage storage = new FileTournamentStateStorage(
                pluginFolder.resolve("SimplePVPArena").resolve(FileTournamentStateStorage.FILE_NAME));

        storage.save(fiveTeamTournament());

        assertEquals(TournamentStateLookup.Outcome.FOUND, storage.find().outcome());
        assertFalse(Files.exists(pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME)));
    }

    private static SavedTournament fiveTeamTournament() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Team01", "Team02", "Team03", "Team04",
                "Team05"));
        bracket.recordWinner("round1-match2", "Team04");
        bracket.recordWinner("round2-match2", "Team03");
        return SavedTournament.capture(bracket, TournamentState.PAUSED, STARTED_AT, SAVED_AT,
                Map.of("round1-match2", MatchDecisionKind.PLAYED, "round2-match2", MatchDecisionKind.WALKOVER));
    }

    private static FileTournamentStateStorage storageIn(Path pluginFolder) {
        return new FileTournamentStateStorage(pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME));
    }

    private static void writeStateFile(Path pluginFolder, String contents) throws IOException {
        Files.writeString(pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME), contents,
                StandardCharsets.UTF_8);
    }

    private static List<String> fileNamesIn(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }
}
