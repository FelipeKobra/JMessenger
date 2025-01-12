package app.server.config;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

import static environment.Port.PORT_MAX;
import static environment.Port.PORT_MIN;

// God class
public record ServerConfig(String name, int port) {

    public ServerConfig {
        validateArgs(name, port);
    }

    public ServerConfig() {
        this(getDefaultName(), getDefaultPort());
    }

    public static String getDefaultName() {
        return "Server";
    }

    public static int getDefaultPort() {
        return 50_000;
    }

    private void validateArgs(String name, int port) {
        Objects.requireNonNull(name);
        Validate.notBlank(name);
        Validate.inclusiveBetween(PORT_MIN, PORT_MAX, port);
    }

}
