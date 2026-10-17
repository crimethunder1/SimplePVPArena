package de.simplepvparena.gui;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

final class RecordingAdminMenuWindows implements AdminMenuWindows {

    private final Map<UUID, RecordedMenu> lastOpenedMenus = new LinkedHashMap<>();
    private final Map<AdminMenuHolder, RecordedMenu> menusByHolder = new IdentityHashMap<>();
    private final Set<UUID> playersWithClosedMenu = new HashSet<>();

    @Override
    public void open(Player player, AdminMenuHolder holder, int size, String title,
                     Consumer<AdminMenuCanvas> filler) {
        RecordedMenu menu = new RecordedMenu(holder, title, size, filler);
        menusByHolder.put(holder, menu);
        filler.accept(menu);
        lastOpenedMenus.put(player.getUniqueId(), menu);
        playersWithClosedMenu.remove(player.getUniqueId());
    }

    @Override
    public void refresh(Player player, AdminMenuHolder holder, Consumer<AdminMenuCanvas> filler) {
        RecordedMenu menu = menusByHolder.get(holder);
        if (menu == null) {
            throw new IllegalStateException("the " + holder.getMenuType() + " menu was never opened");
        }
        filler.accept(menu);
        menu.refreshCount++;
    }

    @Override
    public void close(Player player) {
        playersWithClosedMenu.add(player.getUniqueId());
    }

    @Override
    public boolean isShowing(Player player, AdminMenuType menuType) {
        return !isMenuClosed(player) && findLastOpenedMenu(player)
                .map(menu -> menu.holder().getMenuType() == menuType)
                .orElse(false);
    }

    Optional<RecordedMenu> findLastOpenedMenu(Player player) {
        return Optional.ofNullable(lastOpenedMenus.get(player.getUniqueId()));
    }

    RecordedMenu lastOpenedMenu(Player player) {
        return findLastOpenedMenu(player).orElseThrow(
                () -> new AssertionError("no menu was opened for " + player.getName()));
    }

    void refillLastOpenedMenu(Player player) {
        RecordedMenu menu = lastOpenedMenu(player);
        menu.filler.accept(menu);
    }

    boolean isMenuClosed(Player player) {
        return playersWithClosedMenu.contains(player.getUniqueId());
    }

    static final class RecordedMenu implements AdminMenuCanvas {

        private final AdminMenuHolder holder;
        private final String title;
        private final int size;
        private final Consumer<AdminMenuCanvas> filler;
        private final Map<Integer, MenuTile> tilesBySlot = new TreeMap<>();

        private int refreshCount;

        private RecordedMenu(AdminMenuHolder holder, String title, int size, Consumer<AdminMenuCanvas> filler) {
            this.holder = holder;
            this.title = title;
            this.size = size;
            this.filler = filler;
        }

        @Override
        public void set(int slot, MenuTile tile) {
            tilesBySlot.put(slot, tile);
        }

        @Override
        public void clear() {
            tilesBySlot.clear();
        }

        AdminMenuHolder holder() {
            return holder;
        }

        String title() {
            return title;
        }

        int size() {
            return size;
        }

        int refreshCount() {
            return refreshCount;
        }

        Optional<MenuTile> findTile(int slot) {
            return Optional.ofNullable(tilesBySlot.get(slot));
        }

        Map<Integer, String> describeTiles() {
            Map<Integer, String> descriptions = new TreeMap<>();
            tilesBySlot.forEach((slot, tile) -> descriptions.put(slot, tile.material() + " " + tile.displayName()
                    + " " + tile.loreLines()));
            return descriptions;
        }
    }
}
