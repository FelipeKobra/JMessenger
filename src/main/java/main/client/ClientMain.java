package main.client;

import main.chat.Chat;
import main.chat.ConsoleChat;
import main.chat.CustomTerminal;
import main.chat.DefaultTerminal;

import java.io.IOException;
import java.util.logging.Logger;

public final class ClientMain {
    public static void main(final String... args) {
        try (final CustomTerminal customTerminal = new DefaultTerminal()) {
            final Chat chat = new ConsoleChat(">");
            final var config = DefaultClientConfig.getClientConfig(customTerminal, chat);
            DesignClient.runClient(config);
        } catch (IOException e) {
            Logger.getLogger("org.jline").fine("Error closing the custom terminal");
        }
    }
}
