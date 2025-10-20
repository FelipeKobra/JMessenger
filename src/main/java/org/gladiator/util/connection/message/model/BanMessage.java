package org.gladiator.util.connection.message.model;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.gladiator.util.connection.message.ConnectionMessageType;

/**
 * Represents a ban message exchanged over the connection.
 *
 * <p>Contains the human-readable ban time, the username of the banned user and an optional
 * reason. Instances are validated to ensure {@code formattedBanTime} is not blank.
 *
 * @param formattedBanTime human-readable ban duration or timestamp (must not be blank)
 * @param bannedUser       username of the banned user
 * @param banReason        optional reason for the ban; may be empty
 * @implSpec This record implements {@code Message} and corresponds to
 *     {@link ConnectionMessageType#BAN}
 */
public record BanMessage(String formattedBanTime, String bannedUser, String banReason)
    implements Message {

  private static final ConnectionMessageType TYPE = ConnectionMessageType.BAN;

  /**
   * Compact canonical constructor for the record.
   *
   * <p>Enforces the record invariant that {@code formattedBanTime} is not blank.
   *
   * @param formattedBanTime human-readable ban duration or timestamp; must not be blank
   * @param bannedUser       username of the banned user
   * @param banReason        optional reason for the ban; may be empty
   * @throws IllegalArgumentException if {@code formattedBanTime} is blank
   */
  public BanMessage {
    Validate.notBlank(formattedBanTime);
  }

  /**
   * Parses a transport-level message string and constructs a {@link BanMessage}.
   *
   * <p>Expected transport format:
   * <pre>
   * TYPE + MESSAGE_SPLITTER + bannedUser + MESSAGE_SPLITTER + formattedBanTime + MESSAGE_SPLITTER + banReason
   * </pre>
   * <p>
   * The {@code banReason} field may be empty. This method validates that the {@code message} is not
   * blank and matches the expected BAN message pattern before parsing.
   *
   * @param message the transport string to parse
   * @return a new {@link BanMessage} instance representing the parsed values
   * @throws IllegalArgumentException if {@code message} is blank or does not match the expected BAN
   *                                  message format
   */
  public static Message fromTransportString(final String message) {
    Validate.notBlank(message);
    Validate.matchesPattern(
        message,
        TYPE + MESSAGE_SPLITTER + "(.+)" + MESSAGE_SPLITTER + "(.*)" + MESSAGE_SPLITTER + "(.*)");
    final String[] split = StringUtils.split(message, MESSAGE_SPLITTER, 4);
    final String bannedUser = split[1];
    final String userBanTime = split[2];
    final String userBanReason = 3 < split.length ? split[3] : "";
    return new BanMessage(userBanTime, bannedUser, userBanReason);
  }

  @Override
  public ConnectionMessageType getType() {
    return TYPE;
  }

  @Override
  public String toTransportString() {
    return TYPE
        + MESSAGE_SPLITTER
        + bannedUser
        + MESSAGE_SPLITTER
        + formattedBanTime
        + MESSAGE_SPLITTER
        + banReason;
  }

  @Override
  @Nonnull
  public String toString() {
    final String banMessage = bannedUser + " have been banned for " + formattedBanTime + ".";
    if (banReason.isBlank()) {
      return banMessage;
    }
    return banMessage + " Reason: " + banReason;
  }
}
