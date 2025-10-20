package org.gladiator.util.connection.message;

import java.util.function.Function;
import org.gladiator.util.connection.message.model.BanMessage;
import org.gladiator.util.connection.message.model.DisconnectMessage;
import org.gladiator.util.connection.message.model.KickMessage;
import org.gladiator.util.connection.message.model.Message;
import org.gladiator.util.connection.message.model.NewConnectionMessage;
import org.gladiator.util.connection.message.model.SimpleMessage;

/** Enum representing the types of connection messages. */
public enum ConnectionMessageType {
  /** Represents a simple message type. */
  SIMPLE(false, SimpleMessage::fromTransportString),

  /** Represents a new connection message type. */
  NEW_CONNECTION(true, NewConnectionMessage::fromTransportString),

  /** Represents a disconnection message type. */
  DISCONNECTION(true, DisconnectMessage::fromTransportString),

  /** Represents a kick user message type. */
  KICK(true, KickMessage::fromTransportString),

  /** Represents a ban user message type. */
  BAN(true, BanMessage::fromTransportString);

  private final boolean serverSentOnly;
  private final Function<String, Message> messageFunction;

  /**
   * Constructs a ConnectionMessageType.
   *
   * @param serverSentOnly Indicates if the message type is only sent by the server.
   */
  ConnectionMessageType(
      final boolean serverSentOnly, final Function<String, Message> stringToMessageFunction) {
    this.serverSentOnly = serverSentOnly;
    this.messageFunction = stringToMessageFunction;
  }

  public Message fromTransportString(final String message) {
    return messageFunction.apply(message);
  }

  /**
   * Checks if the message type is only sent by the server.
   *
   * @return true if the message type is only sent by the server, false otherwise.
   */
  public boolean isServerSentOnly() {
    return serverSentOnly;
  }
}
