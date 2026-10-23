package de.simplepvparena.gui;

import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TournamentCommandTribuneFailureTest {

    private static final String[] SPECTATE = {"spectate"};
    private static final String WORLD_NAME = "tribune-failure";

    private final RuntimeException teleportFailure = new IllegalStateException("the teleport was refused");
    private final List<String> receivedTexts = new ArrayList<>();
    private final List<LogRecord> logRecords = new ArrayList<>();

    @Test
    @UseLanguage("en")
    void answersLikeAMissingTribuneSpawnAndLogsAWarningWhenTheTeleportFails() {
        AdminMenuFixture fixture = new AdminMenuFixture();
        World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        SharedBukkitServer.register(world, uniqueId -> null);
        fixture.arenaRegistry().getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME, 0.5D, 64.0D, 0.5D,
                0F, 0F));
        TournamentCommand command = fixture.createTournamentCommand(recordingLogger());

        command.onCommand(spectator(), null, "tournament", SPECTATE);

        assertEquals(List.of("The arena has no usable tribune position"), receivedTexts);
        assertEquals(1, logRecords.size());
        assertEquals(Level.WARNING, logRecords.get(0).getLevel());
        assertEquals("Watcher could not be sent to the tribune", logRecords.get(0).getMessage());
        assertSame(teleportFailure, logRecords.get(0).getThrown());
    }

    private Logger recordingLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }

    private Player spectator() {
        UUID uniqueId = UUID.randomUUID();
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> uniqueId;
            case "getName" -> "Watcher";
            case "hasPermission" -> TournamentCommand.SPECTATE_PERMISSION.equals(arguments[0]);
            case "teleport" -> throw teleportFailure;
            case "sendMessage" -> {
                receivedTexts.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }
}
