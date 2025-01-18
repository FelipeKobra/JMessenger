package app.util;

import com.sshtools.porter.UPnP.Discovery;
import com.sshtools.porter.UPnP.DiscoveryBuilder;
import com.sshtools.porter.UPnP.Gateway;
import com.sshtools.porter.UPnP.Protocol;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private final Discovery discovery;
  private Gateway gateway;

  private PortMapper(final ExecutorService executor, final Discovery discovery) {
    this.executor = executor;
    this.discovery = discovery;
  }

  /**
   * Creates a default {@link PortMapper} instance with a virtual thread executor and a discovery
   * instance.
   *
   * @return A new {@link PortMapper} instance.
   */
  public static PortMapper createDefault() {
    final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    final Discovery discovery = new DiscoveryBuilder().withSoTimeout(600)
        .withoutShutdownHooks()
        .onGateway(gw -> LOGGER.debug("Gateway found {}", gw.ip()))
        .build();

    final PortMapper portMapper = new PortMapper(executor, discovery);

    executor.execute(() -> {
      try {
        portMapper.setGateway(discovery.gateway().orElse(null));
      } catch (final IllegalStateException e) {
        LOGGER.debug(DISCOVERY_PROCESS_INTERRUPTED, e);
      }
    });

    return portMapper;
  }

  /**
   * Opens a port on the router for the specified port number using the configured gateway.
   *
   * @param port The port number to open.
   */
  public void openPort(final int port) {
    executor.execute(() -> {
      try {
        if (null == gateway) {
          discovery.awaitCompletion();
        }
        if (null != gateway) {
          final boolean mapped = gateway.map(port, protocol);
          if (mapped) {
            LOGGER.debug("Port {} mapped with success", port);
          }
        }
      } catch (final IllegalStateException e) {
        LOGGER.debug(DISCOVERY_PROCESS_INTERRUPTED, e);
      }
    });
  }


  /**
   * Closes the port on the router for the specified port number using the configured gateway.
   *
   * @param port The port number to close.
   */
  public void closePort(final int port) {
    if (null != gateway) {
      gateway.unmap(port, protocol);
      LOGGER.debug("Port {} unmapped with success", port);
    }
  }

  private void setGateway(final Gateway gateway) {
    this.gateway = gateway;
  }

  /**
   * Closes the {@link PortMapper}, shutting down the executor and closing the discovery instance.
   */
  @Override
  public void close() {
    discovery.close();
    executor.shutdownNow();
  }
}
