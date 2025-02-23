package org.gladiator.util.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
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
  private final BufferedReader reader;
  private final PrintWriter writer;
  private final Socket socket;
  private final SecretKey aesKey;

  /**
   * Constructs a new Connection.
   *
   * @param name   The name of the client.
   * @param socket The socket for the connection.
   * @param reader The BufferedReader for reading input.
   * @param writer The PrintWriter for writing output.
   * @throws NullPointerException     if any of the parameters are null.
   * @throws IllegalArgumentException if the name is blank.
   */
  private Connection(final String name, final BufferedReader reader, final PrintWriter writer,
      final Socket socket, final SecretKey aesKey) {
    Validate.notBlank(name);
    this.aesKey = aesKey;
    this.name = name;
    this.reader = reader;
    this.writer = Objects.requireNonNull(writer, "writer parameter on Connection must not be null");
    this.socket = Objects.requireNonNull(socket,
        "socket parameter on Connection must not be null");
  }

  /**
   * Creates a new Connection instance.
   *
   * @param name   The name of the client.
   * @param socket The socket for the connection.
   * @param aesKey The AES secret key for encryption and decryption.
   * @return A new Connection instance.
   * @throws IOException If an I/O error occurs when creating the reader or writer.
   */
  public static Connection create(final String name, final Socket socket,
      final SecretKey aesKey) throws IOException {
    return new Connection(name, IoUtils.createReader(socket), IoUtils.createWriter(socket),
        socket, aesKey);
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
  public Stream<String> readStream(final CryptographyManager cryptographyManager) {
    return reader.lines().map(msg -> cryptographyManager.decrypt(aesKey, msg));
  }

  /**
   * Writes a message to the output stream.
   *
   * @param message the message to write to the output stream
   */
  public void writeOutput(final Message message, final CryptographyManager cryptographyManager) {
    final String encryptedMessage = cryptographyManager.encrypt(aesKey,
        message.toTransportString());
    writer.println(encryptedMessage);
  }

  /**
   * Closes the connection, including the input and output streams and the socket.
   *
   * @see java.io.Closeable#close()
   */
  @Override
  public void close() {
    try {
      writer.close();
      reader.close();
      socket.close();
    } catch (final IOException e) {
      LOGGER.error("Error closing the connection: {}", e, e);
    }
  }
}