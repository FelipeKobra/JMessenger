package app.client;

import app.util.SingletonTerminal;
import environment.Port;
import org.apache.commons.lang3.Validate;
import org.jline.reader.UserInterruptException;

public class ClientConfig {
    private final String name;
    private final String serverAddress;
    private final int port;

    private ClientConfig(String name, String serverAddress, int port) {
        this.name = name;
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public static ClientConfig createClientConfig() {
        final String clientName = receiveName();
        final String serverAddress = receiveAddress();
        final int serverPort = receivePort();

        return new ClientConfig(clientName, serverAddress, serverPort);
    }

    private static String receiveName() throws UserInterruptException {
        String name = "";
        while (name.isBlank()) {
            name = SingletonTerminal.TERMINAL.getLineReader().readLine("Choose your name: ");
        }
        return name;
    }

    private static String receiveAddress() throws UserInterruptException {
        return SingletonTerminal.TERMINAL.getLineReader().readLine("Type the Server IP (default = localhost): ");
    }

    private static int receivePort() throws UserInterruptException {
        int serverPort;
        try {
            String serverPortString = SingletonTerminal.TERMINAL.getLineReader().readLine("Type the server port  (default = " + Port.PORT_DEFAULT + "): ");
            if (serverPortString.isBlank()) {
                serverPort = Port.PORT_DEFAULT;
            } else {
                serverPort = Integer.parseInt(serverPortString);
            }

            Validate.inclusiveBetween(Port.PORT_MIN, Port.PORT_MAX, serverPort);
        } catch (IllegalArgumentException e) {
            Client.getLogger().error("Invalid Port, using default " + Port.PORT_DEFAULT);
            serverPort = Port.PORT_DEFAULT;
        }

        return serverPort;
    }

    public String getName() {
        return name;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getPort() {
        return port;
    }
}
