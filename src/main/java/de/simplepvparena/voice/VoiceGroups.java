package de.simplepvparena.voice;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface VoiceGroups {

    boolean leaveGroup(UUID player);
}
