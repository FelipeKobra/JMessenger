package org.gladiator.util.connection.message;

import java.util.function.Predicate;
import org.gladiator.util.connection.message.model.Message;

/**
 * Predicate implementation that tests if a message is not server-side only.
 */
public class NonServerSideOnlyPredicate implements Predicate<Message> {

  /**
   * Tests if the given message is not server-side only.
   *
   * @param msg The message to be tested.
   * @return true if the message is not server-side only, false otherwise.
   */
  @Override
  public boolean test(final Message msg) {
    return !msg.getType().isServerSentOnly();
  }
}