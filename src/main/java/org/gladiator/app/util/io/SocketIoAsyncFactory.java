package app.util.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating asynchronous SocketIo instances.
 */
public final class SocketIoAsyncFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      SocketIoAsyncFactory.class.getName());

  private SocketIoAsyncFactory() {
  }

  /**
   * Creates a new {@link SocketIo} instance using the provided client socket and executor.
   *
   * @param clientSocket The client socket.
   * @param executor     The executor to use for asynchronous operations.
   * @return A new {@link SocketIo} instance.
   */
  public static SocketIo create(final Socket clientSocket, final Executor executor) {
    final SocketIoStreams socketIoStreams = createSocketIoStreams(clientSocket, executor);

    final BufferedReader reader = new BufferedReader(
        new InputStreamReader(socketIoStreams.socketInputStream(), StandardCharsets.UTF_8));
    final PrintWriter writer = new PrintWriter(socketIoStreams.socketOutputStream(), true,
        StandardCharsets.UTF_8);
    return new SocketIo(reader, writer);
  }

  /**
   * Creates a new {@link SocketIoStreams} instance using the provided client socket and executor.
   *
   * @param clientSocket The client socket.
   * @param executor     The executor to use for asynchronous operations.
   * @return A new {@link SocketIoStreams} instance.
   */
  private static SocketIoStreams createSocketIoStreams(final Socket clientSocket,
      final Executor executor) {
    final CompletableFuture<InputStream> clientInputStreamFuture = CompletableFuture.supplyAsync(
        () -> createInputStream(clientSocket), executor);

    final CompletableFuture<OutputStream> clientOutputStreamFuture = CompletableFuture.supplyAsync(
        () -> createOutputStream(clientSocket), executor);

    CompletableFuture.allOf(clientInputStreamFuture, clientOutputStreamFuture).join();

    final InputStream clientInputStream = clientInputStreamFuture.join();
    final OutputStream clientOutputStream = clientOutputStreamFuture.join();

    return new SocketIoStreams(clientInputStream, clientOutputStream);
  }

  /**
   * Creates an input stream from the provided client socket.
   *
   * @param clientSocket The client socket.
   * @return The input stream.
   */
  private static InputStream createInputStream(final Socket clientSocket) {
    InputStream clientInputStream = null;
    try {
      clientInputStream = clientSocket.getInputStream();
    } catch (final IOException e) {
      LOGGER.debug("Input Stream closed: {}", String.valueOf(e));
    }
    return clientInputStream;
  }

  /**
   * Creates an output stream from the provided client socket.
   *
   * @param clientSocket The client socket.
   * @return The output stream.
   */
  private static OutputStream createOutputStream(final Socket clientSocket) {
    OutputStream clientOutputStream = null;
    try {
      clientOutputStream = clientSocket.getOutputStream();
    } catch (final IOException e) {
      LOGGER.debug("Output Stream closed: {}", String.valueOf(e));
    }
    return clientOutputStream;
  }

}


