package de.simplepvparena.message;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.FinalMatchGate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileReferencesTest {

    private static final Pattern QUOTED_TILE = Pattern.compile("\"([^\"]+)\"");
    private static final String ARENA_MENU = "arena-menu";
    private static final String MATCH_MENU = "match-menu";
    private static final String KIT_MENU = "kit-menu";

    @Test
    @UseLanguage("en")
    void everyEnglishReferenceQuotesATileOfTheMenuItNames() {
        assertEquals(List.of(), findReferencesWithoutTheirTile("en"));
    }

    @Test
    @UseLanguage("de")
    void everyGermanReferenceQuotesATileOfTheMenuItNames() {
        assertEquals(List.of(), findReferencesWithoutTheirTile("de"));
    }

    @Test
    void everyTitleASentenceInsertsByItselfIsATileOfTheMenuTheSentenceNamesInBothFiles() {
        Map<MessageKey, String> titlesToMenus = new LinkedHashMap<>();
        titlesToMenus.put(KitMenuMessage.CREATE_TITLE, KIT_MENU);
        titlesToMenus.put(ArenaMenuMessage.START_GATE_TITLE, ARENA_MENU);
        titlesToMenus.put(MatchMenuMessage.DECIDE_WAITING_MATCH_TITLE, MATCH_MENU);
        List<String> misplacedTitles = new ArrayList<>();

        titlesToMenus.forEach((title, menu) -> {
            for (String language : MessageCatalogLoader.SHIPPED_LANGUAGES) {
                if (!isTileTitleOf(title.path(), menu)
                        || MessageCatalogLoader.readShipped(language).find(title.path()).isEmpty()) {
                    misplacedTitles.add(language + " " + title.path() + " is no tile of " + menu);
                }
            }
        });

        assertEquals(List.of(), misplacedTitles);
    }

    private static List<String> findReferencesWithoutTheirTile(String language) {
        Map<String, String> referencesToMenus = new LinkedHashMap<>();
        for (String reference : List.of(Arena.describeCalculateAction(), Arena.describeDiagnoseAction(),
                Arena.describeTribuneSpawnAction(), Arena.describeMarkHeightAction(), Arena.describeCleanAction())) {
            referencesToMenus.put(reference, ARENA_MENU);
        }
        for (MatchMenuMessage tileTitle : List.of(MatchMenuMessage.FORCE_START_TITLE, MatchMenuMessage.CONTINUE_TITLE,
                MatchMenuMessage.FORCE_RESULT_TITLE)) {
            referencesToMenus.put(MatchService.describeMatchMenuTile(tileTitle), MATCH_MENU);
        }
        referencesToMenus.put(FinalMatchGate.describeFinalStartAction(), MATCH_MENU);
        List<String> referencesWithoutTheirTile = new ArrayList<>();

        referencesToMenus.forEach((reference, menu) -> {
            Matcher quotedTile = QUOTED_TILE.matcher(reference);
            if (!quotedTile.find() || !tileTitlesOf(language, menu).contains(quotedTile.group(1))) {
                referencesWithoutTheirTile.add(reference + " names no tile of " + menu + " in " + language);
            }
        });

        return referencesWithoutTheirTile;
    }

    private static Set<String> tileTitlesOf(String language, String menu) {
        YamlMessageCatalog catalog = MessageCatalogLoader.readShipped(language);
        return catalog.paths().stream()
                .filter(path -> isTileTitleOf(path, menu))
                .map(path -> catalog.find(path).orElseThrow().get(0))
                .collect(Collectors.toSet());
    }

    private static boolean isTileTitleOf(String path, String menu) {
        return path.startsWith(menu + ".tile.") && path.endsWith(".title");
    }
}
