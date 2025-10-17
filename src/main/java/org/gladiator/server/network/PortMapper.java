package org.gladiator.server.network;

import com.sshtools.porter.UPnP.Discovery;
import com.sshtools.porter.UPnP.DiscoveryBuilder;
import com.sshtools.porter.UPnP.Gateway;
import com.sshtools.porter.UPnP.Protocol;
import jakarta.annotation.Nullable;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutorService;
import org.gladiator.util.thread.NamedVirtualThreadExecutorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing port mappings using UPnP.
 */
public final class PortMapper implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(PortMapper.class);
  private static final Protocol protocol = Protocol.TCP;
  private static final String DISCOVERY_PROCESS_INTERRUPTED = "UPnP discovery process interrupted";

  private final ExecutorService executor;
  private final int mappedPort;
  private final Discovery discovery;
  @Nullable
  private Gateway gateway;

  private PortMapper(
      final ExecutorService executor, final int mappedPort, final Discovery discovery) {
    this.executor = executor;
    this.mappedPort = mappedPort;
    this.discovery = discovery;
  }

  /**
   * Creates a default {@link PortMapper} instance with the specified port to map and starts the
   * UPnP discovery.
   *
   * @return A new {@link PortMapper} instance.
   */
  public static PortMapper createDefault(final int portToMap) {
    final ExecutorService executor = NamedVirtualThreadExecutorFactory.create("port_mapper");

    final Discovery discovery =
        new DiscoveryBuilder()
            .withSoTimeout(600)
            .withoutShutdownHooks()
            .onGateway(gw -> LOGGER.debug("Gateway found: {}", gw.ip().getHostAddress()))
            .build();

    return new PortMapper(executor, portToMap, discovery);
  }

  /**
   * Waits to find the gateway and pens the {@link PortMapper} port on it.
   */
  public void openPort() {
    executor.execute(
        () -> {
          try {
            setGateway(discovery.gateway().orElse(null));

            if (null == gateway) {
              LOGGER.debug("No UPnP gateway device found");
              return;
            }

            final boolean mapped = gateway.map(mappedPort, protocol);
            if (mapped) {
              LOGGER.debug("Port {} mapped with success", mappedPort);
            }

          } catch (final IllegalStateException | UncheckedIOException e) {
            LOGGER.debug(DISCOVERY_PROCESS_INTERRUPTED, e);
          }
        });
  }

  /**
   * Closes the port on the router for the specified port number using the configured gateway.
   */
  private void closePort() {
    if (null != gateway) {
      gateway.unmap(mappedPort, protocol);
      LOGGER.debug("Port {} unmapped with success", mappedPort);
    }
  }

  private void setGateway(@Nullable final Gateway gateway) {
    this.gateway = gateway;
  }

  /**
   * Closes the {@link PortMapper}, and it's port on the router.
   */
  @Override
  public void close() {
    closePort();
    discovery.close();
    executor.shutdownNow();
  }
}
