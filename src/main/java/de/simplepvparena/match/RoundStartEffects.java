package de.simplepvparena.match;

import org.bukkit.entity.Player;

public interface RoundStartEffects {

    void blind(Player player, int durationTicks);

    void removeBlindness(Player player);

    void playFightStartAnimation(Player player);
}
