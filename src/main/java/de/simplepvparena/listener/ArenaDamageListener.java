package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaDamageCauser;
import de.simplepvparena.arena.ArenaDamageRule;
import de.simplepvparena.arena.ArenaDamageVictimRole;
import de.simplepvparena.arena.ArenaFightRegion;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundParticipants;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ArenaDamageListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final ArenaRegistry arenaRegistry;
    private final Server server;

    public ArenaDamageListener(MatchService matchService, SpectatorService spectatorService,
                               ArenaRegistry arenaRegistry, Server server) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.spectatorService = Objects.requireNonNull(spectatorService, "spectatorService");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.server = Objects.requireNonNull(server, "server");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (matchService.isApplyingSuddenDeathDamage()) {
            return;
        }
        ArenaDamageVictimRole victimRole = findVictimRole(victim);
        if (victimRole == ArenaDamageVictimRole.NONE) {
            return;
        }
        if (ArenaDamageRule.isDamageCancelled(victimRole, event.getCause(),
                findCauser(victim.getUniqueId(), event))) {
            event.setCancelled(true);
        }
    }

    private ArenaDamageVictimRole findVictimRole(Player victim) {
        UUID victimUniqueId = victim.getUniqueId();
        if (matchService.isActiveFighter(victimUniqueId)) {
            return ArenaDamageVictimRole.FIGHTER;
        }
        if (spectatorService.findSpectatorMode(victimUniqueId).isPresent()
                || ArenaFightRegion.contains(arenaRegistry.getArena().getShape(), victim.getLocation())) {
            return ArenaDamageVictimRole.SPECTATOR;
        }
        return ArenaDamageVictimRole.NONE;
    }

    private ArenaDamageCauser findCauser(UUID victimUniqueId, EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent damageByEntityEvent)) {
            return ArenaDamageCauser.NONE;
        }
        Optional<UUID> attackerUniqueId = PlayerDamageSource.resolveCausingPlayer(damageByEntityEvent.getDamager());
        if (attackerUniqueId.isEmpty()) {
            return ArenaDamageCauser.NON_PLAYER;
        }
        return classifyAttacker(victimUniqueId, attackerUniqueId.get());
    }

    private ArenaDamageCauser classifyAttacker(UUID victimUniqueId, UUID attackerUniqueId) {
        if (attackerUniqueId.equals(victimUniqueId)) {
            return ArenaDamageCauser.SELF;
        }
        Optional<RoundParticipants> roundParticipants = matchService.findMatchOfPlayer(victimUniqueId)
                .flatMap(Match::getRoundParticipants);
        Optional<String> victimTeamName = roundParticipants
                .flatMap(participants -> participants.findTeamNameOfPlayer(victimUniqueId));
        Optional<String> attackerTeamName = roundParticipants
                .flatMap(participants -> participants.findTeamNameOfPlayer(attackerUniqueId));
        boolean bothActive = victimTeamName.isPresent() && attackerTeamName.isPresent();
        if (bothActive && !victimTeamName.equals(attackerTeamName)) {
            return ArenaDamageCauser.OPPONENT;
        }
        if (isAdmin(attackerUniqueId)) {
            return ArenaDamageCauser.ADMIN;
        }
        if (bothActive) {
            return ArenaDamageCauser.TEAM_MATE;
        }
        return ArenaDamageCauser.OTHER_PLAYER;
    }

    private boolean isAdmin(UUID playerUniqueId) {
        Player player = server.getPlayer(playerUniqueId);
        return player != null && AdminPermission.isAdmin(player);
    }
}
