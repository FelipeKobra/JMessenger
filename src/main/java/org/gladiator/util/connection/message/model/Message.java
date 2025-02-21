package org.gladiator.util.connection.message.model;

import org.gladiator.util.connection.message.ConnectionMessageType;

/**
 * Interface representing a message.
 */
public interface Message {

  /**
   * The delimiter used to split message components.
   */
  String MESSAGE_SPLITTER = ";";

  /**
   * Gets the type of the message.
   *
   * @return The type of the message.
   */
  ConnectionMessageType getType();

  /**
   * Converts the message to a transport string. A transport string is a formatted string
   * representation of the message, used for transmission over a network, such as via a Socket.
   *
   * @return The transport string representation of the message.
   */
  String toTransportString();

  /**
   * Returns a string representation of the message.
   *
   * @return The string representation of the message.
   */
  @Override
  String toString();
}