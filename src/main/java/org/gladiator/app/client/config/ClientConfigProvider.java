package org.gladiator.app.client.config;


import org.gladiator.app.util.ChatUtils;
import org.gladiator.environment.Port;

/**
 * Provides configuration for the client by interacting with the user to receive input.
 */
public final class ClientConfigProvider {

  private final ChatUtils chatUtils;

  /**
   * Constructs a new ClientConfigProvider with the specified ChatUtils instance.
   *
   * @param chatUtils the ChatUtils instance to use for user interaction
   */
  public ClientConfigProvider(final ChatUtils chatUtils) {
    this.chatUtils = chatUtils;
  }

  /**
   * Creates a new ClientConfig by interacting with the user to receive the necessary input.
   *
   * @return a new ClientConfig instance with the user's input
   */
  public ClientConfig createClientConfig() {

    final String clientName = receiveName();
    final String serverAddress = receiveAddress();
    final int serverPort = receivePort();

    return new ClientConfig(clientName, serverAddress, serverPort);
  }

  private String receiveName() {
    String name = "";
    while (name.isBlank()) {
      name = chatUtils.getUserInput("Choose your name: ");
    }
    return name;
  }

  private String receiveAddress() {
    return chatUtils.askUserOption("Server IP", "localhost");
  }

  private int receivePort() {
    return readPort(chatUtils);
  }

  private int readPort(final ChatUtils chatUtils) {
    int serverPort = 0;
    boolean isPortValid = false;

    while (!isPortValid) {
      final String serverPortString = chatUtils.askUserOption("server port",
          String.valueOf(Port.PORT_DEFAULT));
      if (serverPortString.isBlank()) {
        serverPort = Port.PORT_DEFAULT;
      } else {
        try {
          serverPort = Integer.parseInt(serverPortString);
        } catch (final NumberFormatException e) {
          chatUtils.displayOnScreen("Insert a valid numerical port");
        }
      }

      isPortValid = checkServerPortRange(serverPort);
    }

    return serverPort;
  }

  private boolean checkServerPortRange(final int serverPort) {
    if (Port.PORT_MIN < serverPort && Port.PORT_MAX > serverPort) {
      return true;
    }

    chatUtils.displayOnScreen(
        "Insert a number within the valid port range (" + Port.PORT_MIN + " - " + Port.PORT_MAX
            + ")");

    return false;

  }
}

