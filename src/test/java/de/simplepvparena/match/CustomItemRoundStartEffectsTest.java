package de.simplepvparena.match;

import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.visual.ConfiguredVisuals;
import de.simplepvparena.visual.CustomVisualSettings;
import de.simplepvparena.visual.RecordingCustomVisuals;
import de.simplepvparena.visual.VanillaVisuals;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomItemRoundStartEffectsTest {

    private static final String TOTEM_IDENTIFIER = "simplepvparena:totem";
    private static final CustomVisualSettings WITH_TOTEM = new CustomVisualSettings(true, "", "", "", "",
            TOTEM_IDENTIFIER, "");

    private final List<String> playedEffects = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final RecordingCustomVisuals customVisuals = new RecordingCustomVisuals();
    private final ConfiguredVisuals configuredVisuals = new ConfiguredVisuals(customVisuals, warnings::add);
    private final CustomItemRoundStartEffects effects = new CustomItemRoundStartEffects(
            new VanillaRoundStartEffects(), configuredVisuals::playTotemAnimation);
    private final Player fighter = player("Fighter");

    @Test
    void aTotemItemThatItemsAdderPlaysReplacesTheVanillaTotem() {
        customVisuals.withTotem(TOTEM_IDENTIFIER);
        configuredVisuals.applySettings(WITH_TOTEM);

        effects.playFightStartAnimation(fighter);

        assertEquals(List.of("totem simplepvparena:totem for Fighter"), customVisuals.lookups());
        assertEquals(List.of(), playedEffects, "the vanilla totem stays away");
        assertEquals(List.of(), warnings);
    }

    @Test
    void aTotemItemThatItemsAdderDoesNotPlayFallsBackToTheVanillaTotem() {
        configuredVisuals.applySettings(WITH_TOTEM);

        effects.playFightStartAnimation(fighter);

        assertEquals(List.of("totem simplepvparena:totem for Fighter"), customVisuals.lookups());
        assertEquals(List.of(EntityEffect.PROTECTED_FROM_DEATH + " for Fighter"), playedEffects);
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void withoutATotemItemTheVanillaTotemPlaysWithoutAskingItemsAdder() {
        customVisuals.withTotem(TOTEM_IDENTIFIER);

        effects.playFightStartAnimation(fighter);

        assertEquals(List.of(), customVisuals.lookups());
        assertEquals(List.of(EntityEffect.PROTECTED_FROM_DEATH + " for Fighter"), playedEffects);
        assertEquals(List.of(), warnings);
    }

    @Test
    void withoutItemsAdderTheVanillaTotemPlaysEvenWithATotemItem() {
        ConfiguredVisuals vanillaVisuals = new ConfiguredVisuals(new VanillaVisuals(), warnings::add);
        vanillaVisuals.applySettings(WITH_TOTEM);
        CustomItemRoundStartEffects vanillaEffects = new CustomItemRoundStartEffects(new VanillaRoundStartEffects(),
                vanillaVisuals::playTotemAnimation);

        vanillaEffects.playFightStartAnimation(fighter);

        assertEquals(List.of(EntityEffect.PROTECTED_FROM_DEATH + " for Fighter"), playedEffects);
        assertEquals(List.of(), warnings);
    }

    @Test
    void everyFighterGetsAnOwnAnimation() {
        customVisuals.withTotem(TOTEM_IDENTIFIER);
        configuredVisuals.applySettings(WITH_TOTEM);

        effects.playFightStartAnimation(fighter);
        effects.playFightStartAnimation(player("Opponent"));

        assertEquals(List.of("totem simplepvparena:totem for Fighter", "totem simplepvparena:totem for Opponent"),
                customVisuals.lookups());
        assertEquals(List.of(), playedEffects);
    }

    @Test
    void aReloadThatEmptiesTheTotemItemBringsTheVanillaTotemBackFromTheNextStartOn() {
        customVisuals.withTotem(TOTEM_IDENTIFIER);
        configuredVisuals.applySettings(WITH_TOTEM);
        effects.playFightStartAnimation(fighter);

        configuredVisuals.applySettings(CustomVisualSettings.defaults());
        effects.playFightStartAnimation(fighter);

        assertEquals(List.of("totem simplepvparena:totem for Fighter"), customVisuals.lookups());
        assertEquals(List.of(EntityEffect.PROTECTED_FROM_DEATH + " for Fighter"), playedEffects);
    }

    @Test
    void blindsAndUnblindsLikeTheVanillaEffects() {
        RecordingVanillaEffects vanilla = new RecordingVanillaEffects();
        CustomItemRoundStartEffects delegating = new CustomItemRoundStartEffects(vanilla, player -> false);

        delegating.blind(fighter, 80);
        delegating.removeBlindness(fighter);
        delegating.playFightStartAnimation(fighter);

        assertEquals(List.of("blind Fighter for 80", "unblind Fighter", "totem Fighter"), vanilla.calls);
    }

    @Test
    void aPlayedCustomTotemLeavesOnlyTheBlindnessToTheVanillaEffects() {
        RecordingVanillaEffects vanilla = new RecordingVanillaEffects();
        CustomItemRoundStartEffects delegating = new CustomItemRoundStartEffects(vanilla, player -> true);

        delegating.blind(fighter, 80);
        delegating.playFightStartAnimation(fighter);

        assertEquals(List.of("blind Fighter for 80"), vanilla.calls);
    }

    private Player player(String name) {
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> name;
            case "playEffect" -> {
                playedEffects.add(arguments[0] + " for " + name);
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    private static final class RecordingVanillaEffects implements RoundStartEffects {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void blind(Player player, int durationTicks) {
            calls.add("blind " + player.getName() + " for " + durationTicks);
        }

        @Override
        public void removeBlindness(Player player) {
            calls.add("unblind " + player.getName());
        }

        @Override
        public void playFightStartAnimation(Player player) {
            calls.add("totem " + player.getName());
        }
    }
}
