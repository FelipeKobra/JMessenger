package org.gladiator.util.connection.message.model;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.gladiator.util.connection.message.ConnectionMessageType;

public record KickMessage(String kickedUser) implements Message {

  private static final ConnectionMessageType TYPE = ConnectionMessageType.KICK;
  private static final String KICK_MESSAGE = " have been kicked from the server.";

  public static Message fromTransportString(final String message) {
    Validate.notBlank(message);
    Validate.matchesPattern(message, TYPE + MESSAGE_SPLITTER + "(.+)");
    final String[] split = StringUtils.split(message, MESSAGE_SPLITTER, 2);
    final String kickedUser = split[1];
    return new KickMessage(kickedUser);
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
  @Nonnull
  public String toString() {
    return kickedUser + KICK_MESSAGE;
  }
}
