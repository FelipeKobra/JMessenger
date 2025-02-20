package org.gladiator.util.connection.message;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a message exchanged between connections. This class is immutable and uses the record
 * feature of Java.
 */
public record SimpleMessage(String senderName, String message) implements Message {

  private static final ConnectionMessageType TYPE = ConnectionMessageType.SIMPLE;

  /**
   * Constructs a new ConnectionMessage.
   *
   * @param senderName The name of the sender.
   * @param message    The message of the message.
   * @throws NullPointerException     if any of the parameters are null.
   * @throws IllegalArgumentException if the senderName is blank.
   */
  public SimpleMessage {
    Validate.notBlank(senderName);
    Objects.requireNonNull(message);
  }

  /**
   * Creates a SimpleMessage from a transport string.
   *
   * @param message The transport string.
   * @return The created SimpleMessage.
   * @throws NullPointerException     if the message is null.
   * @throws IllegalArgumentException if the message is blank or does not match the expected
   *                                  pattern.
   */
  static Message fromTransportString(final String message) {
    Validate.notBlank(message);
    Validate.matchesPattern(message, TYPE + MESSAGE_SPLITTER + "(.+)" + MESSAGE_SPLITTER + "(.*)");
    final String[] split = StringUtils.split(message, MESSAGE_SPLITTER, 3);
    final String senderName = split[1];
    final String messageContent =
        2 < split.length ? split[2] : StringUtils.defaultIfEmpty(null, "");
    return new SimpleMessage(senderName, messageContent);
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
    return TYPE + MESSAGE_SPLITTER + senderName + MESSAGE_SPLITTER + message;
  }

  /**
   * Returns a string representation of the message that is meant to be displayed on the screen.
   *
   * @return The string representation of the message.
   */
  @Override
  public String toString() {
    return senderName + ": " + message;
  }
}