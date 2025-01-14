package app.server;

import app.server.config.ServerConfig;
import app.server.config.ServerConfigFactory;
import app.util.AsyncSocketIO;
import app.util.Chat;
import app.util.PortMapper;
import app.util.SingletonTerminal;
import app.util.connection.Connection;
import app.util.connection.ConnectionMessage;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class Server implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class.getName());
    private final List<Connection> clientConnections = new CopyOnWriteArrayList<>();
    private final AtomicBoolean isClosingManually = new AtomicBoolean(false);

    private final ServerConfig serverConfig;
    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final Chat chat;
    private final PortMapper portMapper;

    private Server(ServerConfig serverConfig, ServerSocket serverSocket,
                   ExecutorService executor, Chat chat, PortMapper portMapper) {
        this.serverConfig = serverConfig;
        this.serverSocket = serverSocket;
        this.executor = executor;
        this.chat = chat;
        this.portMapper = portMapper;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Server createServer() throws UserInterruptException {
        Server server = null;
        try {
            ServerConfig serverConfig = new ServerConfigFactory().create();
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(serverConfig.port());
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            Chat chat = new Chat(">");

            PortMapper portMapper = PortMapper.createDefault();
            portMapper.openPort(serverConfig.port());

            server = new Server(serverConfig, serverSocket, executor, chat, portMapper);
        } catch (BindException e) {
            LOGGER.error("Address already in use, check if you have another server opened in the same port");
        } catch (IOException e) {
            LOGGER.error("Error creating Server Socket", e);
        }

        Objects.requireNonNull(server);
        return server;
    }

    public void runServer() {
        LOGGER.info("Server Started...");
        CompletableFuture.anyOf(listenToConnections(), broadcastToConnections()).join();
    }

    private CompletableFuture<Void> listenToConnections() {
        LOGGER.info("Listening to connections...");

        return CompletableFuture.runAsync(() -> {
            while (!serverSocket.isClosed() && serverSocket.isBound()) {
                final Socket clientSocket;
                try {
                    clientSocket = serverSocket.accept();

                    final AsyncSocketIO asyncSocketIO = AsyncSocketIO.getAsyncSocketIO(clientSocket, executor);

                    final var reader = new BufferedReader(new InputStreamReader(asyncSocketIO.getInputStream(), StandardCharsets.UTF_8));
                    final var writer = new PrintWriter(asyncSocketIO.getOutputStream(), true, StandardCharsets.UTF_8);

                    String clientName = exchangeNames(writer, reader);

                    chat.cleanLine();
                    System.out.println("User " + clientName + " connected");
                    chat.showBufferedMessage(SingletonTerminal.TERMINAL.getLineReader().getBuffer().toString());

                    Connection clientConnection = new Connection(clientName, clientSocket, reader, writer);
                    clientConnections.add(clientConnection);

                    receiveMessages(reader, clientConnection);
                } catch (IOException e) {
                    LOGGER.debug("Connection listening ended normally or error during Socket Server accept method: {}", e.getMessage());
                }
            }
        }, executor);
    }

    private CompletableFuture<Void> broadcastToConnections() {
        LOGGER.info("Broadcasting to connections...");

        System.out.println("Type `quit` to exit");
        return CompletableFuture.runAsync(() -> {
            String line;
            try {
                while ((line = SingletonTerminal.TERMINAL.getLineReader().readLine(chat.userPrompt() + " ")) != null) {
                    if (line.equals("quit")) {
                        throw new UserInterruptException("User typed `quit`");
                    }

                    String msg = ConnectionMessage.toRawString(serverConfig.name(), line);

                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    for (Connection connection : clientConnections) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(()
                            -> connection.output().println(msg), executor);
                        futures.add(future);
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
            } catch (EndOfFileException | UserInterruptException e) {
                LOGGER.debug("User stopped the console reading, probably by pressing CTRL + C: {}", String.valueOf(e));
            }
        }, executor);
    }

    private void receiveMessages(BufferedReader reader, Connection clientConnection) {
        executor.execute(() -> {
            try {
                reader.lines()
                    .map(ConnectionMessage::fromRawString)
                    .forEach(msg -> {
                        chat.showConnectionMessage(msg);
                        chat.showBufferedMessage(SingletonTerminal.TERMINAL.getLineReader().getBuffer().toString());
                        clientConnections
                            .stream()
                            .filter(Predicate.not(clientConnection::equals))
                            .forEach(connection -> CompletableFuture.runAsync(() ->
                                connection.output().println(msg.toRawString())));
                    });

            } catch (UncheckedIOException ignored) {
                LOGGER.debug("Connection with {} ended abruptly", clientConnection.name());
            } finally {
                if (!isClosingManually.get()) {
                    LOGGER.info("User Disconnected: {}", clientConnection.name());
                    chat.showBufferedMessage(SingletonTerminal.TERMINAL.getLineReader().getBuffer().toString());
                    clientConnection.close();
                    clientConnections.remove(clientConnection);
                }
            }
        });
    }

    private String exchangeNames(PrintWriter writer, BufferedReader reader) {

        final var sendServerNameFuture =
            CompletableFuture
                .runAsync(() -> {
                    writer.println(serverConfig.name());
                    String logMessage = "Sent name " +
                        serverConfig.name() +
                        " to client.";
                    Server.getLogger().debug(logMessage);
                }, executor);

        final var receiveClientNameFuture = CompletableFuture
            .supplyAsync(() -> {
                String clientName = "";
                try {
                    clientName = reader.readLine();
                    String logMessage = "Received name from client: " + clientName + ".";
                    Server.getLogger().debug(logMessage);
                } catch (IOException e) {
                    Server.getLogger().error("Error receiving client name: {}", e, e);
                }
                return clientName;
            }, executor);

        CompletableFuture.allOf(sendServerNameFuture, receiveClientNameFuture).join();

        return receiveClientNameFuture.join();
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing all connections...");

        isClosingManually.set(true);
        final var futures = clientConnections.stream().map(connection ->
            CompletableFuture.runAsync(connection::close)).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();

        SingletonTerminal.TERMINAL.close();
        executor.shutdown();
        serverSocket.close();
        portMapper.closePort(serverConfig.port());
        portMapper.close();
    }

}
