package org.gladiator.app.util.connection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Utility class for handling connection messages.
 */
public final class ConnectionMessageUtils {

  private ConnectionMessageUtils() {
  }

  /**
   * Converts a raw string to a {@link ConnectionMessage}.
   *
   * @param message The raw string message.
   * @return The {@link ConnectionMessage} object.
   * @throws IllegalArgumentException if the message is blank or does not match the expected
   *                                  pattern.
   */
  public static ConnectionMessage fromRawString(final String message) {
    Validate.notBlank(message);
    Validate.matchesPattern(message, "(.+),(.*)");
    final String[] split = StringUtils.split(message, ",", 2);
    return new ConnectionMessage(split[0], split[1]);
  }

  /**
   * Converts the sender name and content to a raw string format.
   *
   * @param senderName The name of the sender.
   * @param content    The content of the message.
   * @return The raw string representation of the message.
   */
  public static String toRawString(final String senderName, final String content) {
    return senderName + "," + content;
  }

}
