package de.simplepvparena.listener;

import de.simplepvparena.match.CustomItemRoundStartEffects;
import de.simplepvparena.match.VanillaRoundStartEffects;
import de.simplepvparena.support.InventoryTestPlayer;
import de.simplepvparena.support.LabelledItemStack;
import de.simplepvparena.support.RecordingFightStartTotems;
import de.simplepvparena.support.TickingScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightStartTotemListenerTest {

    private static final LabelledItemStack SHIELD = LabelledItemStack.of("shield");
    private static final LabelledItemStack TOTEM = LabelledItemStack.of("custom totem").asFightStartTotem();
    private static final LabelledItemStack VANILLA_TOTEM = LabelledItemStack.of("totem of undying");

    private final CustomItemRoundStartEffects effects = new CustomItemRoundStartEffects(
            new TickingScheduler().plugin(), new VanillaRoundStartEffects(), Optional::empty,
            new RecordingFightStartTotems(), warning -> { });
    private final FightStartTotemListener listener = new FightStartTotemListener(effects);
    private final InventoryTestPlayer player = new InventoryTestPlayer("Fighter");

    @Test
    void aJoiningPlayerLosesEveryTotemLeftOverFromACrashAndKeepsTheRest() {
        player.putItem(InventoryTestPlayer.OFF_HAND_SLOT, TOTEM);
        player.putItem(12, TOTEM);
        player.putItem(3, SHIELD);

        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));

        assertEquals(List.of(SHIELD), player.nonEmptyItems());
    }

    @Test
    void aTotemCannotBeDropped() {
        PlayerDropItemEvent event = new PlayerDropItemEvent(player.player(), player.droppedEntityOf(TOTEM));

        listener.onPlayerDropItem(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void everyOtherItemIsDroppedAsBefore() {
        PlayerDropItemEvent event = new PlayerDropItemEvent(player.player(), player.droppedEntityOf(SHIELD));

        listener.onPlayerDropItem(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void theTotemOfTheAnimationNeverSavesAFighterFromDeath() {
        player.putItem(InventoryTestPlayer.OFF_HAND_SLOT, TOTEM);
        EntityResurrectEvent event = new EntityResurrectEvent(player.player(), EquipmentSlot.OFF_HAND);

        listener.onEntityResurrect(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void aRealTotemInTheHandStillSavesAsBefore() {
        player.putItem(InventoryTestPlayer.MAIN_HAND_SLOT, VANILLA_TOTEM);
        player.putItem(InventoryTestPlayer.OFF_HAND_SLOT, TOTEM);
        EntityResurrectEvent event = new EntityResurrectEvent(player.player(), EquipmentSlot.HAND);

        listener.onEntityResurrect(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void aDeathWithoutAnyTotemStaysUntouched() {
        EntityResurrectEvent event = new EntityResurrectEvent(player.player(), null);
        event.setCancelled(true);

        listener.onEntityResurrect(event);

        assertTrue(event.isCancelled());
    }
}
