package de.simplepvparena.registration;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TeamChoicePresenter {

    TeamChoicePresenter NONE = new TeamChoicePresenter() {

        @Override
        public void show(Player player) {
        }

        @Override
        public void refresh() {
        }
    };

    void show(Player player);

    void refresh();
}
