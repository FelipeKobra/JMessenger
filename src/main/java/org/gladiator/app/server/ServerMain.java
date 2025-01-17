package app.server;

import app.exception.EndApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for starting the server application.
 */
public final class ServerMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

  private ServerMain() {
  }

  /**
   * The main method to start the server.
   *
   * @param args Command line arguments.
   */
  public static void main(final String... args) {
    try (
        final Server server = Server.createServer()) {
      server.runServer();
    } catch (final EndApplicationException e) {
      LOGGER.debug("Server Ended", e);
    }
  }
}
