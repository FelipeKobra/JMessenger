package main.exchange;

import main.client.DesignClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DefaultClientExchangeNamesStrategy implements ExchangeNamesStrategy {

    private final String clientName;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Executor executor;

    public DefaultClientExchangeNamesStrategy(String clientName, BufferedReader reader, PrintWriter writer, Executor executor) {
        this.clientName = clientName;
        this.reader = reader;
        this.writer = writer;
        this.executor = executor;
    }

    @Override
    public void sendName() {
        writer.println(clientName);
        String logMessage = "Sent name (" + clientName + ") to server.";
        DesignClient.getLOGGER().fine(logMessage);
    }

    @Override
    public String receiveName() {
        String serverName = "";
        try {
            serverName = reader.readLine();
            String logMessage = "Received name from server: " + serverName + ".";
            DesignClient.getLOGGER().fine(logMessage);
        } catch (IOException e) {
            DesignClient.getLOGGER().warning("Error receiving server name: " + e);
        }
        return serverName;
    }

    @Override
    public String exchange() {
        final var sendNameFuture = CompletableFuture.runAsync(this::sendName, executor);
        final var receiveNameFuture = CompletableFuture.supplyAsync(this::receiveName, executor);

        CompletableFuture.allOf(sendNameFuture, receiveNameFuture).join();

        return receiveNameFuture.join();
    }
}
