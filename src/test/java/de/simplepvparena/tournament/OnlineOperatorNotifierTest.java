package de.simplepvparena.tournament;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OnlineOperatorNotifierTest {

    private static final String READY_MESSAGE = "The final alpha against bravo is ready, "
            + "an admin starts it with \"Start the final\" in /tournament";

    private final List<Player> onlinePlayers = new ArrayList<>();
    private final List<String> consoleMessages = new ArrayList<>();
    private final OnlineOperatorNotifier notifier = new OnlineOperatorNotifier(createServer());

    @Test
    void tellsAnAdminWithThePermissionButWithoutOperatorStatusThatTheFinalIsReady() {
        FakePlayer permittedAdmin = addPlayer(false);
        permittedAdmin.adminPermissionGranted = true;

        notifier.notifyOperators(READY_MESSAGE);

        assertFalse(permittedAdmin.player.isOp(), "the admin is no operator");
        assertEquals(List.of(READY_MESSAGE), permittedAdmin.messages);
    }

    @Test
    void keepsTheReadyFinalFromAPlayerWithoutThePermissionAndWithoutOperatorStatus() {
        FakePlayer ordinaryPlayer = addPlayer(false);

        notifier.notifyOperators(READY_MESSAGE);

        assertEquals(List.of(), ordinaryPlayer.messages);
        assertEquals(List.of(READY_MESSAGE), consoleMessages, "while the console still reads it");
    }

    @Test
    void tellsAnOperatorWithoutAnExplicitPermissionThatTheFinalIsReady() {
        FakePlayer operator = addPlayer(true);

        notifier.notifyOperators(READY_MESSAGE);

        assertEquals(List.of(READY_MESSAGE), operator.messages);
    }

    private FakePlayer addPlayer(boolean operator) {
        FakePlayer fakePlayer = new FakePlayer(operator);
        onlinePlayers.add(fakePlayer.player);
        return fakePlayer;
    }

    private Server createServer() {
        ConsoleCommandSender console = proxy(ConsoleCommandSender.class,
                (methodName, arguments) -> switch (methodName) {
                    case "sendMessage" -> {
                        consoleMessages.add(recordedText(arguments[0]));
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException("ConsoleCommandSender." + methodName);
                });
        return proxy(Server.class, (methodName, arguments) -> switch (methodName) {
            case "getOnlinePlayers" -> onlinePlayers;
            case "getConsoleSender" -> console;
            default -> throw new UnsupportedOperationException("Server." + methodName);
        });
    }

    private static String recordedText(Object message) {
        return message instanceof Component component
                ? TournamentMessages.toPlainBody(component) : String.valueOf(message);
    }

    private static <T> T proxy(Class<T> type, FakeCall call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxyInstance, method, arguments) -> switch (method.getName()) {
                    case "equals" -> proxyInstance == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "toString" -> "fake " + type.getSimpleName();
                    default -> call.invoke(method.getName(), arguments);
                }));
    }

    @FunctionalInterface
    private interface FakeCall {

        Object invoke(String methodName, Object[] arguments);
    }

    private static final class FakePlayer {

        private final boolean operator;
        private final List<String> messages = new ArrayList<>();
        private final Player player;

        private boolean adminPermissionGranted;

        private FakePlayer(boolean operator) {
            this.operator = operator;
            this.player = proxy(Player.class, (methodName, arguments) -> switch (methodName) {
                case "isOp" -> this.operator;
                case "hasPermission" -> TournamentCommand.ADMIN_PERMISSION.equals(arguments[0])
                        && (this.operator || adminPermissionGranted);
                case "sendMessage" -> {
                    messages.add(recordedText(arguments[0]));
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Player." + methodName);
            });
        }
    }
}
