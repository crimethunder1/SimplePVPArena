package de.simplepvparena.registration;

import org.bukkit.entity.Player;

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
