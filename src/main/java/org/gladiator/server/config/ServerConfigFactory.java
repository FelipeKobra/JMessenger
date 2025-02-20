package org.gladiator.server.config;

import static org.gladiator.util.validation.InputValidator.USER_NAME_MAX_LENGTH;

import java.util.Locale;
import org.apache.commons.lang3.Validate;
import org.gladiator.environment.Port;
import org.gladiator.util.chat.ChatUtils;
import org.gladiator.util.validation.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating {@link ServerConfig} instances.
 */
public class ServerConfigFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfigFactory.class);

  private final ChatUtils chatUtils;

  /**
   * Constructs a new {@link ServerConfigFactory} with the specified {@link ChatUtils}.
   *
   * @param chatUtils the {@link ChatUtils} instance for user interaction
   */
  public ServerConfigFactory(final ChatUtils chatUtils) {
    this.chatUtils = chatUtils;
  }

  /**
   * Creates a new {@link ServerConfig} instance based on user input.
   *
   * @return a {@link ServerConfig} instance
   */
  public ServerConfig create() {
    String isCustom;

    isCustom = chatUtils.getUserInput("Want to change the default settings? y/N: ");
    isCustom = isCustom.toUpperCase(Locale.ROOT);

    return "Y".equals(isCustom) ? createCustom() : createDefault();
  }

  /**
   * Gets a custom server name from the user.
   *
   * @return the custom server name
   */
  private String getCustomName() {
    String serverName;

    final String serverDefaultName = ServerConfig.getDefaultName();
    serverName = chatUtils.askUserOption("Server Name",
        serverDefaultName, USER_NAME_MAX_LENGTH);
    serverName = serverName.trim();

    if (InputValidator.isUserNameNotValid(serverName)) {
      LOGGER.error("Server name has more than " + USER_NAME_MAX_LENGTH
          + " characters, using default name");
      serverName = serverDefaultName;
    }

    return serverName;
  }

  /**
   * Gets a custom server port from the user.
   *
   * @return the custom server port
   */
  private int getCustomPort() {
    int serverPort;
    try {
      serverPort = Integer.parseInt(
          chatUtils.askUserOption("Server Port", String.valueOf(Port.PORT_DEFAULT)));
      Validate.inclusiveBetween(Port.PORT_MIN, Port.PORT_MAX, serverPort);

    } catch (final NumberFormatException e) {
      LOGGER.error("Number not recognized, using default port");
      serverPort = Port.PORT_DEFAULT;
    } catch (final IllegalArgumentException e) {
      LOGGER.error("Invalid port number provided. Using default port.");
      serverPort = Port.PORT_DEFAULT;
    }

    return serverPort;
  }

  /**
   * Creates a custom {@link ServerConfig} instance based on user input.
   *
   * @return a custom {@link ServerConfig} instance
   */
  private ServerConfig createCustom() {

    final String serverName = getCustomName();
    final int serverPort = getCustomPort();

    return new ServerConfig(serverName, serverPort);
  }

  /**
   * Creates a default {@link ServerConfig} instance.
   *
   * @return a default {@link ServerConfig} instance
   */
  private ServerConfig createDefault() {
    return new ServerConfig();
  }
}