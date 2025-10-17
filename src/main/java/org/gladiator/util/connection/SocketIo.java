package org.gladiator.util.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Aggregates and manages all readers and writers for a connected socket.
 *
 * <p>This class centralizes creation and lifecycle management of the socket's character and object
 * streams (a {@link BufferedReader}, a {@link PrintWriter}, an {@link ObjectOutputStream} and an
 * {@link ObjectInputStream}). By wrapping those streams in a single object and implementing {@link
 * AutoCloseable}, the caller can create and close all associated input/output resources in one
 * place. Closing this {@code SocketIo} will also close the underlying socket streams, preventing
 * resource leaks and ensuring consistent shutdown semantics.
 */
public final class SocketIo implements AutoCloseable {

  private final BufferedReader reader;
  private final PrintWriter writer;
  private final ObjectOutputStream objectWriter;
  private final ObjectInput objectReader;

  private SocketIo(
      final BufferedReader reader,
      final PrintWriter writer,
      final ObjectOutputStream objectWriter,
      final ObjectInput objectReader) {
    this.reader = reader;
    this.writer = writer;
    this.objectWriter = objectWriter;
    this.objectReader = objectReader;
  }

  /**
   * Creates a new {@code SocketIo} instance wrapping the provided socket's input and output
   * streams.
   *
   * <p>Initializes:
   *
   * <ul>
   *   <li>a {@link BufferedReader} for character input using UTF-8,
   *   <li>a {@link PrintWriter} for character output using UTF-8 and auto-flush enabled,
   *   <li>an {@link ObjectOutputStream} for serializing objects to the socket, and
   *   <li>an {@link ObjectInputStream} for deserializing objects from the socket.
   * </ul>
   *
   * <p>Note: creation of object streams can block if the remote peer does not create its
   * corresponding object streams in a compatible order.
   *
   * @param socket the connected {@link Socket} to use for IO
   * @return a new {@link SocketIo} wrapping the socket's streams
   * @throws IOException if an I/O error occurs while obtaining or creating streams
   */
  public static SocketIo create(final Socket socket) throws IOException {
    return new SocketIo(
        new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)),
        new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8),
        new ObjectOutputStream(socket.getOutputStream()),
        new ObjectInputStream(socket.getInputStream()));
  }

  public BufferedReader getReader() {
    return reader;
  }

  public PrintWriter getWriter() {
    return writer;
  }

  public ObjectOutputStream getObjectWriter() {
    return objectWriter;
  }

  public ObjectInput getObjectReader() {
    return objectReader;
  }

  @Override
  public void close() throws IOException {
    reader.close();
    writer.close();
    objectWriter.close();
    objectReader.close();
  }
}
