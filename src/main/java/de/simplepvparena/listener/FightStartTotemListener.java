package de.simplepvparena.listener;

import de.simplepvparena.match.CustomItemRoundStartEffects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

public final class FightStartTotemListener implements Listener {

    private final CustomItemRoundStartEffects fightStartEffects;

    public FightStartTotemListener(CustomItemRoundStartEffects fightStartEffects) {
        this.fightStartEffects = Objects.requireNonNull(fightStartEffects, "fightStartEffects");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        fightStartEffects.removeTotems(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (fightStartEffects.isTotem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityResurrect(EntityResurrectEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand == null || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (fightStartEffects.isTotem(player.getInventory().getItem(hand))) {
            event.setCancelled(true);
        }
    }
}
