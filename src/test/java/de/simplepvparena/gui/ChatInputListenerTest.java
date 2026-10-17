package de.simplepvparena.gui;

import de.simplepvparena.listener.ChatInputListener;
import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatInputListenerTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int KITS_CREATE_SLOT = 51;

    private static final String NOT_ALLOWED = "You are not allowed to use the tournament menu";

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer otherPlayer = fixture.letAnotherPlayerJoin("other", false);
    private final ConsoleCommandSender console = SharedBukkitServer.proxy(ConsoleCommandSender.class,
            (method, arguments) -> {
                throw new UnsupportedOperationException("ConsoleCommandSender." + method.getName());
            });

    @Test
    void cancelsTheTypedKitNameAndShowsItToNobodyNotEvenTheConsole() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        AsyncChatEvent event = fixture.sendChatEvent(fixture.admin(), "test1kit", everyoneOnline());

        assertHiddenFromEveryone(event);
        fixture.runNextTick();
        assertEquals(List.of("test1kit"), fixture.kitLibrary().getKitNames());
    }

    @Test
    void cancelsTheLineCancelAndShowsItToNobody() {
        askForTheNameOfTheNewKit();

        AsyncChatEvent event = fixture.sendChatEvent(fixture.admin(), "cancel", everyoneOnline());

        assertHiddenFromEveryone(event);
        fixture.runNextTick();
        assertEquals(List.of("The input for the name of the new kit was cancelled, nothing was changed"),
                fixture.messages());
    }

    @Test
    void cancelsARejectedKitNameAndShowsItToNobody() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        AsyncChatEvent event = fixture.sendChatEvent(fixture.admin(), "my kit", everyoneOnline());

        assertHiddenFromEveryone(event);
        fixture.runNextTick();
        assertEquals(List.of("The kit name my kit is not allowed, a kit name has 1 to 32 letters, digits, "
                + "hyphens or underscores"), fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
    }

    @Test
    void cancelsTheLineOfAnAdminWhosePermissionWasRevokedAndShowsItToNobody() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();
        fixture.revokeAdminPermission();

        AsyncChatEvent event = fixture.sendChatEvent(fixture.admin(), "test1kit", everyoneOnline());

        assertHiddenFromEveryone(event);
        fixture.runNextTick();
        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
    }

    @Test
    void leavesTheLineOfAnAdminWithoutAnOpenRequestUntouched() {
        AsyncChatEvent event = fixture.sendChatEvent(fixture.admin(), "test1kit", everyoneOnline());

        assertUntouched(event);
        assertEquals(0, fixture.scheduledTaskCount());
    }

    @Test
    void leavesTheLineOfAnotherPlayerUntouchedWhileTheAdminOwesAnAnswer() {
        askForTheNameOfTheNewKit();

        AsyncChatEvent event = fixture.sendChatEvent(otherPlayer.player(), "test1kit", everyoneOnline());

        assertUntouched(event);
        assertEquals(0, fixture.scheduledTaskCount());
        assertEquals(1, fixture.pendingChatInputCount());
    }

    @Test
    void handlesOneTypedLineExactlyOnce() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        fixture.sendChatEvent(fixture.admin(), "test1kit", everyoneOnline());

        assertEquals(1, fixture.scheduledTaskCount());
        assertEquals(0, fixture.pendingChatInputCount());
        fixture.runNextTick();
        assertEquals(List.of("test1kit"), fixture.kitLibrary().getKitNames());
        assertEquals(1, fixture.kitStorage().savedLibraries().size());
        assertEquals(0, fixture.scheduledTaskCount());
    }

    @Test
    void listensToTheModernChatEventAloneOnTheLowestPriority() {
        List<Method> eventHandlers = Arrays.stream(ChatInputListener.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventHandler.class))
                .sorted(Comparator.comparing(Method::getName))
                .toList();

        assertEquals(List.of(AsyncChatEvent.class, PlayerQuitEvent.class), eventHandlers.stream()
                .map(method -> method.getParameterTypes()[0])
                .toList());
        assertEquals(EventPriority.LOWEST, eventHandlers.get(0).getAnnotation(EventHandler.class).priority());
    }

    private void askForTheNameOfTheNewKit() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_CREATE_SLOT);
        fixture.messages().clear();
    }

    private Set<Audience> everyoneOnline() {
        return new HashSet<>(Set.of(fixture.admin(), otherPlayer.player(), console));
    }

    private void assertHiddenFromEveryone(AsyncChatEvent event) {
        assertTrue(event.isCancelled());
        assertEquals(Set.of(), event.viewers());
    }

    private void assertUntouched(AsyncChatEvent event) {
        assertFalse(event.isCancelled());
        assertEquals(Set.of(fixture.admin(), otherPlayer.player(), console), event.viewers());
    }
}
