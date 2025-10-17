package org.gladiator.server.config;

import static org.gladiator.environment.UserInputConfig.USER_NAME_MAX_LENGTH;

import org.apache.commons.lang3.Validate;
import org.gladiator.environment.Port;
import org.gladiator.util.chat.ChatUtils;
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
    final boolean isCustom = chatUtils.askUserBinaryOption("Change the default settings?", false);

    return isCustom ? createCustom() : createDefault();
  }

  /**
   * Gets a custom server name from the user.
   *
   * @return the custom server name
   */
  private String getCustomName() {

    final String serverDefaultName = ServerConfig.getDefaultName();

    return chatUtils.askUserOption("Server Name", serverDefaultName, USER_NAME_MAX_LENGTH);
  }

  private boolean getEnableUpnp() {
    return chatUtils.askUserBinaryOption(
        "Open gateway port automatically via UPnP service?", false);
  }

  /**
   * Gets a custom server port from the user.
   *
   * @return the custom server port
   */
  private int getCustomPort() {
    int serverPort;
    try {
      serverPort =
          Integer.parseInt(
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
    final boolean enableUpnp = getEnableUpnp();

    return new ServerConfig(serverName, serverPort, enableUpnp);
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
