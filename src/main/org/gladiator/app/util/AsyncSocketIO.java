package app.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public final class AsyncSocketIO {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSocketIO.class.getName());
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
            LOGGER.error("The current thread was interrupted while waiting: {}", e, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.debug("this future completed exceptionally: {}", String.valueOf(e));
        }

        return asyncSocketIO;
    }

    private static InputStream createInputStream(Socket clientSocket) {
        InputStream clientInputStream = null;
        try {
            clientInputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            LOGGER.debug("Input Stream closed: {}", String.valueOf(e));
        }
        return clientInputStream;
    }

    private static OutputStream createOutputStream(Socket clientSocket) {
        OutputStream clientOutputStream = null;
        try {
            clientOutputStream = clientSocket.getOutputStream();
        } catch (IOException e) {
            LOGGER.debug("Output Stream closed: {}", String.valueOf(e));
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