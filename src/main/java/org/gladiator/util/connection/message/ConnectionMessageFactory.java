package org.gladiator.util.connection.message;

import static org.gladiator.util.connection.message.Message.MESSAGE_SPLITTER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating {@link Message} instances from transport messages.
 */
public final class ConnectionMessageFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionMessageFactory.class);

  /**
   * Private constructor to prevent instantiation.
   */
  private ConnectionMessageFactory() {
  }

  /**
   * Creates a {@link Message} instance from the given transport message string.
   *
   * @param transportMessage The transport message string.
   * @return The created {@link Message} instance, or null if the message type is invalid.
   */
  public static Message createMessage(final String transportMessage) {
    Message message = null;
    try {
      final String messageTypeString = transportMessage.split(MESSAGE_SPLITTER, 2)[0];
      final ConnectionMessageType messageType = ConnectionMessageType.valueOf(messageTypeString);
      message = switch (messageType) {
        case SIMPLE -> SimpleMessage.fromTransportString(transportMessage);
        case NEW_CONNECTION -> NewConnectionMessage.fromTransportString(transportMessage);
      };
    } catch (final IllegalArgumentException e) {
      LOGGER.debug("Type of received message out of pattern: {}", transportMessage, e);
    }
    return message;
  }

}