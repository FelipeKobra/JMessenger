package app.util.connection;

import app.server.Server;
import org.apache.commons.lang3.Validate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public record Connection(String name, Socket socket, BufferedReader input,
                         PrintWriter output) implements AutoCloseable {

    public Connection {
        Validate.notBlank(name);
        Objects.requireNonNull(socket);
        Objects.requireNonNull(input);
        Objects.requireNonNull(output);
    }

    public void close() {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {
            Server.getLogger().error("Error closing the connection: {}", e, e);
        }

    }

}
