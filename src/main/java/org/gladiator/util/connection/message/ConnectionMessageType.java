package org.gladiator.util.connection.message;

/**
 * Enum representing the types of connection messages.
 */
public enum ConnectionMessageType {
  /**
   * Represents a simple message type.
   */
  SIMPLE(false),

  /**
   * Represents a new connection message type.
   */
  NEW_CONNECTION(true),

  /**
   * Represents a disconnection message type.
   */
  DISCONNECTION(true);

  private final boolean serverSentOnly;

  /**
   * Constructs a ConnectionMessageType.
   *
   * @param serverSentOnly Indicates if the message type is only sent by the server.
   */
  ConnectionMessageType(final boolean serverSentOnly) {
    this.serverSentOnly = serverSentOnly;
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