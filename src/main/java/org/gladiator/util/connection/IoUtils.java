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

public final class IoUtils {

  private IoUtils() {
  }

  public static PrintWriter createWriter(final Socket socket) throws IOException {
    return new PrintWriter(socket.getOutputStream(), true,
        StandardCharsets.UTF_8);
  }

  public static BufferedReader createReader(final Socket socket) throws IOException {
    return new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
  }

  public static ObjectInput createObjectReader(final Socket socket) throws IOException {
    return new ObjectInputStream(socket.getInputStream());
  }

  public static ObjectOutput createObjectWriter(final Socket socket) throws IOException {
    return new ObjectOutputStream(socket.getOutputStream());
  }


}
