package org.gladiator.exception;

/**
 * Exception thrown when a message is invalid.
 */
public class InvalidMessageException extends Exception {

  /**
   * Default prompt message for the exception.
   */
  public static final String DEFAULT_PROMPT = "Invalid Message: ";

  /**
   * Constructs a new InvalidMessageException with the specified detail message and cause.
   *
   * @param message The detail message.
   * @param cause   The cause of the exception.
   */
  public InvalidMessageException(final String message, final Throwable cause) {
    super(DEFAULT_PROMPT + message, cause);
  }
}