package org.gladiator.util.connection.exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.crypto.SecretKey;
import org.gladiator.exception.FailedExchangeException;
import org.gladiator.util.connection.IoUtils;
import org.gladiator.util.crypto.CryptographyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the exchange of names between two endpoints over a socket connection.
 */
public class NameExchange {

  private static final Logger LOGGER = LoggerFactory.getLogger(NameExchange.class);

  private final Socket socket;
  private final SecretKey aesKey;
  private final CryptographyManager cryptographyManager;
  private final String ownName;
  private final ExecutorService executor;

  /**
   * Constructs a NameExchange with the specified parameters.
   *
   * @param socket              The socket for the exchange.
   * @param aesKey              The AES secret key for encryption/decryption.
   * @param cryptographyManager The manager for cryptographic operations.
   * @param ownName             The name to be sent to the other endpoint.
   * @param executor            The executor service for asynchronous operations.
   */
  public NameExchange(final Socket socket, final SecretKey aesKey,
      final CryptographyManager cryptographyManager, final String ownName,
      final ExecutorService executor) {
    this.socket = Objects.requireNonNull(socket,
        "The socket of the name exchange must not be null");
    this.aesKey = aesKey;
    this.cryptographyManager = cryptographyManager;
    this.ownName = ownName;
    this.executor = executor;
  }

  /**
   * Performs the name exchange operation.
   *
   * @return The name received from the other endpoint.
   * @throws FailedExchangeException If an error occurs during the exchange.
   */
  public String exchange() throws FailedExchangeException {
    try {
      final PrintWriter writer = IoUtils.createWriter(socket);
      final BufferedReader reader = IoUtils.createReader(socket);

      final CompletableFuture<Void> sendOwnNameFuture = CompletableFuture.runAsync(
          () -> sendName(writer), executor);

      final CompletableFuture<String> receiveNameFuture = CompletableFuture.supplyAsync(
          () -> receiveName(reader), executor);

      CompletableFuture.allOf(sendOwnNameFuture, receiveNameFuture).join();
      return receiveNameFuture.join();

    } catch (final IOException | UncheckedIOException e) {
      throw new FailedExchangeException(e);
    }
  }

  /**
   * Sends the own name to the other endpoint.
   *
   * @param writer The PrintWriter to send the name.
   */
  private void sendName(final PrintWriter writer) {
    final String encryptedOwnName = cryptographyManager.encrypt(aesKey,
        ownName);
    writer.println(encryptedOwnName);
    final String logMessage = "Sent name " + ownName;
    LOGGER.debug(logMessage);
  }

  /**
   * Receives the name from the other endpoint.
   *
   * @param reader The BufferedReader to read the name.
   * @return The decrypted name received from the other endpoint.
   */
  private String receiveName(final BufferedReader reader) {
    final String otherEndName;
    try {
      final String encryptedOtherEndName = Objects.requireNonNull(reader.readLine(),
          "Error during name receive, it is null");
      otherEndName = cryptographyManager.decrypt(aesKey, encryptedOtherEndName);
      final String logMessage = "Received name: " + otherEndName + ".";
      LOGGER.debug(logMessage);
    } catch (final IOException e) {
      LOGGER.error("Error receiving name");
      throw new UncheckedIOException(e);
    }
    return otherEndName;
  }
}