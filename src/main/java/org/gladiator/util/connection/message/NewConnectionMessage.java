package org.gladiator.util.connection.message;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a message exchanged between connections. This class is immutable and uses the record
 * feature of Java.
 */
public record NewConnectionMessage(String newConnectionUserName) implements Message {

  private static final ConnectionMessageType TYPE = ConnectionMessageType.NEW_CONNECTION;

  /**
   * Constructs a new ConnectionMessage.
   *
   * @param newConnectionUserName The name of the sender.
   * @throws NullPointerException     if any of the parameters are null.
   * @throws IllegalArgumentException if the senderName is blank.
   */
  public NewConnectionMessage {
    Validate.notBlank(newConnectionUserName);
  }

  /**
   * Creates a NewConnectionMessage from a transport string.
   *
   * @param message The transport string.
   * @return The created NewConnectionMessage.
   * @throws NullPointerException     if the message is null.
   * @throws IllegalArgumentException if the message is blank or does not match the expected
   *                                  pattern.
   */
  public static Message fromTransportString(final String message) {
    Validate.notBlank(message);
    Validate.matchesPattern(message, TYPE + MESSAGE_SPLITTER + "(.+)");
    final String[] split = StringUtils.split(message, MESSAGE_SPLITTER, 2);
    final String newConnectionUserName = split[1];
    return new NewConnectionMessage(newConnectionUserName);
  }

  @Override
  public ConnectionMessageType getType() {
    return TYPE;
  }

  /**
   * Converts the message to a raw string format. This format is necessary during the transmission
   * of the message, so it is easier to split the sender name to its message.
   *
   * @return The raw string representation of the message.
   */
  @Override
  public String toTransportString() {
    return TYPE + MESSAGE_SPLITTER + newConnectionUserName;
  }

  /**
   * Returns a string representation of the message that is meant to be displayed on the screen.
   *
   * @return The string representation of the message.
   */
  @Override
  public String toString() {
    return "User " + newConnectionUserName + " connected";
  }
}