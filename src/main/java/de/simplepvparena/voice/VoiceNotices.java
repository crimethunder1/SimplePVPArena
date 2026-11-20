package de.simplepvparena.voice;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface VoiceNotices {

    void isolated(UUID fighter);

    void leftGroup(UUID fighter);

    void eliminated(UUID fighter);

    void groupRefused(UUID fighter);
}
