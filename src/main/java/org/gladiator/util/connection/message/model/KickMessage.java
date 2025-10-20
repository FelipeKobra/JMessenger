package org.gladiator.util.connection.message.model;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.gladiator.util.connection.message.ConnectionMessageType;

/**
 * Represents a kick message indicating a user was removed from the server.
 *
 * <p>This immutable record stores the username of the kicked user and implements the {@link
 * Message} interface so it can be converted to/from transport strings.
 *
 * @param kickedUser the username of the user who was kicked; must not be null or blank
 */
public record KickMessage(String kickedUser) implements Message {

  private static final ConnectionMessageType TYPE = ConnectionMessageType.KICK;
  private static final String KICK_MESSAGE = " have been kicked from the server.";

  /**
   * Parses a transport-level message string and constructs a {@link KickMessage}.
   *
   * <p>Expected format: {@code TYPE + MESSAGE_SPLITTER + username} where {@code TYPE} is {@link
   * ConnectionMessageType#KICK} and {@code MESSAGE_SPLITTER} separates the type from payload.
   *
   * @param message the transport string to parse; must not be null or blank and must match the
   *     expected format
   * @return a new {@code KickMessage} containing the extracted username
   * @throws IllegalArgumentException if {@code message} is null/blank or does not match the
   *     expected pattern
   */
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
