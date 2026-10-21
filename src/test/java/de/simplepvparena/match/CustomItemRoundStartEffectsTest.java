package de.simplepvparena.match;

import de.simplepvparena.support.InventoryTestPlayer;
import de.simplepvparena.support.LabelledItemStack;
import de.simplepvparena.support.RecordingFightStartTotems;
import de.simplepvparena.support.TickingScheduler;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static de.simplepvparena.support.InventoryTestPlayer.totemEffect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemRoundStartEffectsTest {

    private static final LabelledItemStack CUSTOM_TOTEM = LabelledItemStack.of("custom totem");
    private static final LabelledItemStack TOTEM_IN_HAND = CUSTOM_TOTEM.asFightStartTotem();
    private static final LabelledItemStack SHIELD = LabelledItemStack.of("shield");

    private final TickingScheduler scheduler = new TickingScheduler();
    private final RecordingFightStartTotems fightStartTotems = new RecordingFightStartTotems();
    private final AtomicReference<Optional<ItemStack>> totemItem = new AtomicReference<>(Optional.of(CUSTOM_TOTEM));
    private final List<String> warnings = new ArrayList<>();
    private final CustomItemRoundStartEffects effects = new CustomItemRoundStartEffects(scheduler.plugin(),
            new VanillaRoundStartEffects(), totemItem::get, fightStartTotems, warnings::add);
    private final InventoryTestPlayer fighter = new InventoryTestPlayer("Fighter");
    private final InventoryTestPlayer opponent = new InventoryTestPlayer("Opponent");

    @Test
    void withoutATotemItemThePlainVanillaTotemPlaysAtOnceAndTheOffHandIsNotTouched() {
        totemItem.set(Optional.empty());
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);

        effects.playFightStartAnimation(fighter.player());

        assertEquals(List.of(totemEffect("shield")), fighter.playedEffects());
        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(0, scheduler.pendingTaskCount(), "nothing waits for a later tick");
        assertEquals(0, fightStartTotems.createdTotemCount());
    }

    @Test
    void theTotemGoesIntoTheOffHandTheAnimationFollowsOneTickLaterAndTheOldItemComesBackOneTickAfterThat() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);

        effects.playFightStartAnimation(fighter.player());

        assertEquals(TOTEM_IN_HAND, fighter.offHandItem());
        assertEquals(List.of(), fighter.playedEffects(), "the client gets the item first");

        scheduler.advanceTicks(1);
        assertEquals(List.of(totemEffect("totem custom totem")), fighter.playedEffects());
        assertEquals(TOTEM_IN_HAND, fighter.offHandItem());

        scheduler.advanceTicks(1);
        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(List.of(SHIELD), fighter.nonEmptyItems(), "no totem is left anywhere");
        assertEquals(List.of(totemEffect("totem custom totem")), fighter.playedEffects(), "the animation plays once");
        assertEquals(0, scheduler.pendingTaskCount());
    }

    @Test
    void anEmptyOffHandIsEmptyAgainAfterwards() {
        effects.playFightStartAnimation(fighter.player());
        scheduler.advanceTicks(2);

        assertNull(fighter.offHandItem());
        assertEquals(List.of(), fighter.nonEmptyItems());
    }

    @Test
    void theOldItemIsACopyOfWhatWasHeldSoAChangeOfTheTotemCannotTouchIt() {
        LabelledItemStack heldItem = LabelledItemStack.of("shield");
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, heldItem);

        effects.playFightStartAnimation(fighter.player());
        scheduler.advanceTicks(2);

        assertEquals(heldItem, fighter.offHandItem());
        assertNotSame(heldItem, fighter.offHandItem(), "the old item is given back as a copy");
    }

    @Test
    void everyFighterGetsAnOwnTotemAndTheOwnItemBack() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        LabelledItemStack torch = LabelledItemStack.of("torch");
        opponent.putItem(InventoryTestPlayer.OFF_HAND_SLOT, torch);

        effects.playFightStartAnimation(fighter.player());
        effects.playFightStartAnimation(opponent.player());
        scheduler.advanceTicks(2);

        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(torch, opponent.offHandItem());
        assertEquals(2, fightStartTotems.createdTotemCount());
        assertEquals(List.of(totemEffect("totem custom totem")), opponent.playedEffects());
    }

    @Test
    void stoppingOneFighterGivesTheItemBackAtOnceAndCancelsTheAnimation() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        effects.playFightStartAnimation(fighter.player());
        effects.playFightStartAnimation(opponent.player());

        effects.stopFightStartAnimation(fighter.player());

        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(List.of(SHIELD), fighter.nonEmptyItems());
        assertEquals(TOTEM_IN_HAND, opponent.offHandItem(), "the other fighter keeps the animation");

        scheduler.advanceTicks(2);
        assertEquals(List.of(), fighter.playedEffects(), "the stopped animation never plays");
        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(List.of(totemEffect("totem custom totem")), opponent.playedEffects());
        assertNull(opponent.offHandItem());
    }

    @Test
    void stoppingAfterTheAnimationGivesTheItemBackBeforeTheSecondTick() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        effects.playFightStartAnimation(fighter.player());
        scheduler.advanceTicks(1);

        effects.stopFightStartAnimation(fighter.player());

        assertEquals(SHIELD, fighter.offHandItem());
        scheduler.advanceTicks(5);
        assertEquals(List.of(SHIELD), fighter.nonEmptyItems(), "the later tick gives nothing back twice");
    }

    @Test
    void stoppingEveryFighterGivesEveryItemBackAtOnce() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        LabelledItemStack torch = LabelledItemStack.of("torch");
        opponent.putItem(InventoryTestPlayer.OFF_HAND_SLOT, torch);
        effects.playFightStartAnimation(fighter.player());
        effects.playFightStartAnimation(opponent.player());

        effects.stopFightStartAnimations();

        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(torch, opponent.offHandItem());
        assertEquals(0, scheduler.pendingTaskCount());
        scheduler.advanceTicks(2);
        assertEquals(List.of(), fighter.playedEffects());
        assertEquals(List.of(), opponent.playedEffects());
    }

    @Test
    void stoppingWithoutAPendingTotemChangesNothing() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);

        effects.stopFightStartAnimation(fighter.player());
        effects.stopFightStartAnimations();

        assertEquals(List.of(SHIELD), fighter.nonEmptyItems());
    }

    @Test
    void aSecondAnimationForTheSameFighterFirstGivesTheFirstItemBackSoTheTotemIsNeverRemembered() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);

        effects.playFightStartAnimation(fighter.player());
        effects.playFightStartAnimation(fighter.player());
        scheduler.advanceTicks(2);

        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(List.of(SHIELD), fighter.nonEmptyItems());
        assertEquals(List.of(totemEffect("totem custom totem")), fighter.playedEffects());
    }

    @Test
    void aTotemMovedToAnotherSlotIsRemovedAndTheOldItemTakesItsPlace() {
        LabelledItemStack sword = LabelledItemStack.of("sword");
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        fighter.putItem(InventoryTestPlayer.MAIN_HAND_SLOT, sword);
        effects.playFightStartAnimation(fighter.player());

        fighter.putItem(InventoryTestPlayer.MAIN_HAND_SLOT, TOTEM_IN_HAND);
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, sword);
        scheduler.advanceTicks(2);

        assertEquals(SHIELD, fighter.item(InventoryTestPlayer.MAIN_HAND_SLOT));
        assertEquals(sword, fighter.offHandItem());
        assertEquals(2, fighter.nonEmptyItems().size(), fighter.nonEmptyItems().toString());
    }

    @Test
    void everyCopyOfTheTotemIsRemovedFromTheInventory() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        effects.playFightStartAnimation(fighter.player());
        fighter.putItem(5, TOTEM_IN_HAND);
        fighter.putItem(39, TOTEM_IN_HAND);

        scheduler.advanceTicks(2);

        assertEquals(List.of(SHIELD), fighter.nonEmptyItems());
    }

    @Test
    void aTotemThatIsGoneLeavesTheOldItemInTheFreeOffHand() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        effects.playFightStartAnimation(fighter.player());

        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, null);
        scheduler.advanceTicks(2);

        assertEquals(SHIELD, fighter.offHandItem());
    }

    @Test
    void aTotemThatIsGoneWithAnOccupiedOffHandPutsTheOldItemIntoTheInventory() {
        LabelledItemStack torch = LabelledItemStack.of("torch");
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        effects.playFightStartAnimation(fighter.player());

        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, torch);
        scheduler.advanceTicks(2);

        assertEquals(torch, fighter.offHandItem());
        assertEquals(SHIELD, fighter.item(0));
        assertEquals(List.of(), fighter.droppedItems());
    }

    @Test
    void aFullInventoryDropsTheOldItemAtTheFighterInsteadOfLosingIt() {
        LabelledItemStack torch = LabelledItemStack.of("torch");
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        effects.playFightStartAnimation(fighter.player());
        for (int slot = 0; slot < InventoryTestPlayer.STORAGE_SLOT_COUNT; slot++) {
            fighter.putItem(slot, LabelledItemStack.of("stone " + slot));
        }

        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, torch);
        scheduler.advanceTicks(2);

        assertEquals(List.of(SHIELD), fighter.droppedItems());
    }

    @Test
    void anOfflineFighterSeesNoAnimationButGetsTheItemBack() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        effects.playFightStartAnimation(fighter.player());
        fighter.setOnline(false);

        scheduler.advanceTicks(2);

        assertEquals(List.of(), fighter.playedEffects());
        assertEquals(SHIELD, fighter.offHandItem());
    }

    @Test
    void aTotemThatCannotBePreparedPlaysTheVanillaTotemAndIsWarnedAboutOnce() {
        fightStartTotems.failCreationWith(new IllegalStateException("no data components"));
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);

        effects.playFightStartAnimation(fighter.player());
        effects.playFightStartAnimation(opponent.player());

        assertEquals(List.of(totemEffect("shield")), fighter.playedEffects());
        assertEquals(SHIELD, fighter.offHandItem());
        assertEquals(List.of("The item of items-adder.totem-item in config.yml could not be made into a totem "
                + "(no data components), the vanilla totem is played instead"), warnings);
    }

    @Test
    void anOffHandThatCannotBeChangedKeepsItsItemAndPlaysTheVanillaTotem() {
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, SHIELD);
        fighter.failOffHandChangeWith(new IllegalStateException("inventory locked"));

        effects.playFightStartAnimation(fighter.player());

        assertEquals(List.of(SHIELD), fighter.nonEmptyItems(), "nothing is doubled or lost");
        assertEquals(List.of(totemEffect("shield")), fighter.playedEffects());
        assertEquals(0, scheduler.pendingTaskCount());
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    @UseLanguage("de")
    void warnsAboutATotemThatCannotBePreparedInGerman() {
        fightStartTotems.failCreationWith(new IllegalStateException("no data components"));

        effects.playFightStartAnimation(fighter.player());

        assertEquals(List.of("Das Item aus items-adder.totem-item in config.yml konnte nicht zum Totem gemacht "
                + "werden (no data components), stattdessen erscheint das normale Totem"), warnings);
    }

    @Test
    void aReloadThatEmptiesTheTotemItemBringsTheVanillaTotemBackFromTheNextStartOn() {
        effects.playFightStartAnimation(fighter.player());
        scheduler.advanceTicks(2);
        totemItem.set(Optional.empty());

        effects.playFightStartAnimation(opponent.player());

        assertEquals(List.of(totemEffect("null")), opponent.playedEffects());
        assertEquals(1, fightStartTotems.createdTotemCount());
    }

    @Test
    void removesEveryTotemOfAPlayerAndKnowsATotem() {
        fighter.putItem(3, TOTEM_IN_HAND);
        fighter.putItem(InventoryTestPlayer.OFF_HAND_SLOT, TOTEM_IN_HAND);
        fighter.putItem(7, SHIELD);

        effects.removeTotems(fighter.player());

        assertEquals(List.of(SHIELD), fighter.nonEmptyItems());
        assertTrue(effects.isTotem(TOTEM_IN_HAND));
        assertFalse(effects.isTotem(CUSTOM_TOTEM));
        assertFalse(effects.isTotem(null));
    }

    @Test
    void blindsAndUnblindsLikeTheVanillaEffects() {
        RecordingVanillaEffects vanilla = new RecordingVanillaEffects();
        CustomItemRoundStartEffects delegating = new CustomItemRoundStartEffects(scheduler.plugin(), vanilla,
                Optional::empty, fightStartTotems, warnings::add);

        delegating.blind(fighter.player(), 80);
        delegating.removeBlindness(fighter.player());
        delegating.playFightStartAnimation(fighter.player());

        assertEquals(List.of("blind Fighter for 80", "unblind Fighter", "totem Fighter"), vanilla.calls);
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

        @Override
        public void stopFightStartAnimation(Player player) {
            calls.add("stop " + player.getName());
        }

        @Override
        public void stopFightStartAnimations() {
            calls.add("stop every");
        }
    }
}
