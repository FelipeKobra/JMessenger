package org.gladiator.util.connection.message.model;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.gladiator.util.connection.message.ConnectionMessageType;

public record BanMessage(String formattedBanTime, String bannedUser, String banReason)
    implements Message {

  private static final ConnectionMessageType TYPE = ConnectionMessageType.BAN;

  public BanMessage {
    Validate.notBlank(formattedBanTime);
  }

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
