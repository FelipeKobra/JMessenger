package org.gladiator.environment;

/**
 * Utility class for port-related constants.
 */
public final class Port {

  /**
   * The default port number.
   */
  public static final int PORT_DEFAULT = 443;
  /**
   * The minimum port number.
   */
  public static final int PORT_MIN = 0;
  /**
   * The maximum port number.
   */
  public static final int PORT_MAX = 65_535;

  private Port() {
  }
}
