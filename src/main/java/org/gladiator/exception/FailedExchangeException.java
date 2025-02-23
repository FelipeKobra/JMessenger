package org.gladiator.exception;

/**
 * Exception thrown when an exchange operation fails. It is main used to not end the application in
 * the case of an error during an exchange on {@link org.gladiator.server.Server} and to treat them
 * better in {@link org.gladiator.client.Client}
 */
public class FailedExchangeException extends Exception {
  
  public FailedExchangeException(final Throwable cause) {
    super(cause);
  }
}