package org.gladiator.client.config;

import static org.gladiator.util.validation.InputValidator.USER_NAME_MAX_LENGTH;

import org.gladiator.environment.Port;
import org.gladiator.exception.EndApplicationException;
import org.gladiator.util.chat.ChatUtils;
import org.gladiator.util.validation.InputValidator;

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
   * @throws EndApplicationException if the user input is invalid
   */
  public ClientConfig createClientConfig() throws EndApplicationException {

    final String clientName = receiveName();
    final String serverAddress = receiveAddress();
    final int serverPort = receivePort();

    return new ClientConfig(clientName, serverAddress, serverPort);
  }

  /**
   * Receives the client's name from the user.
   *
   * @return the client's name
   * @throws EndApplicationException if the username is not valid
   */
  private String receiveName() throws EndApplicationException {
    String name;
    name = chatUtils.getUserInput(
        "Choose your name [max size: " + USER_NAME_MAX_LENGTH + "]: ");
    name = name.trim();

    if (InputValidator.isUserNameNotValid(name)) {
      chatUtils.displayOnScreen("The username is not valid");
      throw new EndApplicationException("User name not valid");
    }
    return name;
  }

  /**
   * Receives the server address from the user.
   *
   * @return the server address
   */
  private String receiveAddress() {
    return chatUtils.askUserOption("Server IP", "localhost");
  }

  /**
   * Receives the server port from the user.
   *
   * @return the server port
   */
  private int receivePort() {
    return readPort(chatUtils);
  }

  /**
   * Reads the server port from the user.
   *
   * @param chatUtils the ChatUtils instance to use for user interaction
   * @return the server port
   */
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

  /**
   * Checks if the server port is within the valid range.
   *
   * @param serverPort the server port to check
   * @return true if the server port is within the valid range, false otherwise
   */
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