package org.gladiator.app.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.net.SocketFactory;
import org.gladiator.app.client.config.ClientConfig;
import org.gladiator.app.client.config.ClientConfigProvider;
import org.gladiator.app.exception.EndApplicationException;
import org.gladiator.app.util.ChatUtils;
import org.gladiator.app.util.NamedVirtualThreadExecutorFactory;
import org.gladiator.app.util.connection.ConnectionMessageUtils;
import org.gladiator.app.util.io.SocketIo;
import org.gladiator.app.util.io.SocketIoAsyncFactory;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Client class represents a client that connects to a server, exchanges messages, and handles
 * user interactions.
 */
public final class Client implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

  private final ClientConfig config;
  private final Socket socket;
  private final ExecutorService executor;
  private final ChatUtils chatUtils;

  private Client(final ClientConfig config, final Socket socket, final ExecutorService executor,
      final ChatUtils chatUtils) {
    this.config = config;
    this.socket = socket;
    this.executor = executor;
    this.chatUtils = chatUtils;
  }

  /**
   * Creates a new Client instance by initializing the necessary components and establishing a
   * connection to the server.
   *
   * @return a new Client instance
   * @throws EndApplicationException if an error occurs during client creation
   */
  public static Client createClient(final ChatUtils chatUtils)
      throws EndApplicationException {

    Client client = null;
    try {
      final ClientConfig clientConfig = new ClientConfigProvider(
          chatUtils).createClientConfig();
      final Socket socket = createSocket(chatUtils, clientConfig.serverAddress(),
          clientConfig.port());
      final ExecutorService executor = NamedVirtualThreadExecutorFactory.create("client");

      client = new Client(clientConfig, socket, executor, chatUtils);
    } catch (final UserInterruptException | EndOfFileException e) {
      handleException(ChatUtils.USER_INTERRUPT_MESSAGE, e);
    }

    return client;
  }

  /**
   * Creates a socket connection to the specified server address and port.
   *
   * @param chatUtils     the ChatUtils instance for user interaction
   * @param serverAddress the address of the server to connect to
   * @param port          the port number to connect to
   * @return a Socket connected to the server
   * @throws EndApplicationException if an error occurs during socket creation
   */
  private static Socket createSocket(final ChatUtils chatUtils,
      final String serverAddress, final int port)
      throws EndApplicationException {

    Socket clientSocket = null;

    try {
      clientSocket = SocketFactory.getDefault()
          .createSocket(serverAddress, port);
    } catch (final UnknownHostException e) {
      handleException(chatUtils, "Server Address not found",
          "Server " + serverAddress + " not Found", e);
    } catch (final ConnectException e) {
      handleException(chatUtils, "Connection time out with server",
          "Timed out when trying to connect to server: " + serverAddress, e);
    } catch (final IOException e) {
      handleException("Error connecting to server", e);
    }
    return clientSocket;
  }

  /**
   * Handles exceptions by logging the message, displaying a user message, and throwing an
   * EndApplicationException.
   *
   * @param chatUtils   the ChatUtils instance for user interaction
   * @param logMessage  the message to log
   * @param userMessage the message to display to the user
   * @param exception   the exception to handle
   * @throws EndApplicationException the wrapped exception
   */
  private static void handleException(final ChatUtils chatUtils, final String logMessage,
      final String userMessage,
      final Exception exception)
      throws EndApplicationException {
    LOGGER.debug(logMessage, exception);
    chatUtils.displayOnScreen(userMessage);
    throw new EndApplicationException(exception);
  }

  /**
   * Handles exceptions by logging the message and throwing an EndApplicationException.
   *
   * @param logMessage the message to log
   * @param exception  the exception to handle
   * @throws EndApplicationException the wrapped exception
   */
  private static void handleException(final String logMessage, final Exception exception)
      throws EndApplicationException {
    LOGGER.debug(logMessage, exception);
    throw new EndApplicationException(exception);
  }

  /**
   * Runs the client by establishing a connection, exchanging names with the server, and handling
   * message sending and receiving.
   *
   * @throws EndApplicationException if an error occurs during client execution
   */
  public void run() throws EndApplicationException {
    final SocketIo socketIo = SocketIoAsyncFactory.create(socket, executor);

    final BufferedReader reader = socketIo.getReader();
    final PrintWriter writer = socketIo.getWriter();

    final String serverName = exchangeNames(writer, reader);

    chatUtils.prettyPrint("Connection Established with " + serverName);

    chatUtils.displayOnScreen("Type `quit` to exit");

    final CompletableFuture<Void> receiveMessagesFuture = CompletableFuture.runAsync(
        () -> receiveMessages(reader), executor);

    final CompletableFuture<Void> sendMessagesFuture = CompletableFuture.runAsync(
        () -> sendMessages(writer), executor);

    CompletableFuture.allOf(receiveMessagesFuture, sendMessagesFuture).join();
    reconnectPrompt();
  }

  /**
   * Receives messages from the server and displays them to the user.
   *
   * @param reader the BufferedReader to read messages from the server
   */
  private void receiveMessages(final BufferedReader reader) {
    try {
      reader.lines()
          .map(ConnectionMessageUtils::fromRawString)
          .forEach(msg ->
              chatUtils.showNewMessage(msg.toString()));
    } catch (final UncheckedIOException e) {
      LOGGER.debug("The connection with the server has ended");
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * Sends messages from the user to the server.
   *
   * @param writer the PrintWriter to send messages to the server
   */
  private void sendMessages(final PrintWriter writer) {

    try {
      String line = chatUtils.getUserInput();

      while (null != line) {
        if ("quit".equals(line)) {
          break;
        }

        final String msg = ConnectionMessageUtils.toRawString(config.name(), line);
        writer.println(msg);
        line = chatUtils.getUserInput();
      }
    } catch (final EndOfFileException | UserInterruptException e) {
      LOGGER.debug(ChatUtils.USER_INTERRUPT_MESSAGE, e);
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * Exchanges names with the server by sending the client's name and receiving the server's name.
   *
   * @param writer the PrintWriter to send the client's name to the server
   * @param reader the BufferedReader to receive the server's name
   * @return the name of the server
   */
  private String exchangeNames(final PrintWriter writer, final BufferedReader reader) {

    final CompletableFuture<Void> sendNameFuture = CompletableFuture.runAsync(() -> {
      writer.println(config.name());
      final String logMessage = "Sent name (" + config.name() + ") to server.";
      LOGGER.debug(logMessage);
    }, executor);

    final CompletableFuture<String> receiveNameFuture = CompletableFuture.supplyAsync(() -> {
      String serverName = "";
      try {
        serverName = reader.readLine();
        final String logMessage = "Received name from server: " + serverName + ".";
        LOGGER.debug(logMessage);
      } catch (final IOException e) {
        LOGGER.error("Error receiving server name: {}", e, e);
      }

      return serverName;
    }, executor);

    CompletableFuture.allOf(sendNameFuture, receiveNameFuture).join();

    return receiveNameFuture.join();
  }

  /**
   * Prompts the user to reconnect to another server.
   *
   * @throws EndApplicationException if the user chooses not to reconnect or an error occurs
   */
  private void reconnectPrompt() throws EndApplicationException {
    try {
      final String choice = chatUtils.getUserInput("Connect to another server? y/N: ");
      if (!"Y".equalsIgnoreCase(choice)) {
        throw new EndApplicationException("User chose to not reconnect");
      }
    } catch (final UserInterruptException | EndOfFileException e) {
      throw new EndApplicationException(e);
    }
  }

  private void closeSocket() {
    try {
      socket.close();
    } catch (final IOException e) {
      LOGGER.error("Error closing the socket connection: {}", e, e);
    }
  }

  @Override
  public void close() {
    LOGGER.debug("Closing connection");
    executor.shutdownNow();
    closeSocket();
  }
}
