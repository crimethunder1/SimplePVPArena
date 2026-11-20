package de.simplepvparena.message;

import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public enum VoiceMessage implements MessageKey {

    SIMPLE_VOICE_CHAT_USED("voice.simple-voice-chat-used", "path", "permission"),
    SIMPLE_VOICE_CHAT_SWITCHED_OFF("voice.simple-voice-chat-switched-off", "path"),
    SIMPLE_VOICE_CHAT_SERVICE_MISSING("voice.simple-voice-chat-service-missing"),
    SIMPLE_VOICE_CHAT_NOT_CONNECTED("voice.simple-voice-chat-not-connected"),
    ISOLATED("voice.isolated"),
    LEFT_GROUP("voice.left-group"),
    ELIMINATED("voice.eliminated"),
    BACK_IN_THE_FIGHT("voice.back-in-the-fight"),
    GROUP_REFUSED("voice.group-refused");

    private final String path;
    private final Set<String> placeholderNames;

    VoiceMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}
