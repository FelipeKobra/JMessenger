package org.gladiator.app.exception;

/**
 * Exception thrown to indicate that the application should end.
 */
public class EndApplicationException extends Exception {

  /**
   * Constructs a new EndApplicationException with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public EndApplicationException(final Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new EndApplicationException with the specified message.
   *
   * @param message the detail message
   */
  public EndApplicationException(final String message) {
    super(message);
  }
}
