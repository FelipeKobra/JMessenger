package org.gladiator.util.connection.message.model;

import org.apache.commons.lang3.Validate;
import org.gladiator.util.connection.message.ConnectionMessageType;

public record KickMessage() implements Message {

  private static final ConnectionMessageType TYPE = ConnectionMessageType.KICK;
  private static final String KICK_MESSAGE = "You have been kicked from the server.";

  public static Message fromTransportString(final String message) {
    Validate.notBlank(message);
    Validate.matchesPattern(message, TYPE.toString());
    return new KickMessage();
  }

  @Override
  public ConnectionMessageType getType() {
    return TYPE;
  }

  @Override
  public String toTransportString() {
    return TYPE.toString();
  }

  @Override
  public String toString() {
    return KICK_MESSAGE;
  }
}
