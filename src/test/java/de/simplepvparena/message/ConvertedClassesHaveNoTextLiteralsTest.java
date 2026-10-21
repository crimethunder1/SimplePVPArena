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
            "gui/BracketViewMenu.java",
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
            "gui/TeamMembersAdminMenu.java",
            "kit/KitDefinition.java",
            "kit/KitSaveResult.java",
            "kit/KitSaveService.java",
            "tournament/PendingKitChoice.java",
            "gui/KitAdminMenu.java",
            "gui/KitActionsAdminMenu.java",
            "gui/KitChoiceAdminMenu.java",
            "gui/KitChoiceAdminMenuPresenter.java",
            "arena/ArenaSpawnType.java",
            "arena/EffectArea.java",
            "arena/ArenaRegistry.java",
            "arena/ArenaDebugCellLimit.java",
            "arena/ArenaSetupService.java",
            "gui/ArenaAdminMenu.java",
            "arena/Arena.java",
            "arena/ArenaBaselineReport.java",
            "arena/ArenaBaselineSnapshot.java",
            "arena/ArenaCleaningPhase.java",
            "arena/ArenaCleaningTarget.java",
            "arena/ArenaCleaningPlan.java",
            "arena/ArenaCleaningReport.java",
            "arena/ArenaHeightMarkerMode.java",
            "arena/ArenaHeightMarkers.java",
            "arena/ArenaHeightMarkerService.java",
            "arena/ArenaShapeCalculation.java",
            "arena/ArenaGapTarget.java",
            "arena/ArenaGapGuide.java",
            "arena/ArenaGapCompass.java",
            "arena/ArenaFillDiagnostics.java",
            "arena/SpectatorItemFactory.java",
            "arena/ArenaWorldBuildRule.java",
            "border/BorderMarkToggle.java",
            "border/BorderWallOutline.java",
            "listener/PlayerInteractListener.java",
            "listener/PlayerMoveListener.java",
            "listener/BorderSelectorListener.java",
            "listener/ArenaBreakListener.java",
            "listener/ArenaBuildListener.java",
            "listener/PlayerConnectionListener.java",
            "gate/StartGateCountdown.java",
            "gate/StartGateDuration.java",
            "gate/StartGateItemService.java",
            "tournament/TournamentState.java",
            "tournament/FinalMatchStage.java",
            "tournament/FinalMatchGate.java",
            "tournament/TournamentOperationResult.java",
            "tournament/AutoMatchService.java",
            "tournament/AutoMatchSettings.java",
            "tournament/AutoMatchState.java",
            "match/Match.java",
            "match/MatchScore.java",
            "kit/KitService.java",
            "command/TournamentCommand.java",
            "diagnostics/TournamentDryRun.java",
            "diagnostics/DryRunReport.java",
            "diagnostics/DryRunStatus.java",
            "diagnostics/DryRunStep.java",
            "diagnostics/DryRunTeam.java",
            "diagnostics/DryRunArena.java",
            "diagnostics/PluginDryRunArena.java",
            "config/PluginSettings.java",
            "config/ConfigurationReloadReport.java",
            "config/ConfigurationReloadService.java",
            "config/PluginLiveConfiguration.java",
            "config/LiveConfiguration.java",
            "arena/AreaInventoryService.java",
            "arena/AreaInventoryStorage.java",
            "arena/AreaInventoryLookup.java",
            "arena/FileAreaInventoryStorage.java",
            "listener/AreaInventoryListener.java",
            "visual/CustomVisuals.java",
            "visual/VanillaVisuals.java",
            "visual/ItemsAdderVisuals.java",
            "visual/CustomVisualSettings.java",
            "visual/CustomVisualsSelection.java",
            "visual/ConfiguredVisuals.java",
            "match/CountdownImages.java",
            "match/FightStartTotems.java",
            "match/PaperFightStartTotems.java",
            "match/CustomItemRoundStartEffects.java",
            "listener/FightStartTotemListener.java");

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
