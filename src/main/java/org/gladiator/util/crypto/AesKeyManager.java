package org.gladiator.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;
import java.util.random.RandomGenerator;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.gladiator.exception.EndApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages AES encryption and decryption operations.
 */
public final class AesKeyManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AesKeyManager.class);

  private static final String ALGORITHM = "AES";
  private static final String PADDING = "/GCM/NoPadding";
  private static final int KEY_SIZE = 256;
  private static final int GCM_INIT_VECTOR_SIZE = 12;
  private static final int GCM_SPEC_SIZE = 128;

  private final RandomGenerator random = new SecureRandom();
  private final SecretKey key;

  /**
   * Constructs an AesKeyManager with the specified key and cipher.
   *
   * @param key The AES secret key.
   */
  private AesKeyManager(final SecretKey key) {
    this.key = key;
  }

  /**
   * Creates a new AesKeyManager instance.
   *
   * @return A new AesKeyManager instance.
   * @throws EndApplicationException If an error occurs during creation.
   */
  static AesKeyManager create() throws EndApplicationException {
    try {
      final KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(KEY_SIZE);
      final SecretKey aesKey = keyGenerator.generateKey();
      return new AesKeyManager(aesKey);
    } catch (final NoSuchAlgorithmException e) {
      throw new EndApplicationException(e);
    }
  }

  private Cipher createCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
    return Cipher.getInstance(ALGORITHM + PADDING);
  }

  /**
   * Encrypts the given message using the specified AES key.
   *
   * <p>It uses GCM, so, after encrypting the message, it needs to add an initialization Vector
   * (IV) to follow the GCM pattern and be really more secure and faster than some other
   * cryptography modes, the IV is just a group of random bytes that are placed before the encrypted
   * message
   * </p>
   *
   * <p>It also needs to encode the message with Base64 before sending it via Socket, because, if
   * not, the message can reach the other end altered and not in the same way it was sent
   * </p>
   *
   * @param aesKey  The AES secret key.
   * @param message The message to be encrypted.
   * @return The encrypted message as a Base64 encoded string.
   */
  public String encrypt(final SecretKey aesKey, final String message) {
    try {
      final byte[] iv = createIv();
      final Cipher cipher = createCipher();

      final AlgorithmParameterSpec parameterSpec = new GCMParameterSpec(GCM_SPEC_SIZE, iv);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);

      final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
      final byte[] encryptedMessageBytes = cipher.doFinal(messageBytes);
      final byte[] encryptedMessageBytesWithIv = addIvToMessage(iv, encryptedMessageBytes);

      return Base64.getEncoder().encodeToString(encryptedMessageBytesWithIv);
    } catch (final InvalidKeyException | IllegalBlockSizeException | BadPaddingException
                   | InvalidAlgorithmParameterException | NoSuchPaddingException
                   | NoSuchAlgorithmException e) {
      return handleException("Error during a AES encrypt with message: (" + message + ")", e,
          Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8)));
    }
  }

  /**
   * Decrypts the given message using the specified AES key.
   *
   * <p>This method first decodes de Base64 message and extracts the initialization vector (IV)
   * from the encrypted message to decrypt it correctly. The IV is necessary to follow the GCM
   * pattern used during encryption.</p>
   *
   * @param aesKey  The AES secret key.
   * @param message The message to be decrypted.
   * @return The decrypted message as a string.
   */
  public String decrypt(final SecretKey aesKey, final String message) {
    try {
      final byte[] encryptedMessageBytes = message.getBytes(StandardCharsets.UTF_8);
      final byte[] decodedMessageBytes = Base64.getDecoder().decode(encryptedMessageBytes);

      final byte[] iv = getIvFromMessage(decodedMessageBytes);

      final Cipher cipher = createCipher();
      final AlgorithmParameterSpec paramSpec = new GCMParameterSpec(GCM_SPEC_SIZE, iv);
      cipher.init(Cipher.DECRYPT_MODE, aesKey, paramSpec);

      final byte[] decryptedMessageBytes = cipher.doFinal(decodedMessageBytes,
          GCM_INIT_VECTOR_SIZE, decodedMessageBytes.length - GCM_INIT_VECTOR_SIZE);

      return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
    } catch (final InvalidKeyException | InvalidAlgorithmParameterException
                   | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException
                   | NoSuchAlgorithmException e) {
      return handleException("Error during a AES decrypt with message: " + message, e,
          new String(Base64.getDecoder().decode(message), StandardCharsets.UTF_8));
    }
  }

  /**
   * Handles exceptions by logging the error and returning the specified message.
   *
   * @param <T>           The type of the return message.
   * @param errorWarning  The error warning message.
   * @param ex            The exception that occurred.
   * @param returnMessage The message to be returned.
   * @return The specified return message.
   */
  private <T> T handleException(final String errorWarning, final Throwable ex,
      final T returnMessage) {
    LOGGER.error(errorWarning, ex);
    return returnMessage;
  }

  /**
   * Extracts the initialization vector (IV) from the given message bytes.
   *
   * @param messageBytes The message bytes.
   * @return The extracted IV.
   */
  private byte[] getIvFromMessage(final byte[] messageBytes) {
    final byte[] iv = new byte[GCM_INIT_VECTOR_SIZE];
    System.arraycopy(messageBytes, 0, iv, 0, iv.length);
    return iv;
  }

  /**
   * Creates a new initialization vector (IV).
   *
   * @return The created IV.
   */
  private byte[] createIv() {
    final byte[] iv = new byte[GCM_INIT_VECTOR_SIZE];
    random.nextBytes(iv);
    return iv;
  }

  /**
   * Adds the initialization vector (IV) to the given message bytes.
   *
   * @param iv           The initialization vector.
   * @param messageBytes The message bytes.
   * @return The message bytes with the IV prepended.
   */
  private byte[] addIvToMessage(final byte[] iv, final byte[] messageBytes) {
    final byte[] result = new byte[iv.length + messageBytes.length];
    System.arraycopy(iv, 0, result, 0, iv.length);
    System.arraycopy(messageBytes, 0, result, iv.length, messageBytes.length);
    return result;
  }

  /**
   * Gets the AES secret key.
   *
   * @return The AES secret key.
   */
  public SecretKey getKey() {
    return key;
  }
}