package app.server;

import org.jline.reader.UserInterruptException;

import java.io.IOException;

public final class ServerMain {
    public static void main(final String... args) {
        try (Server server = Server.createServer()) {
            server.runServer();
        } catch (IOException e) {
            Server.getLogger().error("Error closing Server Socket", e);
        } catch (UserInterruptException e) {
            Server.getLogger().debug("User didn't finished typing during a line read, probably by pressing CTRL+C", e);
        }
    }
}