package main.exchange;

import main.server.DesignServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DefaultServerExchangeNamesStrategy implements ExchangeNamesStrategy {
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final String serverName;
    private final Executor executor;

    public DefaultServerExchangeNamesStrategy(BufferedReader reader, PrintWriter writer, String serverName, Executor executor) {
        this.reader = reader;
        this.writer = writer;
        this.serverName = serverName;
        this.executor = executor;
    }

    @Override
    public void sendName() {
        writer.println(serverName);
        String logMessage = "Sent name " +
            serverName +
            " to client.";
        DesignServer.getLOGGER().fine(logMessage);
    }

    @Override
    public String receiveName() {
        String clientName = "";
        try {
            clientName = reader.readLine();
            String logMessage = "Received name from client: " + clientName + ".";
            DesignServer.getLOGGER().fine(logMessage);
            return clientName;
        } catch (IOException e) {
            DesignServer.getLOGGER().warning("Error receiving client name: " + e);
        }

        return clientName;
    }

    @Override
    public String exchange() {
        final var sendServerNameFuture =
            CompletableFuture
                .runAsync(this::sendName, executor);
        final var clientNameFuture = CompletableFuture
            .supplyAsync(this::receiveName, executor);

        CompletableFuture.allOf(sendServerNameFuture, clientNameFuture).join();

        return clientNameFuture.join();
    }
}
