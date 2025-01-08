package main.server;

import main.chat.ConsoleChat;
import main.chat.CustomTerminal;
import main.connection.AsyncSocketIO;
import main.connection.Connection;
import main.connection.DesignMessage;
import main.exchange.DefaultServerExchangeNamesStrategy;
import main.exchange.ExchangeNamesStrategy;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.logging.Logger;

public final class DesignServer implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(DesignServer.class.getName());
    private final List<Connection> clientConnections = new CopyOnWriteArrayList<>();

    private final CustomTerminal terminal;
    private final DesignServerConfig config;
    private final ServerSocket serverSocket;
    private final ExecutorService executor;

    public DesignServer(CustomTerminal terminal, DesignServerConfig config, ServerSocket serverSocket) {
        this.terminal = terminal;
        this.config = config;
        this.serverSocket = serverSocket;
        this.executor = config.executor();
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public void runServer() {
        LOGGER.info("Server Started...");
        CompletableFuture.anyOf(listenToConnections(), broadcastToConnections()).join();
    }

    private void receiveMessages(BufferedReader reader, Connection clientConnection) {
        executor.execute(() -> {
            try {
                reader.lines()
                    .map(msg -> {
                        msg = new String(msg.getBytes(), StandardCharsets.UTF_8);
                        return DesignMessage.fromRawString(msg);
                    })
                    .forEach(msg -> {
                        config.chat().showConnectionMessage(msg);
                        config.chat().showBufferedMessage(terminal.getLineReader().getBuffer().toString());
                        clientConnections
                            .stream()
                            .filter(Predicate.not(clientConnection::equals))
                            .forEach(connection -> CompletableFuture.runAsync(() ->
                                connection.output().println(msg.toRawString())));
                    });

            } catch (UncheckedIOException ignored) {
                LOGGER.fine("Connection with " + clientConnection.name() + "ended abruptly");
            } finally {
                LOGGER.info("User Disconnected: " + clientConnection.name());
                config.chat().showBufferedMessage(terminal.getLineReader().getBuffer().toString());
                clientConnection.close();
                clientConnections.remove(clientConnection);
            }
        });
    }

    private CompletableFuture<Void> listenToConnections() {
        LOGGER.info("Listening to connections...");

        return CompletableFuture.runAsync(() -> {
            while (!serverSocket.isClosed() && serverSocket.isBound()) {
                final Socket clientSocket;
                try {
                    clientSocket = serverSocket.accept();

                    final AsyncSocketIO asyncSocketIO = AsyncSocketIO.getAsyncSocketIO(clientSocket, executor);

                    final var reader = new BufferedReader(new InputStreamReader(asyncSocketIO.getInputStream()));
                    final var writer = new PrintWriter(asyncSocketIO.getOutputStream(), true, StandardCharsets.UTF_8);

                    final ExchangeNamesStrategy exchangeNamesStrategy = new DefaultServerExchangeNamesStrategy(reader, writer, config.name(), executor);
                    String clientName = exchangeNamesStrategy.exchange();

                    ConsoleChat.cleanLine();
                    System.out.println("User " + clientName + " connected");
                    config.chat().showBufferedMessage(terminal.getLineReader().getBuffer().toString());

                    Connection clientConnection = new Connection(clientName, clientSocket, reader, writer);
                    clientConnections.add(clientConnection);

                    receiveMessages(reader, clientConnection);
                } catch (IOException e) {
                    LOGGER.fine("Connection listening ended or error during Socket Server accept method: " + e);
                }
            }
        }, executor);

    }

    private CompletableFuture<Void> broadcastToConnections() {
        LOGGER.info("Broadcasting to connections...");

        return CompletableFuture.runAsync(() -> {
            String line;
            try {
                while ((line = terminal.getLineReader().readLine(config.chat().userPrompt() + " ")) != null) {
                    String msg = DesignMessage.toRawString(config.name(), line);

                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    for (Connection connection : clientConnections) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(()
                            -> connection.output().println(msg), executor);
                        futures.add(future);
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
            } catch (EndOfFileException | UserInterruptException e) {
                LOGGER.fine("User stopped the console reading, probably by pressing CTRL + C");
            }
        }, executor);
    }

    @Override
    public void close() {
        LOGGER.info("Closing all connections...");
        final var futures = clientConnections.stream().map(connection ->
            CompletableFuture.runAsync(connection::close)).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
        executor.shutdown();
    }

}
