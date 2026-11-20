package de.simplepvparena.voice;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.GroupEvent;
import de.maxhenkel.voicechat.api.events.JoinGroupEvent;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import org.bukkit.Server;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@NullMarked
public final class SimpleVoiceChatBridge implements VoicechatPlugin, VoiceGroups {

    public static final String PLUGIN_NAME = "voicechat";
    public static final String PLUGIN_ID = "simplepvparena";

    private final VoiceIsolation isolation;
    private final Consumer<Runnable> mainThread;
    private volatile @Nullable VoicechatServerApi serverApi;

    private SimpleVoiceChatBridge(VoiceIsolation isolation, Consumer<Runnable> mainThread) {
        this.isolation = Objects.requireNonNull(isolation, "isolation");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public static boolean register(Server server, VoiceIsolation isolation, Consumer<Runnable> mainThread) {
        BukkitVoicechatService service = server.getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            return false;
        }
        SimpleVoiceChatBridge bridge = new SimpleVoiceChatBridge(isolation, mainThread);
        service.registerPlugin(bridge);
        isolation.attach(bridge);
        return true;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi voicechatServerApi) {
            serverApi = voicechatServerApi;
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> serverApi = event.getVoicechat());
        registration.registerEvent(EntitySoundPacketEvent.class, this::filterSound);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::filterSound);
        registration.registerEvent(StaticSoundPacketEvent.class, this::filterSound);
        registration.registerEvent(JoinGroupEvent.class, this::filterGroup);
        registration.registerEvent(CreateGroupEvent.class, this::filterGroup);
    }

    private void filterSound(SoundPacketEvent<?> event) {
        VoicechatConnection sender = event.getSenderConnection();
        VoicechatConnection receiver = event.getReceiverConnection();
        if (sender == null || receiver == null) {
            return;
        }
        if (!isolation.canHear(sender.getPlayer().getUuid(), receiver.getPlayer().getUuid())) {
            event.cancel();
        }
    }

    private void filterGroup(GroupEvent event) {
        VoicechatConnection connection = event.getConnection();
        if (connection == null) {
            return;
        }
        UUID player = connection.getPlayer().getUuid();
        if (!isolation.mayJoinGroup(player)) {
            event.cancel();
            mainThread.accept(() -> isolation.refuseGroup(player));
        }
    }

    @Override
    public boolean leaveGroup(UUID player) {
        VoicechatServerApi api = serverApi;
        if (api == null) {
            return false;
        }
        VoicechatConnection connection = api.getConnectionOf(player);
        if (connection == null || !connection.isInGroup()) {
            return false;
        }
        connection.setGroup(null);
        return true;
    }
}
