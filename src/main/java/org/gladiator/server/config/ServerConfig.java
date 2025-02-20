package org.gladiator.server.config;

import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.gladiator.environment.Port;

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

  /**
   * Gets the default name for the server.
   *
   * @return the default server name
   */
  public static String getDefaultName() {
    return DEFAULT_NAME;
  }

  /**
   * Validates the server name and port.
   *
   * @param name the name of the server
   * @param port the port number of the server
   * @throws NullPointerException     if the name is null
   * @throws IllegalArgumentException if the name is blank or the port is out of range
   */
  private void validateArgs(final String name, final int port) {
    Objects.requireNonNull(name);
    Validate.notBlank(name);
    Validate.inclusiveBetween(Port.PORT_MIN, Port.PORT_MAX, port);
  }
}