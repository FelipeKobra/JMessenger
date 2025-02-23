package org.gladiator.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.crypto.SecretKey;
import javax.net.ServerSocketFactory;
import org.gladiator.exception.EndApplicationException;
import org.gladiator.exception.FailedExchangeException;
import org.gladiator.exception.InvalidMessageException;
import org.gladiator.server.config.ServerConfig;
import org.gladiator.server.config.ServerConfigFactory;
import org.gladiator.server.network.PortMapper;
import org.gladiator.util.chat.ChatUtils;
import org.gladiator.util.connection.Connection;
import org.gladiator.util.connection.IoUtils;
import org.gladiator.util.connection.exchange.NameExchange;
import org.gladiator.util.connection.message.ConnectionMessageFactory;
import org.gladiator.util.connection.message.NonServerSideOnlyPredicate;
import org.gladiator.util.connection.message.model.DisconnectMessage;
import org.gladiator.util.connection.message.model.Message;
import org.gladiator.util.connection.message.model.NewConnectionMessage;
import org.gladiator.util.connection.message.model.SimpleMessage;
import org.gladiator.util.crypto.CryptographyManager;
import org.gladiator.util.thread.NamedVirtualThreadExecutorFactory;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a server that manages connections with clients.
 */
public final class Server implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
  private final List<Connection> clientConnections = new CopyOnWriteArrayList<>();
  private final AtomicBoolean isClosingManually = new AtomicBoolean(false);
  private final CryptographyManager cryptographyManager;

  private final ServerConfig serverConfig;
  private final ServerSocket serverSocket;
  private final ChatUtils chatUtils;
  private final ExecutorService executor;

  /**
   * Constructs a new Server instance.
   *
   * @param cryptographyManager the RSA cryptography keys manager
   * @param serverConfig        the server configuration
   * @param serverSocket        the server socket
   * @param chatUtils           the chat utilities
   * @param executor            the executor service
   */
  private Server(final CryptographyManager cryptographyManager, final ServerConfig serverConfig,
      final ServerSocket serverSocket,
      final ChatUtils chatUtils, final ExecutorService executor) {
    this.cryptographyManager = cryptographyManager;
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
      final ExecutorService executor = NamedVirtualThreadExecutorFactory.create("server");
      final ServerSocket serverSocket = createServerSocket(serverConfig.port(), chatUtils);
      final CryptographyManager keysManager = CryptographyManager.create();

      server = new Server(keysManager, serverConfig, serverSocket, chatUtils, executor);
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
      try {
        final Socket clientSocket = serverSocket.accept();

        sendRsaPublicKey(clientSocket);

        final SecretKey clientAesKey = receiveAesKey(clientSocket);
        final String clientName = new NameExchange(clientSocket, clientAesKey,
            cryptographyManager,
            serverConfig.name(), executor).exchange();

        final Connection clientConnection = Connection.create(clientName, clientSocket,
            clientAesKey);

        clientConnections.add(clientConnection);
        final Message newConnectionMessage = new NewConnectionMessage(clientName);

        chatUtils.showNewMessage(newConnectionMessage);
        sendToOtherConnections(newConnectionMessage, clientConnection);

        receiveMessages(clientConnection);
      } catch (final IOException e) {
        LOGGER.debug(
            "Connection listening ended normally or error during Socket Server accept method: {}",
            e.getMessage());
      } catch (final FailedExchangeException e) {
        LOGGER.debug("Error during exchange name or Keys exchange", e);
      }
    }

    executor.shutdownNow();
  }

  private void sendRsaPublicKey(final Socket socket) throws FailedExchangeException {
    try {
      final ObjectOutput writer = IoUtils.createObjectWriter(socket);
      final Key ownPublicKey = cryptographyManager.getRsaPublicKey();
      writer.writeObject(ownPublicKey);
      final String logMessage = "Sent RSA public key";
      LOGGER.debug(logMessage);
    } catch (final IOException e) {
      LOGGER.error("Error sending RSA key");
      throw new FailedExchangeException(e);
    }
  }

  private SecretKey receiveAesKey(final Socket socket) {
    try {
      final BufferedReader objectReader = IoUtils.createReader(socket);
      final String encryptedAesKeyString = objectReader.readLine();
      final SecretKey aesKey = cryptographyManager.decryptRsa(encryptedAesKeyString);
      final String logMessage = "Received AES key";
      LOGGER.debug(logMessage);

      return aesKey;
    } catch (final IOException e) {
      LOGGER.error("Error receiving AES key");
      throw new UncheckedIOException(e);
    }
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

        if (!line.isBlank()) {
          final Message msg = new SimpleMessage(serverConfig.name(), line);
          broadcastMessageToConnections(msg);
        }
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
   * @param message The message to be broadcast.
   */
  private void broadcastMessageToConnections(final Message message) {
    final List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (final Connection connection : clientConnections) {
      final CompletableFuture<Void> future = CompletableFuture.runAsync(
          () -> connection.writeOutput(message, cryptographyManager), executor);
      futures.add(future);
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  /**
   * Receives messages from a client.
   *
   * @param clientConnection The connection to the client.
   */
  private void receiveMessages(final Connection clientConnection) {
    final String clientName = clientConnection.getName();

    executor.execute(() -> {
      try {
        processMessages(clientConnection);
      } catch (final UncheckedIOException e) {
        LOGGER.debug("Connection with {} ended abruptly", clientName, e);
      } finally {
        closeConnection(clientConnection);
      }
    });
  }

  /**
   * Processes messages received from a client. This method shows the message on the console and
   * redirects it to other connected clients.
   *
   * @param connection The Connection object representing the client's connection.
   */
  private void processMessages(final Connection connection) {
    connection.readStream(cryptographyManager)
        .map(transportMessage -> {
          try {
            return ConnectionMessageFactory.createFromString(transportMessage);
          } catch (final InvalidMessageException e) {
            LOGGER.debug(InvalidMessageException.DEFAULT_PROMPT, e);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .filter(new NonServerSideOnlyPredicate())
        .forEach(msg -> {
          chatUtils.showNewMessage(msg);
          sendToOtherConnections(msg, connection);
        });
  }

  /**
   * Sends a message to all connected clients, except the client that sent the message.
   *
   * @param message    The message to be sent.
   * @param connection The connection to the client that sent the message.
   */
  private void sendToOtherConnections(final Message message, final Connection connection) {
    clientConnections.stream()
        .filter(Predicate.not(connection::equals))
        .forEach(otherConnection -> {
          final CompletableFuture<Void> sendMessageFuture = CompletableFuture.runAsync(
              () -> otherConnection.writeOutput(message, cryptographyManager), executor);
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
   */
  private void closeConnection(final Connection connection) {
    final String clientName = connection.getName();

    if (!isClosingManually.get()) {
      connection.removeConnection(clientConnections);

      final Message disconnectMessage = new DisconnectMessage(clientName);
      broadcastMessageToConnections(disconnectMessage);
      chatUtils.showNewMessage(disconnectMessage);

      LOGGER.debug("User Disconnected: {}", clientName);
    }
  }


  /**
   * Closes the server socket.
   */
  private void closeServerSocket() {
    try {
      serverSocket.close();
    } catch (final IOException e) {
      LOGGER.error("Error during closing server socket", e);
    }
  }

  /**
   * Closes the server and all client connections.
   */
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