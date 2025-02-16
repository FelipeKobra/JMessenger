package org.gladiator.app.util;

/**
 * Utility class for validating user input.
 */
public final class InputValidator {

  /**
   * Maximum length for a user name.
   */
  public static final int USER_NAME_MAX_LENGTH = 40;

  /**
   * Private constructor to prevent instantiation.
   */
  private InputValidator() {
  }

  /**
   * Validates the given username.
   *
   * @param userName the username to validate
   * @return {@code true} if the username is not empty, not null, and does not exceed the maximum
   * length; {@code false} otherwise
   */
  public static boolean isUserNameNotValid(final String userName) {
    return "".equals(userName) || null == userName || USER_NAME_MAX_LENGTH < userName.length();
  }
}
