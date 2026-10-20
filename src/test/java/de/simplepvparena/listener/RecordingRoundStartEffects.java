package de.simplepvparena.listener;

import de.simplepvparena.match.RoundStartEffects;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RecordingRoundStartEffects implements RoundStartEffects {

    private final Map<String, Integer> blindnessTicksByPlayerName = new LinkedHashMap<>();
    private final List<String> events = new ArrayList<>();

    @Override
    public void blind(Player player, int durationTicks) {
        blindnessTicksByPlayerName.put(player.getName(), durationTicks);
        events.add("blind " + player.getName() + " for " + durationTicks + " ticks");
    }

    @Override
    public void removeBlindness(Player player) {
        blindnessTicksByPlayerName.remove(player.getName());
        events.add("remove the blindness of " + player.getName());
    }

    @Override
    public void playFightStartAnimation(Player player) {
        events.add("totem for " + player.getName());
    }

    boolean isBlind(ArenaMatchHarness.FakePlayer player) {
        return blindnessTicksByPlayerName.containsKey(player.name());
    }

    List<String> blindPlayerNames() {
        return List.copyOf(blindnessTicksByPlayerName.keySet());
    }

    List<String> events() {
        return events;
    }

    void forgetEvents() {
        events.clear();
    }
}
