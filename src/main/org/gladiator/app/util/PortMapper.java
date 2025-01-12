package app.util;

import com.sshtools.porter.UPnP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PortMapper implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(PortMapper.class);
    private final static UPnP.Protocol protocol = UPnP.Protocol.TCP;

    private final ExecutorService executor;
    private final UPnP.Discovery discovery;
    private UPnP.Gateway gateway;

    public PortMapper(ExecutorService executor, UPnP.Discovery discovery) {
        this.executor = executor;
        this.discovery = discovery;
    }

    public static PortMapper createDefault() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        UPnP.Discovery discovery = new UPnP.DiscoveryBuilder().withSoTimeout(600)
            .withoutShutdownHooks()
            .onGateway(gw -> LOGGER.debug("Gateway found {}", gw.ip()))
            .build();

        PortMapper portMapper = new PortMapper(executor, discovery);

        executor.execute(() -> portMapper.setGateway(discovery.gateway().orElse(null)));

        return portMapper;
    }

    public void openPort(int port) {
        executor.execute(() -> {
            if (gateway == null) {
                discovery.awaitCompletion(5, TimeUnit.SECONDS);
            }
            if (gateway != null) {
                boolean mapped = gateway.map(port, protocol);
                if (mapped) {
                    LOGGER.debug("Port {} mapped with success", port);
                }
            }
        });
    }

    public void closePort(int port) {
        if (gateway != null) {
            gateway.unmap(port, protocol);
            LOGGER.debug("Port {} unmapped with success", port);
        }
    }

    public void setGateway(UPnP.Gateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void close() {
        executor.shutdown();
        discovery.close();
    }
}
