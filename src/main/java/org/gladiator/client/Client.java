package org.gladiator.client;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import org.gladiator.client.config.ClientConfig;
import org.gladiator.client.config.ClientConfigProvider;
import org.gladiator.exception.EndApplicationException;
import org.gladiator.exception.FailedExchangeException;
import org.gladiator.exception.InvalidMessageException;
import org.gladiator.util.chat.ChatUtils;
import org.gladiator.util.connection.Connection;
import org.gladiator.util.connection.SocketIo;
import org.gladiator.util.connection.exchange.NameExchange;
import org.gladiator.util.connection.message.ConnectionMessageFactory;
import org.gladiator.util.connection.message.model.Message;
import org.gladiator.util.connection.message.model.SimpleMessage;
import org.gladiator.util.crypto.CryptographyManager;
import org.gladiator.util.thread.NamedVirtualThreadExecutorFactory;
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
  private final ExecutorService executor;
  private final ChatUtils chatUtils;
  private final CryptographyManager cryptographyManager;

  private Client(
      final ClientConfig config,
      final ExecutorService executor,
      final ChatUtils chatUtils,
      final CryptographyManager cryptographyManager) {
    this.config = config;
    this.executor = executor;
    this.chatUtils = chatUtils;
    this.cryptographyManager = cryptographyManager;
  }

  /**
   * Creates a new Client instance by initializing the necessary components and establishing a
   * connection to the server.
   *
   * @return a new Client instance
   * @throws EndApplicationException if an error occurs during client creation
   */
  public static Client createClient(final ChatUtils chatUtils) throws EndApplicationException {

    final Client client;
    try {
      final ClientConfig clientConfig = new ClientConfigProvider(chatUtils).createClientConfig();

      final CryptographyManager cryptographyManager = CryptographyManager.create();

      client =
          new Client(
              clientConfig,
              NamedVirtualThreadExecutorFactory.create("client"),
              chatUtils,
              cryptographyManager);
    } catch (final UserInterruptException | EndOfFileException e) {
      throw new EndApplicationException(e);
    }

    Objects.requireNonNull(client);
    return client;
  }

  /**
   * Handles exceptions by logging the message, displaying a user message, and throwing an
   * EndApplicationException.
   *
   * @param chatUtils the ChatUtils instance for user interaction
   * @param logMessage the message to log
   * @param userMessage the message to display to the user
   * @param exception the exception to handle
   * @throws EndApplicationException the wrapped exception
   */
  private static void handleException(
      final ChatUtils chatUtils,
      final String logMessage,
      final String userMessage,
      final Exception exception)
      throws EndApplicationException {
    LOGGER.debug(logMessage);
    chatUtils.displayOnScreen(userMessage);
    throw new EndApplicationException(exception);
  }

  private Socket createSocket(final ChatUtils chatUtils, final ClientConfig clientConfig)
      throws EndApplicationException {

    final String serverAddress = clientConfig.serverAddress();
    final int port = clientConfig.port();
    Socket clientSocket = null;

    try {
      clientSocket = SocketFactory.getDefault().createSocket(serverAddress, port);
    } catch (final UnknownHostException e) {
      handleException(
          chatUtils, "Server Address not found", "Server " + serverAddress + " not Found", e);
    } catch (final ConnectException e) {
      handleException(
          chatUtils,
          "Connection time out with server",
          "Timed out when trying to connect to server: " + serverAddress,
          e);
    } catch (final SocketException e) {
      handleException(
          chatUtils, "Invalid Server IP Address", "Server address not valid: " + serverAddress, e);
    } catch (final IOException e) {
      handleException(
          chatUtils,
          "Error connecting to server",
          "Error during connection with server: " + serverAddress,
          e);
    }

    Objects.requireNonNull(clientSocket);
    return clientSocket;
  }

  private Socket createSocket() throws EndApplicationException {
    return createSocket(chatUtils, config);
  }

  /**
   * Runs the client by establishing a connection, exchanging names with the server, and handling
   * message sending and receiving.
   *
   * @throws EndApplicationException if an error occurs during client execution
   */
  public void run() throws EndApplicationException {
    try {
      final Socket socket = createSocket();
      final Connection serverConnection = establishConnection(socket);
      startCommunication(serverConnection);
      serverConnection.close();
      reconnectPrompt();
    } catch (final IOException e) {
      throw new EndApplicationException("Error creating Socket IO" + e);
    } catch (final FailedExchangeException e) {
      throw new EndApplicationException(e);
    }
  }

  /**
   * Establishes a connection to the server while exchanges cryptographic keys and names.
   *
   * @param socket The socket connected to the server.
   * @return The {@link Connection} object representing the server connection.
   * @throws FailedExchangeException If an error occurs during the key exchange process.
   * @throws IOException If an I/O error occurs during the connection process.
   */
  private Connection establishConnection(final Socket socket)
      throws FailedExchangeException, IOException {

    final SocketIo socketIo = SocketIo.create(socket);

    final SecretKey ownAesKey = exchangeCryptographyKeys(socketIo);

    final String serverName =
        new NameExchange(ownAesKey, cryptographyManager, config.name(), executor)
            .exchange(socketIo);

    return handleNewConnection(socketIo, serverName, ownAesKey);
  }

  /**
   * Starts communication with the server by running message sending and receiving tasks
   * asynchronously.
   *
   * @param serverConnection The connection to the server.
   */
  private void startCommunication(final Connection serverConnection) {
    final CompletableFuture<Void> receiveMessagesFuture =
        CompletableFuture.runAsync(() -> receiveMessages(serverConnection), executor);
    final CompletableFuture<Void> sendMessagesFuture =
        CompletableFuture.runAsync(() -> sendMessages(serverConnection), executor);
    CompletableFuture.allOf(receiveMessagesFuture, sendMessagesFuture).join();
  }

  /**
   * Creates and handles a new connection to the server by displaying connection messages and
   * creating a {@link Connection} object.
   *
   * @param socketIo The socketIO conected to the server.
   * @param serverName The name of the server.
   * @param ownAesKey The AES key used for encryption.
   * @return The {@link Connection} object representing the server connection.
   */
  private Connection handleNewConnection(
      final SocketIo socketIo, final String serverName, final SecretKey ownAesKey) {
    chatUtils.displayBanner("Connection Established with " + serverName);
    chatUtils.displayOnScreen("Type `quit` to exit");
    return Connection.create(serverName, socketIo, ownAesKey);
  }

  /**
   * Exchanges cryptographic keys with the server. This involves receiving the server's RSA public
   * key and sending the client's AES key encrypted with the server's RSA public key.
   *
   * @param socketIo The socket connected to the server.
   * @return The AES key used for encryption.
   * @throws FailedExchangeException If an error occurs during the key exchange process.
   */
  private SecretKey exchangeCryptographyKeys(final SocketIo socketIo)
      throws FailedExchangeException {
    final PublicKey serverPublicKey = receiveRsaPublicKey(socketIo.getObjectReader());
    sendOwnEncryptedAesKey(serverPublicKey, socketIo.getWriter());
    return cryptographyManager.getAesKey();
  }

  /**
   * Receives the RSA public key from the server.
   *
   * <p>Note: The X509 encoded key specification is used because native images do not support the
   * serialization of PublicKey objects.
   *
   * @param objectReader the ObjectInput stream to read the RSA public key from
   * @return the received RSA public key
   * @throws FailedExchangeException if the key exchange fails
   */
  private PublicKey receiveRsaPublicKey(final ObjectInput objectReader)
      throws FailedExchangeException {
    final PublicKey otherEndPublicKey;
    try {
      final byte[] publicKeyBytes = (byte[]) objectReader.readObject();
      final KeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      otherEndPublicKey = keyFactory.generatePublic(publicKeySpec);
      final String logMessage = "Received RSA public key";
      LOGGER.debug(logMessage);
    } catch (final IOException e) {
      LOGGER.error("Error receiving RSA key");
      throw new FailedExchangeException(e);
    } catch (final ClassNotFoundException e) {
      LOGGER.error("Class of PublicKey not found during RSA key receiving");
      throw new FailedExchangeException(e);
    } catch (final NoSuchAlgorithmException e) {
      LOGGER.error("Algorithm not found during RSA public key receiving");
      throw new FailedExchangeException(e);
    } catch (final InvalidKeySpecException e) {
      LOGGER.error("Key Spec not found during RSA public key receiving");
      throw new FailedExchangeException(e);
    }
    return otherEndPublicKey;
  }

  /**
   * Sends the client's AES key encrypted with the server's RSA public key.
   *
   * @param otherEndPublicKey the server's RSA public key
   * @param printWriter the socket connected to the server
   * @throws UncheckedIOException if an I/O error occurs while sending the AES key
   */
  private void sendOwnEncryptedAesKey(
      final PublicKey otherEndPublicKey, final PrintWriter printWriter) {
    final SecretKey aesKey = cryptographyManager.getAesKey();
    final String encryptedAesKey = cryptographyManager.encryptRsa(otherEndPublicKey, aesKey);
    printWriter.println(encryptedAesKey);
    final String logMessage = "AES key sent";
    LOGGER.debug(logMessage);
  }

  /**
   * Sends messages to the server. Reads user input and sends it as messages to the server.
   * Terminates when the user types "quit".
   *
   * @param serverConnection The connection to the server.
   */
  private void sendMessages(final Connection serverConnection) {

    try {
      String line = chatUtils.getUserInput();

      while (null != line) {
        if ("quit".equals(line)) {
          break;
        }

        if (!line.isBlank()) {
          final Message msg = new SimpleMessage(config.name(), line);
          serverConnection.writeOutput(msg, cryptographyManager);
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
   * Receives messages from the server and displays them using ChatUtils.
   *
   * @param serverConnection The connection to the server.
   */
  private void receiveMessages(final Connection serverConnection) {
    try {
      serverConnection
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
          .forEach(chatUtils::showNewMessage);
    } catch (final UncheckedIOException e) {
      LOGGER.debug("The connection with the server has ended");
    } finally {
      executor.shutdownNow();
    }
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

  @Override
  public void close() {
    LOGGER.debug("Closing connection");
    executor.shutdownNow();
  }
}
