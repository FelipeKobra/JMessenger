package main.server;

import main.chat.Chat;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

public record DesignServerConfig(String name, int port, Chat chat, ExecutorService executor) {
    public static final int PORT_DEFAULT = 50_000;
    private static final int PORT_MIN = 0;
    private static final int PORT_MAX = 65_535;

    public DesignServerConfig {
        Validate.notBlank(name);
        Validate.inclusiveBetween(PORT_MIN, PORT_MAX, port);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(chat);
    }
}
