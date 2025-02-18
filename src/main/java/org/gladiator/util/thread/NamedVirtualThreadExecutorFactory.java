package org.gladiator.util.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Factory that creates virtual thread executors with custom prefix names.
 */
public final class NamedVirtualThreadExecutorFactory {

  private NamedVirtualThreadExecutorFactory() {
  }

  /**
   * Creates virtual thread executors with custom names.
   *
   * @param threadPrefix prefix names of the threads
   * @return the {@link ExecutorService} that creates named virtual threads
   */
  public static ExecutorService create(final String threadPrefix) {
    final ThreadFactory factory = Thread.ofVirtual().name(threadPrefix + "-", 0).factory();
    return Executors.newThreadPerTaskExecutor(factory);
  }

}
