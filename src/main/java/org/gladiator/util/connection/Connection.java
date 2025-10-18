package org.gladiator.util.connection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.Validate;
import org.gladiator.server.Server;
import org.gladiator.util.connection.message.model.Message;
import org.gladiator.util.crypto.CryptographyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a connection to a client. This class handles the input and output streams for the
 * client connection.
 *
 * @see Server
 */
public final class Connection implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

  /**
   * The name of the entity this connection is connected to (e.g., server name if this is a client
   * connection).
   */
  private final String name;

  private final SocketIo socketIo;
  private final SecretKey aesKey;

  /**
   * Constructs a new Connection.
   *
   * @param name The name of the client.
   * @param socketIo The SocketIo for the connection.
   * @throws NullPointerException if any of the parameters are null.
   * @throws IllegalArgumentException if the name is blank.
   */
  private Connection(final String name, final SocketIo socketIo, final SecretKey aesKey) {
    Validate.notBlank(name);
    this.socketIo = socketIo;
    this.aesKey = aesKey;
    this.name = name;
  }

  /**
   * Creates a new Connection instance.
   *
   * @param name The name of the client.
   * @param socketIo The SocketIo for the connection.
   * @param aesKey The AES secret key for encryption and decryption.
   * @return A new Connection instance.
   */
  public static Connection create(
      final String name, final SocketIo socketIo, final SecretKey aesKey) {
    return new Connection(name, socketIo, aesKey);
  }

  /**
   * Removes this connection from the list of connections and closes it.
   *
   * @param connections The list of connections.
   * @see #close()
   */
  public void removeConnection(final List<Connection> connections) {
    connections.remove(this);
    this.close();
  }

  /**
   * Gets the name of the entity this connection is connected to.
   *
   * @return the name of the entity.
   */
  public String getName() {
    return name;
  }

  /**
   * Reads the input stream as a stream of lines.
   *
   * @return a Stream of lines from the input.
   */
  public Stream<String> readStream(final CryptographyManager cryptographyManager)
      throws UncheckedIOException {
    return socketIo.getReader().lines().map(msg -> cryptographyManager.decrypt(aesKey, msg));
  }

  /**
   * Writes a message to the output stream.
   *
   * @param message the message to write to the output stream
   */
  public void writeOutput(final Message message, final CryptographyManager cryptographyManager) {
    final String encryptedMessage =
        cryptographyManager.encrypt(aesKey, message.toTransportString());
    socketIo.println(encryptedMessage);
  }

  /**
   * Closes the connection, including the input and output streams and the socket.
   *
   * @see java.io.Closeable#close()
   */
  @Override
  public void close() {
    try {
      socketIo.close();
    } catch (final IOException e) {
      LOGGER.error("Error closing the connection: {}", e, e);
    }
  }
}
