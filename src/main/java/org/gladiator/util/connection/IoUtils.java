package org.gladiator.util.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for creating I/O streams in a default way. This class provides static methods to
 * create various types of I/O streams for a given socket. It should always be used to create I/O
 * streams to ensure consistency and proper configuration.
 */
public final class IoUtils {

  /**
   * Private constructor to prevent instantiation.
   */
  private IoUtils() {
  }

  /**
   * Creates a PrintWriter for the given socket.
   *
   * @param socket The socket for which the PrintWriter is created.
   * @throws IOException If an I/O error occurs when creating the PrintWriter.
   */
  public static PrintWriter createWriter(final Socket socket) throws IOException {
    return new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
  }

  /**
   * Creates a BufferedReader for the given socket.
   *
   * @param socket The socket for which the BufferedReader is created.
   * @return A BufferedReader for the socket.
   * @throws IOException If an I/O error occurs when creating the BufferedReader.
   */
  public static BufferedReader createReader(final Socket socket) throws IOException {
    return new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
  }

  /**
   * Creates an ObjectInput for the given socket.
   *
   * @param socket The socket for which the ObjectInput is created.
   * @return An ObjectInput for the socket.
   * @throws IOException If an I/O error occurs when creating the ObjectInput.
   */
  public static ObjectInput createObjectReader(final Socket socket) throws IOException {
    return new ObjectInputStream(socket.getInputStream());
  }

  /**
   * Creates an ObjectOutput for the given socket.
   *
   * @param socket The socket for which the ObjectOutput is created.
   * @return An ObjectOutput for the socket.
   * @throws IOException If an I/O error occurs when creating the ObjectOutput.
   */
  public static ObjectOutput createObjectWriter(final Socket socket) throws IOException {
    return new ObjectOutputStream(socket.getOutputStream());
  }
}