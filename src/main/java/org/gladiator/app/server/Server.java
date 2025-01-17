package app.server;

import app.exception.EndApplicationException;
import app.server.config.ServerConfig;
import app.server.config.ServerConfigFactory;
import app.util.ChatUtils;
import app.util.PortMapper;
import app.util.connection.Connection;
import app.util.connection.ConnectionMessage;
import app.util.connection.ConnectionMessageUtils;
import app.util.io.SocketIo;
import app.util.io.SocketIoAsyncFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.net.ServerSocketFactory;
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
  private final PortMapper portMapper;
  private final ExecutorService executor;


  private Server(final ServerConfig serverConfig, final ServerSocket serverSocket,
      final ChatUtils chatUtils, final PortMapper portMapper, final ExecutorService executor) {
    this.serverConfig = serverConfig;
    this.serverSocket = serverSocket;
    this.chatUtils = chatUtils;
    this.portMapper = portMapper;
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
      final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
      final ServerSocket serverSocket = ServerSocketFactory.getDefault()
          .createServerSocket(serverConfig.port());

      final PortMapper portMapper = PortMapper.createDefault();
      portMapper.openPort(serverConfig.port());

      server = new Server(serverConfig, serverSocket, chatUtils, portMapper, executor);
    } catch (final BindException e) {
      LOGGER.debug("Port already in use", e);
      chatUtils.displayOnScreen(
          "Address already in use, check if you have another server opened in the same port");
      throw new EndApplicationException(e);
    } catch (final IOException e) {
      LOGGER.error("Error creating Server Socket", e);
      throw new EndApplicationException(e);
    } catch (final UserInterruptException e) {
      LOGGER.debug(ChatUtils.USER_INTERRUPT_MESSAGE);
      throw new EndApplicationException(e);
    }

    Objects.requireNonNull(server);
    return server;
  }

  /**
   * Starts the server and begins listening for connections.
   */
  public void runServer() {
    LOGGER.info("Server Started...");
    final CompletableFuture<Void> listenToConnectionsFuture = CompletableFuture.runAsync(
        this::listenToConnections, executor);

    final CompletableFuture<Void> broadcastToConnectionsFuture = CompletableFuture.runAsync(
        this::broadcastToConnections, executor);

    CompletableFuture.allOf(listenToConnectionsFuture, broadcastToConnectionsFuture).join();
  }

  /**
   * Listens for incoming connections from clients.
   */
  private void listenToConnections() {
    LOGGER.info("Listening to connections...");

    while (!serverSocket.isClosed() && serverSocket.isBound()) {
      final Socket clientSocket;
      try {
        clientSocket = serverSocket.accept();

        final SocketIo socketIo = SocketIoAsyncFactory.create(clientSocket, executor);
        final PrintWriter writer = socketIo.getWriter();
        final BufferedReader reader = socketIo.getReader();

        final String clientName = exchangeNames(writer, reader);

        chatUtils.showNewMessage("User " + clientName + " Connected");

        final Connection clientConnection = new Connection(clientName, clientSocket, reader,
            writer);
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
    LOGGER.info("Broadcasting to connections...");
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
   * @param msg The message to be broadcasted.
   */
  private void broadcastMessageToConnections(final String msg) {
    final List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (final Connection connection : clientConnections) {
      final CompletableFuture<Void> future = CompletableFuture.runAsync(
          () -> connection.getOutput().println(msg), executor);
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
    clientConnections.stream().filter(Predicate.not(connection::equals)).forEach(
        otherConnection -> CompletableFuture.runAsync(
            () -> otherConnection.getOutput().println(msg.toRawString()), executor));
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

  @Override
  public void close() {
    LOGGER.info("Closing all connections...");

    isClosingManually.set(true);

    try {
      for (final Connection connection : clientConnections) {
        connection.close();
      }
      serverSocket.close();
    } catch (final IOException e) {
      LOGGER.error("Error during closing server socket", e);
    } finally {
      chatUtils.close();
      executor.shutdown();
      portMapper.closeAll(serverConfig.port());
    }
  }

}