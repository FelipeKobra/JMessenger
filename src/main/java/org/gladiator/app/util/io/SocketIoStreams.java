package app.util.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A record that holds the input and output streams for a socket.
 */
public record SocketIoStreams(InputStream socketInputStream, OutputStream socketOutputStream) {

}
