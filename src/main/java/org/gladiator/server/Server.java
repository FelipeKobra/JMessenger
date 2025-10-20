package org.gladiator.server;

import static org.gladiator.server.admin.CommandUtils.BAN_PATTERN;
import static org.gladiator.server.admin.CommandUtils.formatCommandList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import javax.crypto.SecretKey;
import javax.net.ServerSocketFactory;
import org.gladiator.exception.EndApplicationException;
import org.gladiator.exception.FailedExchangeException;
import org.gladiator.exception.InvalidMessageException;
import org.gladiator.server.admin.BanUtils;
import org.gladiator.server.admin.Command;
import org.gladiator.server.admin.CommandUtils;
import org.gladiator.server.config.ServerConfig;
import org.gladiator.server.config.ServerConfigFactory;
import org.gladiator.server.network.PortMapper;
import org.gladiator.util.chat.ChatUtils;
import org.gladiator.util.connection.Connection;
import org.gladiator.util.connection.SocketIo;
import org.gladiator.util.connection.exchange.NameExchange;
import org.gladiator.util.connection.message.ConnectionMessageFactory;
import org.gladiator.util.connection.message.NonServerSideOnlyPredicate;
import org.gladiator.util.connection.message.model.BanMessage;
import org.gladiator.util.connection.message.model.DisconnectMessage;
import org.gladiator.util.connection.message.model.KickMessage;
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

  public static final String PERMA = "perma";
  private static final String IS_NOT_CONNECTED = " is not connected.";
  private static final String UNKNOWN_COMMAND_MESSAGE = "Unknown command. Type /help for a list of commands.";
  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
  private final List<Connection> clientConnections = new CopyOnWriteArrayList<>();
  private final Map<InetAddress, Instant> bannedUsers = new ConcurrentHashMap<>();
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
  private Server(
      final CryptographyManager cryptographyManager,
      final ServerConfig serverConfig,
      final ServerSocket serverSocket,
      final ChatUtils chatUtils,
      final ExecutorService executor) {
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
  public static Server createServer() throws EndApplicationException {

    final Server server;
    final ChatUtils chatUtils = ChatUtils.create(">");
    try {
      final ServerConfig serverConfig = new ServerConfigFactory(chatUtils).create();
      final ServerSocket serverSocket = createServerSocket(serverConfig.port(), chatUtils);
      final CryptographyManager keysManager = CryptographyManager.create();

      server =
          new Server(
              keysManager,
              serverConfig,
              serverSocket,
              chatUtils,
              NamedVirtualThreadExecutorFactory.create("server"));
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
      if (serverConfig.enableUpnp()) {
        portMapper.openPort();
      }

      final CompletableFuture<Void> listenToConnectionsFuture =
          CompletableFuture.runAsync(this::listenToConnections, executor);

      final CompletableFuture<Void> broadcastToConnectionsFuture =
          CompletableFuture.runAsync(this::broadcastToConnections, executor);

      CompletableFuture.allOf(listenToConnectionsFuture, broadcastToConnectionsFuture).join();
    }
  }

  /**
   * Listens for incoming client connections and handles them. This method runs in a loop until the
   * server socket is closed or unbound.
   */
  private void listenToConnections() {
    LOGGER.debug("Listening to connections...");

    while (!serverSocket.isClosed() && serverSocket.isBound()) {
      try {
        final Socket clientSocket = serverSocket.accept();

        final SocketIo socketIo = SocketIo.create(clientSocket);

        final SecretKey clientAesKey = exchangeCryptographyKeys(socketIo);

        final String clientName =
            new NameExchange(clientAesKey, cryptographyManager, serverConfig.name(), executor)
                .exchange(socketIo);

        final Connection clientConnection =
            handleNewClientConnection(socketIo, clientSocket, clientName, clientAesKey);

        if (bannedUsers.containsKey(clientSocket.getInetAddress())
            && Instant.now().isBefore(bannedUsers.get(clientSocket.getInetAddress()))) {
          final Message bannedUserMessage =
              new SimpleMessage(
                  serverConfig.name(),
                  "You are banned from this server until "
                      + bannedUsers.get(clientSocket.getInetAddress()).toString());
          clientConnection.writeOutput(bannedUserMessage, cryptographyManager);
          closeConnection(clientConnection);
          continue;
        }

        boolean duplicateFound = false;

        for (final Connection connection : clientConnections) {
          if (connection.getName().equalsIgnoreCase(clientName) && connection != clientConnection) {
            final Message duplicateUserMessage =
                new SimpleMessage(
                    serverConfig.name(),
                    "A user with the name "
                        + clientName
                        + " is already connected. Connection will be closed.");
            clientConnection.writeOutput(duplicateUserMessage, cryptographyManager);

            closeConnection(clientConnection);
            duplicateFound = true;
            break;
          }
        }

        if (duplicateFound) {
          continue;
        }

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

  /**
   * Handles a new client connection by creating a {@link Connection} object, adding it to the list
   * of client connections, and broadcasting a {@link NewConnectionMessage} to other clients.
   *
   * @param socketIo     The SocketIo for the client connection.
   * @param socket       The socket for the client connection.
   * @param clientName   The name of the client.
   * @param clientAesKey The AES key for encrypting/decrypting messages with the client.
   * @return The Connection object representing the client's connection.
   */
  private Connection handleNewClientConnection(
      final SocketIo socketIo,
      final Socket socket,
      final String clientName,
      final SecretKey clientAesKey) {

    final Connection clientConnection =
        Connection.create(clientName, socketIo, socket, clientAesKey);

    clientConnections.add(clientConnection);
    final Message newConnectionMessage = new NewConnectionMessage(clientName);

    chatUtils.showNewMessage(newConnectionMessage);
    sendToOtherConnections(newConnectionMessage, clientConnection);

    return clientConnection;
  }

  /**
   * Exchanges cryptographic keys with the client. This involves sending the server's RSA public key
   * to the client and receiving the client's AES key.
   *
   * @param socketIo The SocketIo for the client connection.
   * @return The AES key received from the client.
   * @throws FailedExchangeException If an error occurs during the key exchange process.
   */
  private SecretKey exchangeCryptographyKeys(final SocketIo socketIo)
      throws FailedExchangeException {

    sendRsaPublicKey(socketIo);
    return receiveAesKey(socketIo.getReader());
  }

  /**
   * Sends the server's RSA public key to the client.
   *
   * <p>Note: The public key is sent as bytes instead of an object because native images do not
   * support the deserialization of PublicKey objects due to the absence of a suitable constructor.
   *
   * @param socketIo The SocketIo for the client connection.
   * @throws FailedExchangeException if an error occurs while sending the RSA public key
   */
  private void sendRsaPublicKey(final SocketIo socketIo) throws FailedExchangeException {
    try {
      final Key ownPublicKey = cryptographyManager.getRsaPublicKey();
      socketIo.writeObject(ownPublicKey.getEncoded());
      final String logMessage = "Sent RSA public key";
      LOGGER.debug(logMessage);
    } catch (final IOException e) {
      LOGGER.error("Error sending RSA key");
      throw new FailedExchangeException(e);
    }
  }

  /**
   * Receives the AES key from the client.
   *
   * @param reader the BufferedReader to read the AES key
   * @return the AES key received from the client
   * @throws UncheckedIOException if an error occurs while receiving the AES key
   */
  private SecretKey receiveAesKey(final BufferedReader reader) {
    try {
      final String encryptedAesKeyString = reader.readLine();
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
    LOGGER.info("Type `/help` to display all commands");

    try {
      String line = chatUtils.getUserInput();

      while (null != line) {

        if (line.startsWith("/")) {
          handleCommand(line);
        } else if (!line.isBlank()) {
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

  private void handleCommand(final String commandLine) {
    final Command command = CommandUtils.getCommandByString(commandLine);

    if (null == command) {
      chatUtils.displayOnScreen(UNKNOWN_COMMAND_MESSAGE);
    }

    if (null != command) {
      switch (command) {
        case QUIT -> quitCommand();
        case HELP -> helpCommand(chatUtils);
        case BAN -> banCommand(commandLine, chatUtils, clientConnections, bannedUsers);
        case UNBAN -> unbanCommand(commandLine, chatUtils, bannedUsers);
        case KICK -> kickCommand(commandLine, chatUtils, clientConnections);
        case SHOW_USERS -> showUsersCommand(chatUtils, clientConnections);
        case SHOW_BANS -> showBansCommand(chatUtils, bannedUsers);
        default -> chatUtils.showSystemMessage(UNKNOWN_COMMAND_MESSAGE);
      }
    }
  }

  private void quitCommand() {
    throw new UserInterruptException("User requested to quit the application");
  }

  private void helpCommand(final ChatUtils chatUtils) {
    chatUtils.showSystemMessage(formatCommandList());
  }

  private void banCommand(
      final String commandLine,
      final ChatUtils chatUtils,
      final List<Connection> clientConnections,
      final Map<InetAddress, Instant> bannedUsers) {
    try {
      final Matcher matcher = BAN_PATTERN.matcher(commandLine);

      if (matcher.matches()) {
        final String user = matcher.group(1);

        String duration = matcher.group(2);

        if (null == duration) {
          duration = PERMA;
        }

        final Instant durationInstant = BanUtils.parseBanInputToInstant(duration);
        final String formattedDuration = BanUtils.formatRemaining(durationInstant);

        final String reason = null != matcher.group(3) ? matcher.group(3) : "";

        final Optional<Connection> userConnectionOpt =
            clientConnections.stream()
                .filter(conn -> conn.getName().equalsIgnoreCase(user))
                .findFirst();

        if (userConnectionOpt.isPresent()) {
          final Connection userConnection = userConnectionOpt.get();
          bannedUsers.put(userConnection.getIp(), durationInstant);
          final Message banMessage = new BanMessage(formattedDuration, user, reason);
          broadcastMessageToConnections(banMessage);
          userConnection.removeConnection(clientConnections);
          chatUtils.showSystemMessage("User " + user + " has been banned.");
        } else {
          chatUtils.showSystemMessage("User " + user + IS_NOT_CONNECTED);
        }

      } else {
        chatUtils.showSystemMessage(formatCommandList());
      }
    } catch (final IllegalArgumentException e) {
      chatUtils.showSystemMessage(e.getMessage());
    }
  }

  private void unbanCommand(
      final String commandLine,
      final ChatUtils chatUtils,
      final Map<InetAddress, Instant> bannedUsers) {
    try {
      final String[] parts = commandLine.split("\\s+", 2);
      if (2 > parts.length) {
        chatUtils.showSystemMessage(formatCommandList());
        return;
      }
      final String stringIpToUnban = parts[1].trim();
      final InetAddress ipToUnban = InetAddress.getByName(stringIpToUnban);
      if (null != bannedUsers.remove(ipToUnban)) {
        chatUtils.showSystemMessage("User " + stringIpToUnban + " has been unbanned.");
      } else {
        chatUtils.showSystemMessage("User " + stringIpToUnban + " is not banned.");
      }
    } catch (final UnknownHostException e) {
      chatUtils.showSystemMessage("Invalid IP address.");
    }
  }

  private void kickCommand(
      final String commandLine,
      final ChatUtils chatUtils,
      final List<Connection> clientConnections) {
    final String[] parts = commandLine.split("\\s+", 2);
    if (2 > parts.length) {
      chatUtils.showSystemMessage(formatCommandList());
      return;
    }
    final String userToKick = parts[1].trim();
    final Optional<Connection> userConnectionOpt =
        clientConnections.stream()
            .filter(conn -> conn.getName().equalsIgnoreCase(userToKick))
            .findFirst();

    if (userConnectionOpt.isPresent()) {
      final Connection userConnection = userConnectionOpt.get();
      final Message kickMessage = new KickMessage(userToKick);
      broadcastMessageToConnections(kickMessage);
      userConnection.removeConnection(clientConnections);
      chatUtils.showSystemMessage("User " + userToKick + " has been kicked.");
    } else {
      chatUtils.showSystemMessage("User " + userToKick + IS_NOT_CONNECTED);
    }
  }

  private void showUsersCommand(
      final ChatUtils chatUtils, final List<Connection> clientConnections) {
    if (clientConnections.isEmpty()) {
      chatUtils.showSystemMessage("No users are currently connected.");
    } else {
      final StringBuilder userList = new StringBuilder("Connected users:\n");
      for (final Connection connection : clientConnections) {
        userList.append("- ").append(connection.getName()).append("\n");
      }
      chatUtils.showSystemMessage(userList.toString().trim());
    }
  }

  private void showBansCommand(
      final ChatUtils chatUtils, final Map<InetAddress, Instant> bannedUsers) {
    if (bannedUsers.isEmpty()) {
      chatUtils.showSystemMessage("No users are currently banned.");
    } else {
      final StringBuilder banList = new StringBuilder("Banned users:\n");
      for (final Entry<InetAddress, Instant> entry : bannedUsers.entrySet()) {
        final String formattedTime = BanUtils.formatRemaining(entry.getValue());
        banList
            .append("- ")
            .append(entry.getKey().getHostAddress())
            .append(" | Banned until: ")
            .append(formattedTime)
            .append("\n");
      }
      chatUtils.showSystemMessage(banList.toString().trim());
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
      final CompletableFuture<Void> future =
          CompletableFuture.runAsync(
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

    executor.execute(
        () -> {
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
    connection
        .readStream(cryptographyManager)
        .map(
            transportMessage -> {
              try {
                return ConnectionMessageFactory.createFromString(transportMessage);
              } catch (final InvalidMessageException e) {
                LOGGER.debug(InvalidMessageException.DEFAULT_PROMPT, e);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .filter(new NonServerSideOnlyPredicate())
        .forEach(
            msg -> {
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
        .forEach(
            otherConnection -> {
              final CompletableFuture<Void> sendMessageFuture =
                  CompletableFuture.runAsync(
                      () -> otherConnection.writeOutput(message, cryptographyManager), executor);
              sendMessageFuture.exceptionally(
                  ex -> {
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

    final List<CompletableFuture<?>> closeFutures = new ArrayList<>();

    for (final Connection connection : clientConnections) {
      closeFutures.add(CompletableFuture.runAsync(connection::close));
    }

    closeServerSocket();
    chatUtils.close();
    CompletableFuture.allOf(closeFutures.toArray(new CompletableFuture[0])).join();
    executor.shutdownNow();
  }
}
