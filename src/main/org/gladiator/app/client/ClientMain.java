package app.client;

import app.server.Server;
import app.util.SingletonTerminal;
import org.jline.reader.UserInterruptException;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientMain {
    public static void main(final String... args) {
        AtomicBoolean isRunning = new AtomicBoolean(true);
        while (isRunning.get()) {
            try (Client client = Client.createClient(isRunning)) {
                client.run();
            } catch (UserInterruptException e) {
                isRunning.set(false);
                Server.getLogger().debug("User didn't finished typing during a line read, probably by pressing CTRL+C", e);
            }
        }
        SingletonTerminal.TERMINAL.close();
    }
}
