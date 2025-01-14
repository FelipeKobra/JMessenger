package app.client;

import app.util.AsyncSocketIO;
import app.util.Chat;
import app.util.SingletonTerminal;
import app.util.connection.ConnectionMessage;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Client implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class.getName());

    private final ClientConfig config;
    private final Socket socket;
    private final ExecutorService executor;
    private final Chat chat;
    private final AtomicBoolean isRunning;

    private Client(ClientConfig config, Socket socket, ExecutorService executor, Chat chat, AtomicBoolean isRunning) {
        this.config = config;
        this.socket = socket;
        this.executor = executor;
        this.chat = chat;
        this.isRunning = isRunning;
    }

    public static Client createClient(AtomicBoolean isRunning) {
        Client client = null;

        try {
            ClientConfig clientConfig = ClientConfig.createClientConfig();
            Socket socket = SocketFactory.getDefault().createSocket(clientConfig.getServerAddress(), clientConfig.getPort());
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            Chat chat = new Chat(">");

            client = new Client(clientConfig, socket, executor, chat, isRunning);
        } catch (UnknownHostException | ConnectException e) {
            LOGGER.error("Server not found", e);
        } catch (IOException e) {
            LOGGER.error("Error creating Socket", e);
        }

        return client;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public void run() {
        final AsyncSocketIO socketIO = AsyncSocketIO.getAsyncSocketIO(socket, executor);

        final var reader = new BufferedReader(new InputStreamReader(socketIO.getInputStream(), StandardCharsets.UTF_8));
        final var writer = new PrintWriter(socketIO.getOutputStream(), true, StandardCharsets.UTF_8);

        String serverName = exchangeNames(writer, reader);

        chat.prettyPrint("Connection Established with " + serverName);

        System.out.println("Type `quit` to exit");
        CompletableFuture.allOf(receiveMessages(reader), sendMessages(writer)).join();
        reconnectPrompt();
    }

    private CompletableFuture<Void> receiveMessages(BufferedReader reader) {
        return CompletableFuture.runAsync(() -> {
            try {
                reader.lines()
                    .map(ConnectionMessage::fromRawString)
                    .forEach(msg -> {
                        chat.showConnectionMessage(msg);
                        chat.showUserPrompt();
                        System.out.print(SingletonTerminal.TERMINAL.getLineReader().getBuffer());
                    });
            } catch (UncheckedIOException e) {
                LOGGER.debug("The connection with the server has ended");
            } finally {
                executor.shutdownNow();
            }
        }, executor);
    }

    private CompletableFuture<Void> sendMessages(PrintWriter writer) {
        return CompletableFuture.runAsync(() -> {
            String line;
            try {
                while ((line = SingletonTerminal.TERMINAL.getLineReader().readLine(chat.userPrompt() + " ")) != null) {
                    if (line.equals("quit")) {
                        break;
                    }
                    String msg = ConnectionMessage.toRawString(config.getName(), line);
                    writer.println(msg);
                }
            } catch (EndOfFileException | UserInterruptException e) {
                LOGGER.debug("User stopped the console reading, probably by pressing CTRL + C", e);
            } finally {
                executor.shutdownNow();
            }
        }, executor);
    }

    private String exchangeNames(PrintWriter writer, BufferedReader reader) {

        final var sendNameFuture = CompletableFuture.runAsync(() -> {
            writer.println(config.getName());
            String logMessage = "Sent name (" + config.getName() + ") to server.";
            Client.getLogger().debug(logMessage);
        }, executor);

        final var receiveNameFuture = CompletableFuture.supplyAsync(() -> {
            String serverName = "";
            try {
                serverName = reader.readLine();
                String logMessage = "Received name from server: " + serverName + ".";
                Client.getLogger().debug(logMessage);
            } catch (IOException e) {
                Client.getLogger().error("Error receiving server name: {}", e, e);
            }

            return serverName;
        }, executor);

        CompletableFuture.allOf(sendNameFuture, receiveNameFuture).join();

        return receiveNameFuture.join();
    }

    private void reconnectPrompt() {
        try {
            String choice = SingletonTerminal.TERMINAL.getLineReader().readLine("Connect to another server? y/N: ");
            if (!choice.equalsIgnoreCase("Y")) {
                throw new UserInterruptException("Reconnection choice not made");
            }
        } catch (UserInterruptException | EndOfFileException e) {
            isRunning.set(false);
            chat.prettyPrint("Chat Ended");
        }
    }

    @Override
    public void close() {
        LOGGER.debug("Closing connection");
        try {
            executor.shutdown();
            socket.close();
        } catch (IOException e) {
            LOGGER.error("Error closing the socket connection: {}", e, e);
        }
    }
}
