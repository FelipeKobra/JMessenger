package main.client;

import main.chat.Chat;
import main.chat.CustomTerminal;
import main.server.DesignServerConfig;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import java.util.logging.Logger;

public class DefaultClientConfig {
    private final String name;
    private final String hostname;
    private final int port;
    private final Chat chat;
    private final CustomTerminal customTerminal;

    private DefaultClientConfig(String name, String hostname, int port, Chat chat, CustomTerminal customTerminal) {
        this.name = name;
        this.hostname = hostname;
        this.port = port;
        this.chat = chat;
        this.customTerminal = customTerminal;
    }

    public static DefaultClientConfig getClientConfig(CustomTerminal customTerminal, Chat chat) {
        final String clientName = receiveName(customTerminal);
        final String serverAddress = receiveAddress(customTerminal);

        return new DefaultClientConfig(clientName, serverAddress, DesignServerConfig.PORT_DEFAULT, chat, customTerminal);
    }

    private static String receiveName(CustomTerminal terminal) {
        String name = "";
        try {
            while (name.isBlank()) {
                name = terminal.getLineReader().readLine("Choose your name: ");
            }
        } catch (EndOfFileException | UserInterruptException e) {
            Logger.getLogger("org.jline").fine("Line reading interrupted");
        }
        return name;
    }

    private static String receiveAddress(CustomTerminal terminal) {
        String ip = "";
        try {
            ip = terminal.getLineReader().readLine("Type the Server IP address: ");
        } catch (EndOfFileException | UserInterruptException e) {
            Logger.getLogger("org.jline").fine("Line reading interrupted");
        }
        return ip;
    }

    public CustomTerminal getCustomTerminal() {
        return customTerminal;
    }

    public Chat getChat() {
        return chat;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}
