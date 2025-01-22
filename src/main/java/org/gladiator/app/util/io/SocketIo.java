package org.gladiator.app.util.io;

import java.io.BufferedReader;
import java.io.PrintWriter;

/**
 * A model to represent the reader and writer of the socket.
 *
 * @param reader The socket {@link java.io.InputStream} reader
 * @param writer The socket {@link java.io.OutputStream} writer
 */
public record SocketIo(BufferedReader reader, PrintWriter writer) {

  public BufferedReader getReader() {
    return reader;
  }

  public PrintWriter getWriter() {
    return writer;

  }
}