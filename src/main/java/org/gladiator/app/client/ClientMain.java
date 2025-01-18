package app.client;

import app.exception.EndApplicationException;
import app.util.ChatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point for the client application. This class is responsible for starting and
 * running the client.
 */
public final class ClientMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientMain.class);

  private ClientMain() {
  }

  /**
   * The main method that starts the client application.
   *
   * @param args command-line arguments
   */
  public static void main(final String... args) {
    final ChatUtils chatUtils = ChatUtils.create(">");
    
    while (true) {
      try (final Client client = Client.createClient(chatUtils)) {
        client.run();
      } catch (final EndApplicationException e) {
        LOGGER.debug("Client Ended", e);
        break;
      }
    }

    chatUtils.close();
  }
}

