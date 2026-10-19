package de.simplepvparena.message;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertedClassesHaveNoTextLiteralsTest {

    private static final Path SOURCE_FOLDER = Path.of("src", "main", "java", "de", "simplepvparena");

    private static final List<String> CONVERTED_CLASSES = List.of(
            "message/TournamentMessages.java",
            "gui/MenuItems.java",
            "gui/MenuTile.java",
            "gui/MenuPagination.java",
            "gui/AdminMenuReplies.java",
            "gui/MainAdminMenu.java",
            "gui/TournamentMenuStatus.java",
            "gui/MatchAdminMenu.java",
            "gui/ForceResultAdminMenu.java",
            "gui/WaitingMatchesAdminMenu.java",
            "gui/WaitingMatchResultAdminMenu.java",
            "gui/ConfirmationAdminMenu.java",
            "gui/ConfirmationRequest.java",
            "gui/AdminMenuService.java",
            "gui/AdminMenuLayout.java",
            "input/BlockItemRequest.java",
            "input/BlockItemRequestResult.java",
            "input/ChatInputRegistry.java",
            "input/ChatInputService.java",
            "input/ChatInputValues.java",
            "input/ExpiredChatInput.java",
            "input/PendingChatInput.java",
            "team/Team.java",
            "team/TeamCreationRequest.java",
            "team/TeamMemberLookup.java",
            "team/TeamSizeWriter.java",
            "team/TeamSizeSetting.java",
            "gui/TeamsAdminMenu.java",
            "gui/TeamActionsAdminMenu.java",
            "gui/TeamMembersAdminMenu.java");

    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern USAGE_TOKEN = Pattern.compile("<[^<>]*>");
    private static final Pattern LETTER = Pattern.compile("\\p{L}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final String EXCEPTION_MARK = "Exception(";

    @Test
    void theConvertedClassesHoldNoTextLiteral() throws IOException {
        List<String> textLiterals = new ArrayList<>();

        for (String convertedClass : CONVERTED_CLASSES) {
            List<String> lines = Files.readAllLines(SOURCE_FOLDER.resolve(convertedClass), StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String line = lines.get(lineIndex);
                if (line.contains(EXCEPTION_MARK)) {
                    continue;
                }
                Matcher literal = STRING_LITERAL.matcher(line);
                while (literal.find()) {
                    if (isText(literal.group(1))) {
                        textLiterals.add(convertedClass + ":" + (lineIndex + 1) + " " + literal.group());
                    }
                }
            }
        }

        assertEquals(List.of(), textLiterals, "texts belong into lang/en.yml and lang/de.yml");
    }

    @Test
    void theRuleCatchesSentencesAndLetsKeysFormatsAndSeparatorsPass() {
        assertTrue(isText("Returns to the main menu"));
        assertTrue(isText("Type <material> <amount> in chat"));
        assertFalse(isText("menu.back.main-menu"));
        assertFalse(isText("lang/en.yml"));
        assertFalse(isText("<material> <amount>"));
        assertFalse(isText(", "));
        assertFalse(isText("] "));
        assertFalse(isText("pageCount"));
    }

    private static boolean isText(String literal) {
        String withoutUsageTokens = USAGE_TOKEN.matcher(literal).replaceAll("");
        return LETTER.matcher(withoutUsageTokens).find() && WHITESPACE.matcher(withoutUsageTokens).find();
    }
}
