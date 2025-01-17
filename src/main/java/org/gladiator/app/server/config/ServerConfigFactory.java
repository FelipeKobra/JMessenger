package app.server.config;

import app.util.ChatUtils;
import environment.Port;
import org.apache.commons.lang3.Validate;
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
    isCustom = isCustom.toUpperCase();

    return "Y".equals(isCustom) ? createCustom() : createDefault();
  }

  private ServerConfig createCustom() {
    String serverName = "";
    int serverPort;

    try {
      chatUtils.displayOnScreen("Leave the field blank for the default setting");

      serverName = getCustomConfig("Server Name", ServerConfig.getDefaultName());
      serverPort = Integer.parseInt(
          getCustomConfig("Server Port", String.valueOf(Port.PORT_DEFAULT)));
      Validate.inclusiveBetween(Port.PORT_MIN, Port.PORT_MAX, serverPort);

    } catch (final NumberFormatException e) {
      LOGGER.error("Number not recognized, using default port");
      serverPort = Port.PORT_DEFAULT;
    } catch (final IllegalArgumentException e) {
      LOGGER.error("Invalid port number provided. Using default port.");
      serverPort = Port.PORT_DEFAULT;
    }

    return new ServerConfig(serverName, serverPort);
  }

  private ServerConfig createDefault() {
    return new ServerConfig();
  }

  private String getCustomConfig(final String configName, final String defaultConfig) {
    String config = chatUtils.askUserOption(configName, defaultConfig);
    if (config.isBlank()) {
      config = defaultConfig;
    }
    return config;
  }
}
