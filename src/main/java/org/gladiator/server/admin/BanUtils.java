package org.gladiator.server.admin;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gladiator.server.Server;

/**
 * Utility class containing helpers for parsing and formatting ban durations and expiries.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Parse user-provided ban strings (numeric units like "1d2h30m", ISO local date-time, or
 *       permanent indicators such as blank, `perma`, `perm`, `permanent`) into an {@link Instant}.
 *   <li>Format remaining time until a ban expiry into a human readable string (e.g. "1 day 2
 *       hours").
 * </ul>
 *
 * <p>This class is a final utility class with only static methods and is not intended to be
 * instantiated.
 *
 * @see #parseBanInputToInstant(String)
 * @see #formatRemaining(Instant)
 */
public final class BanUtils {

  private static final int SECOND_TO_MINUTE_MULTI = 60;
  private static final int SECOND_TO_HOUR_MULTI = 3600;
  private static final int SECOND_TO_DAY_MULTI = 86400;
  private static final int DAYS_YEAR = 365;

  private BanUtils() {
  }

  private static boolean isPermanentIndicator(final String input) {
    return input.isBlank()
        || Server.PERMA.equals(input)
        || "perm".equals(input)
        || "permanent".equals(input);
  }

  private static boolean isIsoFormat(final String input) {
    try {
      LocalDateTime.parse(input);
      return true;
    } catch (final DateTimeParseException e) {
      return false;
    }
  }

  private static Instant parseNumerictoInstant(final String input) {
    final Pattern pattern = Pattern.compile("(\\d+)\\s*([smhdw])");
    final Matcher matcher = pattern.matcher(input);
    long seconds = 0;
    boolean found = false;
    while (matcher.find()) {
      found = true;
      final long value = Long.parseLong(matcher.group(1));
      final String unit = matcher.group(2);
      switch (unit) {
        case "s":
          seconds += value;
          break;
        case "m":
          seconds += value * SECOND_TO_MINUTE_MULTI;
          break;
        case "h":
          seconds += value * SECOND_TO_HOUR_MULTI;
          break;
        case "d":
          seconds += value * SECOND_TO_DAY_MULTI;
          break;
        case "w":
          seconds += value * 7 * SECOND_TO_DAY_MULTI;
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid time unit: "
                  + unit
                  + ". Allowed units: s (seconds), m (minutes), h (hours), d (days), w (weeks).");
      }
    }

    if (!found) {
      throw new IllegalArgumentException(
          "Invalid format. Use for example: 30m, 1h30m, 1d, perma, or ISO like 2025-12-01T10:00");
    }

    return Instant.now().plusSeconds(seconds);
  }

  /**
   * Parses ban duration or expiry input into an Instant.
   *
   * <p>Supported input forms:
   *
   * <ul>
   *   <li>Numeric values with suffixes: `30s`, `15m`, `2h`, `1d`, `2w`
   *   <li>Combined units: `1d2h30m`
   *   <li>Permanent indicators: blank string, `perma`, `perm`, `permanent` (returns `null`)
   *   <li>ISO local date-time: `2025-12-01T10:00`
   * </ul>
   *
   * @param input user-provided ban string
   * @return expiry Instant
   * @throws IllegalArgumentException if the input cannot be parsed as any supported format
   */
  public static Instant parseBanInputToInstant(final String input) {
    final String formattedInput = input.trim().toLowerCase();

    if (isPermanentIndicator(formattedInput)) {
      return Instant.now().plus(100 * (long) DAYS_YEAR, ChronoUnit.DAYS);
    }

    if (isIsoFormat(formattedInput)) {
      final LocalDateTime ldt =
          LocalDateTime.parse(formattedInput); // expects e.g. 2025-12-01T10:00
      return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }

    return parseNumerictoInstant(formattedInput);
  }

  /**
   * Formats remaining time in a readable way.
   *
   * <p>Examples: "1 day 2 hours 5 minutes", "Permanent", "Ban expired".
   *
   * @param until expiry {@code Instant}
   * @return human-readable remaining time string
   */
  public static String formatRemaining(final Instant until) {

    final Instant now = Instant.now();
    if (now.isAfter(until)) {
      return "Ban expired";
    }

    Duration duration = Duration.between(now, until);
    final long days = duration.toDays();
    duration = duration.minusDays(days);
    final long hours = duration.toHours();
    duration = duration.minusHours(hours);
    final long minutes = duration.toMinutes();
    duration = duration.minusMinutes(minutes);
    final long seconds = duration.getSeconds();

    final StringBuilder sb = new StringBuilder();
    if (0 < days) {
      sb.append(days).append(1 == days ? " day " : " days ");
    }
    if (0 < hours) {
      sb.append(hours).append(1 == hours ? " hour " : " hours ");
    }
    if (0 < minutes) {
      sb.append(minutes).append(1 == minutes ? " minute " : " minutes ");
    }
    if (sb.isEmpty()) {
      sb.append(seconds).append(1 == seconds ? " second" : " seconds");
    }
    return sb.toString().trim();
  }
}
