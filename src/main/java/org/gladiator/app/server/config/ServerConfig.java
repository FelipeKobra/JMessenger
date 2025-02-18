package org.gladiator.app.server.config;


import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.gladiator.app.environment.Port;


/**
 * Represents the configuration for a server, including its name and port.
 */
public record ServerConfig(String name, int port) {

  private static final String DEFAULT_NAME = "Server";

  /**
   * Constructs a new ServerConfig with the specified name and port.
   *
   * @param name the name of the server
   * @param port the port number of the server
   */
  public ServerConfig {
    validateArgs(name, port);
  }

  /**
   * Constructs a new ServerConfig with default name and port.
   */
  public ServerConfig() {
    this(getDefaultName(), Port.PORT_DEFAULT);
  }


  public static String getDefaultName() {
    return DEFAULT_NAME;
  }


  private void validateArgs(final String name, final int port) {
    Objects.requireNonNull(name);
    Validate.notBlank(name);
    Validate.inclusiveBetween(Port.PORT_MIN, Port.PORT_MAX, port);
  }


}
