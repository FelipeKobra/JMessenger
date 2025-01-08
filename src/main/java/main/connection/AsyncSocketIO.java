package main.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public final class AsyncSocketIO {
    private static final Logger logger = Logger.getLogger(AsyncSocketIO.class.getName());
    private final InputStream inputStream;
    private final OutputStream outputStream;

    private AsyncSocketIO(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = Objects.requireNonNull(inputStream);
        this.outputStream = Objects.requireNonNull(outputStream);
    }

    public static AsyncSocketIO getAsyncSocketIO(Socket clientSocket, Executor executor) {
        final var clientInputStreamFuture = CompletableFuture.supplyAsync(() ->
            createInputStream(clientSocket), executor);

        final var clientOutputStreamFuture = CompletableFuture.supplyAsync(() ->
            createOutputStream(clientSocket), executor);

        CompletableFuture.allOf(clientInputStreamFuture, clientOutputStreamFuture).join();

        AsyncSocketIO asyncSocketIO = null;
        try {
            asyncSocketIO = new AsyncSocketIO(
                clientInputStreamFuture.get(),
                clientOutputStreamFuture.get());
        } catch (InterruptedException e) {
            logger.warning("The current thread was interrupted while waiting: " + e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.fine("this future completed exceptionally: " + e);
        }

        return asyncSocketIO;
    }

    private static InputStream createInputStream(Socket clientSocket) {
        InputStream clientInputStream = null;
        try {
            clientInputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            logger.finer("Input Stream closed: " + e);
        }
        return clientInputStream;
    }

    private static OutputStream createOutputStream(Socket clientSocket) {
        OutputStream clientOutputStream = null;
        try {
            clientOutputStream = clientSocket.getOutputStream();
        } catch (IOException e) {
            logger.finer("Output Stream closed: " + e);
        }
        return clientOutputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}