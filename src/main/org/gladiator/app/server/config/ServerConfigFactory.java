package app.server.config;

import app.server.Server;
import app.util.SingletonTerminal;
import environment.Port;
import org.apache.commons.lang3.Validate;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;


public class ServerConfigFactory {

    public ServerConfig create() throws UserInterruptException {
        ServerConfig serverConfig;
        String isCustom;

        LineReader reader = SingletonTerminal.TERMINAL.getLineReader();

        isCustom = reader.readLine("Want to change the default settings? y/N: ");
        isCustom = isCustom.toUpperCase();
        serverConfig = isCustom.equals("Y") ? createCustom() : createDefault();

        return serverConfig;
    }

    private ServerConfig createCustom() {
        String serverName = "";
        int serverPort;

        try {
            LineReader lineReader = SingletonTerminal.TERMINAL.getLineReader();
            System.out.println("Leave the field blank for the default setting");

            serverName = getCustomConfig(lineReader, "Server Name", ServerConfig.getDefaultName());
            serverPort = Integer.parseInt(getCustomConfig(lineReader, "Server Port", String.valueOf(ServerConfig.getDefaultPort())));
            Validate.inclusiveBetween(Port.PORT_MIN, Port.PORT_MAX, serverPort);

        } catch (IllegalArgumentException e) {
            Server.getLogger().error("Invalid port number provided. Using default port.");
            serverPort = ServerConfig.getDefaultPort();
        }

        return new ServerConfig(serverName, serverPort);
    }

    private ServerConfig createDefault() {
        return new ServerConfig();
    }

    private String getCustomConfig(LineReader reader, String configName, String defaultConfig) {
        String config = reader.readLine(configName + " (default = " + defaultConfig + "): ");
        if (config.isBlank()) {
            config = defaultConfig;
        }
        return config;
    }
}
