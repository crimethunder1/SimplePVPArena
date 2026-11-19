package de.simplepvparena.config;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class ServerModeSetting {

    private ServerMode serverMode = ServerMode.DEFAULT_MODE;

    public ServerMode getServerMode() {
        return serverMode;
    }

    public void setServerMode(ServerMode serverMode) {
        this.serverMode = Objects.requireNonNull(serverMode, "serverMode");
    }
}
