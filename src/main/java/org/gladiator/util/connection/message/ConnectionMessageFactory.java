package org.gladiator.util.connection.message;

import org.gladiator.exception.InvalidMessageException;
import org.gladiator.util.connection.message.model.Message;

/** Factory class for creating {@link Message} instances from transport messages. */
public final class ConnectionMessageFactory {

  /** Private constructor to prevent instantiation. */
  private ConnectionMessageFactory() {}

  /**
   * Creates a {@link Message} instance from the given transport message string.
   *
   * @param transportMessage The transport message, it is the string that is sent and received via
   *     socket.
   * @return The created {@link Message} instance, or null if the message type is invalid.
   */
  public static Message createFromString(final String transportMessage)
      throws InvalidMessageException {
    try {
      final String messageTypeString = transportMessage.split(Message.MESSAGE_SPLITTER, 2)[0];
      final ConnectionMessageType messageType = ConnectionMessageType.valueOf(messageTypeString);
      return messageType.fromTransportString(transportMessage);
    } catch (final IllegalArgumentException e) {
      throw new InvalidMessageException(transportMessage, e);
    }
  }
}
