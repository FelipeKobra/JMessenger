package main.client;

import main.chat.CustomTerminal;
import main.connection.AsyncSocketIO;
import main.connection.DesignMessage;
import main.exchange.DefaultClientExchangeNamesStrategy;
import main.exchange.ExchangeNamesStrategy;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import javax.net.SocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class DesignClient implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(DesignClient.class.getName());
    private final CustomTerminal terminal;
    private final DefaultClientConfig config;
    private final Socket socket;
    private final ExecutorService executor;

    private DesignClient(DefaultClientConfig config, Socket socket, ExecutorService executor, CustomTerminal terminal) {
        this.config = config;
        this.socket = socket;
        this.executor = executor;
        this.terminal = terminal;
    }

    public static void runClient(DefaultClientConfig config) {
        try (final var socket = SocketFactory.getDefault().createSocket(config.getHostname(), config.getPort());
             final var terminal = config.getCustomTerminal();
             final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             final var client = new DesignClient(config, socket, executor, terminal)
        ) {
            client.run();
        } catch (UnknownHostException e) {
            LOGGER.warning("Server not found: " + e);
        } catch (IOException e) {
            LOGGER.warning("Error creating socket or terminal instances " + e);
        }
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    private void reconnectPrompt() {
        System.out.println(("Connect to another server? (Y/N)"));

        boolean isChosen = false;
        int choiceInt;

        try (Reader reader = terminal.getTerminal().reader()) {
            while (!isChosen && (choiceInt = reader.read()) != -1) {

                switch (Character.toUpperCase((char) choiceInt)) {
                    case 'Y', 'S' -> {
                        close();
                        new Thread(ClientMain::main).start();
                        isChosen = true;
                    }
                    case 'N' -> {
                        System.out.println(("Chat Ended"));
                        isChosen = true;
                    }
                    default -> System.out.println(("Type `Y` or `N`"));
                }
            }
        } catch (UserInterruptException | EndOfFileException e) {
            LOGGER.fine("Reconnection choice not made: " + e);
        } catch (IOException e) {
            LOGGER.warning("Error during the reading of choice operation: " + e);
        }
    }

    private CompletableFuture<Void> receiveMessages(BufferedReader reader) {
        return CompletableFuture.runAsync(() -> {
            try {
                reader.lines()
                    .map(msg -> {
                        msg = new String(msg.getBytes(), StandardCharsets.UTF_8);
                        return DesignMessage.fromRawString(msg);
                    })
                    .forEach(msg -> {
                        config.getChat().showConnectionMessage(msg);
                        config.getChat().showUserPrompt();
                        System.out.print(terminal.getLineReader().getBuffer());
                    });
            } catch (UncheckedIOException e) {
                LOGGER.fine("The connection with the server has ended");
            }
        });
    }

    private CompletableFuture<Void> sendMessages(PrintWriter writer) {
        return CompletableFuture.runAsync(() -> {
            String line;
            try {
                while ((line = terminal.getLineReader().readLine(config.getChat().userPrompt() + " ")) != null) {
                    if (line.equals("quit")) {
                        throw new UserInterruptException("User typed `quit`");
                    }
                    String msg = DesignMessage.toRawString(config.getName(), line);
                    writer.println(msg);
                }
            } catch (EndOfFileException | UserInterruptException e) {
                LOGGER.fine("User stopped the console reading, probably by pressing CTRL + C");
                try {
                    socket.close();
                } catch (IOException ex) {
                    LOGGER.warning("Error during socket closing after shutdown of sending message: " + e);
                }
            }
        });
    }

    private void run() {
        final var socketIO = AsyncSocketIO.getAsyncSocketIO(socket, executor);

        final var reader = new BufferedReader(new InputStreamReader(socketIO.getInputStream()));
        final var writer = new PrintWriter(socketIO.getOutputStream(), true, StandardCharsets.UTF_8);

        final ExchangeNamesStrategy exchangeNamesStrategy = new DefaultClientExchangeNamesStrategy(config.getName(), reader, writer, executor);
        exchangeNamesStrategy.exchange();

        System.out.println("Connection Established");
        System.out.println("Press `quit` to exit");
        CompletableFuture.anyOf(receiveMessages(reader), sendMessages(writer)).join();
        reconnectPrompt();
    }

    @Override
    public void close() {
        LOGGER.fine("Closing connection");
        try {
            terminal.close();
            socket.close();
            executor.shutdown();
        } catch (IOException e) {
            LOGGER.warning("Error closing the socket connection or the terminal: " + e);
        }
    }
}
