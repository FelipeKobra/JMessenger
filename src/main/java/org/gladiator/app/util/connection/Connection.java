package org.gladiator.app.util.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.gladiator.app.server.Server;
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

  private final String name;
  private final Socket socket;
  private final BufferedReader input;
  private final PrintWriter output;


  /**
   * Constructs a new Connection.
   *
   * @param name   The name of the client.
   * @param socket The socket for the connection.
   * @param input  The input stream for the connection.
   * @param output The output stream for the connection.
   * @throws NullPointerException     if any of the parameters are null.
   * @throws IllegalArgumentException if the name is blank.
   */
  public Connection(final String name, final Socket socket, final BufferedReader input,
      final PrintWriter output) {
    Validate.notBlank(name);
    Objects.requireNonNull(socket);
    Objects.requireNonNull(input);
    Objects.requireNonNull(output);
    this.name = name;
    this.socket = socket;
    this.input = input;
    this.output = output;
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

  public String getName() {
    return name;
  }

  public PrintWriter getOutput() {
    return output;
  }

  /**
   * Closes the connection, including the input and output streams and the socket.
   *
   * @see java.io.Closeable#close()
   */
  @Override
  public void close() {
    try {
      output.close();
      input.close();
      socket.close();
    } catch (final IOException e) {
      LOGGER.error("Error closing the connection: {}", e, e);
    }
  }
}



