package org.gladiator.app.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.net.ServerSocketFactory;
import org.gladiator.app.exception.EndApplicationException;
import org.gladiator.app.server.config.ServerConfig;
import org.gladiator.app.server.config.ServerConfigFactory;
import org.gladiator.app.util.ChatUtils;
import org.gladiator.app.util.NamedVirtualThreadExecutorFactory;
import org.gladiator.app.util.PortMapper;
import org.gladiator.app.util.connection.Connection;
import org.gladiator.app.util.connection.ConnectionMessage;
import org.gladiator.app.util.connection.ConnectionMessageUtils;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a server that manages connections with clients.
 */
public final class Server implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class.getName());

  private final List<Connection> clientConnections = new CopyOnWriteArrayList<>();
  private final AtomicBoolean isClosingManually = new AtomicBoolean(false);

  private final ServerConfig serverConfig;
  private final ServerSocket serverSocket;
  private final ChatUtils chatUtils;
  private final ExecutorService executor;

  private Server(final ServerConfig serverConfig, final ServerSocket serverSocket,
      final ChatUtils chatUtils, final ExecutorService executor) {
    this.serverConfig = serverConfig;
    this.serverSocket = serverSocket;
    this.chatUtils = chatUtils;
    this.executor = executor;
  }

  /**
   * Creates a new server with the default configuration.
   *
   * @return A new server instance.
   * @throws EndApplicationException If an error occurs during server creation.
   */
  public static Server createServer()
      throws EndApplicationException {

    final Server server;
    final ChatUtils chatUtils = ChatUtils.create(">");
    try {
      final ServerConfig serverConfig = new ServerConfigFactory(chatUtils).create();
      final ExecutorService executor = NamedVirtualThreadExecutorFactory.create("Server");
      final ServerSocket serverSocket = createServerSocket(serverConfig.port(), chatUtils);

      server = new Server(serverConfig, serverSocket, chatUtils, executor);
    } catch (final UserInterruptException e) {
      LOGGER.debug(ChatUtils.USER_INTERRUPT_MESSAGE);
      throw new EndApplicationException(e);
    }

    Objects.requireNonNull(server);
    return server;
  }

  /**
   * Creates a ServerSocket bound to the specified port.
   *
   * @param port      the port number to bind the ServerSocket to
   * @param chatUtils the ChatUtils instance for user interaction
   * @return a ServerSocket bound to the specified port
   * @throws EndApplicationException if an error occurs during ServerSocket creation
   */
  private static ServerSocket createServerSocket(final int port, final ChatUtils chatUtils)
      throws EndApplicationException {

    final ServerSocket serverSocket;
    try {
      serverSocket = ServerSocketFactory.getDefault().createServerSocket(port);
    } catch (final BindException e) {
      chatUtils.displayOnScreen(
          "Address already in use, check if you have another server opened in the same port");
      throw new EndApplicationException("Port already in use" + e);
    } catch (final IOException e) {
      throw new EndApplicationException("Error creating Server Socket" + e);
    }
    return serverSocket;
  }

  /**
   * Starts the server, begins listening and broadcasting for connections.
   */
  public void runServer() {
    LOGGER.info("Server Started...");

    final int serverPort = serverConfig.port();

    try (final PortMapper portMapper = PortMapper.createDefault(serverPort)) {
      portMapper.openPort();

      final CompletableFuture<Void> listenToConnectionsFuture = CompletableFuture.runAsync(
          this::listenToConnections, executor);

      final CompletableFuture<Void> broadcastToConnectionsFuture = CompletableFuture.runAsync(
          this::broadcastToConnections, executor);

      CompletableFuture.allOf(listenToConnectionsFuture, broadcastToConnectionsFuture).join();
    }
  }

  /**
   * Listens for incoming connections from clients.
   */
  private void listenToConnections() {
    LOGGER.debug("Listening to connections...");

    while (!serverSocket.isClosed() && serverSocket.isBound()) {
      try (final Socket clientSocket = serverSocket.accept()) {

        final PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true,
            StandardCharsets.UTF_8);
        final BufferedReader reader = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

        final String clientName = exchangeNames(writer, reader);

        chatUtils.showNewMessage("User " + clientName + " Connected");

        final Connection clientConnection = new Connection(clientName, clientSocket);
        clientConnections.add(clientConnection);

        receiveMessages(reader, clientConnection);
      } catch (final IOException e) {
        LOGGER.debug(
            "Connection listening ended normally or error during Socket Server accept method: {}",
            e.getMessage());
      }
    }

    executor.shutdownNow();
  }

  /**
   * Broadcasts messages to all connected clients.
   */
  private void broadcastToConnections() {
    LOGGER.debug("Broadcasting to connections...");
    LOGGER.info("Type `quit` to exit");

    try {
      String line = chatUtils.getUserInput();

      while (null != line) {
        if ("quit".equals(line)) {
          break;
        }

        final String msg = ConnectionMessageUtils.toRawString(serverConfig.name(), line);
        broadcastMessageToConnections(msg);
        line = chatUtils.getUserInput();
      }
    } catch (final EndOfFileException | UserInterruptException e) {
      LOGGER.debug(ChatUtils.USER_INTERRUPT_MESSAGE, e);
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * Broadcasts a message to all connected clients.
   *
   * @param msg The message to be broadcast.
   */
  private void broadcastMessageToConnections(final String msg) {
    final List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (final Connection connection : clientConnections) {
      final CompletableFuture<Void> future = CompletableFuture.runAsync(
          () -> connection.writeOutput(msg), executor);
      futures.add(future);
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  /**
   * Receives messages from a client.
   *
   * @param reader           The reader for the client's messages.
   * @param clientConnection The connection to the client.
   */
  private void receiveMessages(final BufferedReader reader, final Connection clientConnection) {
    final String clientName = clientConnection.getName();

    executor.execute(() -> {
      try {
        processMessages(reader, clientConnection);
      } catch (final UncheckedIOException e) {
        LOGGER.debug("Connection with {} ended abruptly", clientName);
      } finally {
        closeConnection(clientConnection, clientName);
      }
    });
  }

  /**
   * Processes messages received from a client.
   *
   * @param reader     The reader for the client's messages.
   * @param connection The connection to the client.
   */
  private void processMessages(final BufferedReader reader, final Connection connection) {
    reader.lines().map(ConnectionMessageUtils::fromRawString).forEach(msg -> {
      chatUtils.showNewMessage(msg.toString());
      sendToOtherConnections(msg, connection);
    });
  }

  /**
   * Sends a message to all connected clients, except the client that sent the message.
   *
   * @param msg        The message to be sent.
   * @param connection The connection to the client that sent the message.
   */
  private void sendToOtherConnections(final ConnectionMessage msg, final Connection connection) {
    clientConnections.stream()
        .filter(Predicate.not(connection::equals))
        .forEach(otherConnection -> {
          final CompletableFuture<Void> sendMessageFuture = CompletableFuture.runAsync(
              () -> otherConnection.writeOutput(msg.toRawString()), executor);
          sendMessageFuture.exceptionally(ex -> {
            LOGGER.error("Error writing output to connection", ex);
            return null;
          });
        });

  }

  /**
   * Closes a connection to a client.
   *
   * @param connection The connection to be closed.
   * @param clientName The name of the client.
   */
  private void closeConnection(final Connection connection, final String clientName) {
    if (!isClosingManually.get()) {
      LOGGER.debug("User Disconnected: {}", clientName);
      chatUtils.showNewMessage("User " + clientName + " Disconnected");
      connection.removeConnection(clientConnections);
    }
  }

  /**
   * Exchanges names between the server and a client.
   *
   * @param writer The writer for the client's messages.
   * @param reader The reader for the client's messages.
   * @return The name of the client.
   */
  private String exchangeNames(final PrintWriter writer, final BufferedReader reader) {

    final CompletableFuture<Void> sendServerNameFuture = CompletableFuture.runAsync(() -> {
      writer.println(serverConfig.name());
      final String logMessage = "Sent name " + serverConfig.name() + " to client.";
      LOGGER.debug(logMessage);
    }, executor);

    final CompletableFuture<String> receiveClientNameFuture = CompletableFuture.supplyAsync(() -> {
      String clientName = "";
      try {
        clientName = reader.readLine();
        final String logMessage = "Received name from client: " + clientName + ".";
        LOGGER.debug(logMessage);
      } catch (final IOException e) {
        LOGGER.error("Error receiving client name: {}", e, e);
      }
      return clientName;
    }, executor);

    CompletableFuture.allOf(sendServerNameFuture, receiveClientNameFuture).join();

    return receiveClientNameFuture.join();
  }

  private void closeServerSocket() {
    try {
      serverSocket.close();
    } catch (final IOException e) {
      LOGGER.error("Error during closing server socket", e);
    }
  }

  @Override
  public void close() {
    LOGGER.info("Closing all connections...");

    isClosingManually.set(true);

    final CompletableFuture<?>[] closeConnectionsFuture = clientConnections.stream()
        .map(connection -> CompletableFuture.runAsync(connection::close))
        .toArray(CompletableFuture<?>[]::new);

    closeServerSocket();
    chatUtils.close();
    executor.shutdownNow();
    CompletableFuture.allOf(closeConnectionsFuture).join();
  }

}
