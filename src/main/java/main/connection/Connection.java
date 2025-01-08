package main.connection;

import main.server.DesignServer;
import org.apache.commons.lang3.Validate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public record Connection(String name, Socket socket, BufferedReader input,
                         PrintWriter output) implements AutoCloseable {
    private static int anonymousClients = 0;

    public Connection {
        try {
            Validate.notBlank(name);
            Objects.requireNonNull(socket);
            Objects.requireNonNull(input);
            Objects.requireNonNull(output);
        } catch (IllegalArgumentException e) {
            DesignServer.getLOGGER().fine("Client name not received");
            name = "Anonymous(" + anonymousClients + ")";
            anonymousClients++;
        }
    }

    public void close() {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {
            DesignServer.getLOGGER().warning("Error closing the connection: " + e);
        }

    }
}
