package main.server;

import main.chat.Chat;
import main.chat.ConsoleChat;
import main.chat.CustomTerminal;
import main.chat.DefaultTerminal;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class ServerMain {
    public static void main(final String... args) {

        final String serverName = "Server";
        final Chat chat = new ConsoleChat(">");
        final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        final var config = new DesignServerConfig(serverName, DesignServerConfig.PORT_DEFAULT, chat, executor);
        try (final CustomTerminal customTerminal = new DefaultTerminal();
             final ServerSocket socket = ServerSocketFactory.getDefault().createServerSocket(config.port());
             DesignServer server = new DesignServer(customTerminal, config, socket)) {

            server.runServer();
        } catch (IOException e) {
            DesignServer.getLOGGER().warning("Error creating the Server Socket or during the Custom Terminal closing: " + e);
        }
    }
}