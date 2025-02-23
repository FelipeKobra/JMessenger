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

public final class AesKeyManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AesKeyManager.class);

  private static final String ALGORITHM = "AES";
  private static final String PADDING = "/GCM/NoPadding";
  private static final int KEY_SIZE = 256;
  private static final int GCM_INIT_VECTOR_SIZE = 12;
  private static final int GCM_SPEC_SIZE = 128;


  private final RandomGenerator random = new SecureRandom();
  private final Cipher cipher;
  private final SecretKey key;

  private AesKeyManager(final SecretKey key, final Cipher cipher) {
    this.key = key;
    this.cipher = cipher;
  }


  static AesKeyManager create() throws EndApplicationException {
    try {
      final Cipher cipher = Cipher.getInstance(ALGORITHM + PADDING);

      final KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(KEY_SIZE);
      final SecretKey aesKey = keyGenerator.generateKey();
      return new AesKeyManager(aesKey, cipher);
    } catch (final NoSuchAlgorithmException e) {
      throw new EndApplicationException(
          "The algorithm: " + ALGORITHM + "is not valid.", e);
    } catch (final NoSuchPaddingException e) {
      throw new EndApplicationException("Error during cipher padding creation", e);
    }
  }


  public String encrypt(final SecretKey aesKey, final String message) {
    try {
      final byte[] iv = createIv();

      final AlgorithmParameterSpec parameterSpec = new GCMParameterSpec(GCM_SPEC_SIZE, iv);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);

      final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
      final byte[] encryptedMessageBytes = cipher.doFinal(messageBytes);
      final byte[] encryptedMessageBytesWithIv = addIvToMessage(iv, encryptedMessageBytes);

      return Base64.getEncoder().encodeToString(encryptedMessageBytesWithIv);
    } catch (final InvalidKeyException e) {
      return handleException("Invalid key used for AES encrypting", e, message);
    } catch (final IllegalBlockSizeException e) {
      return handleException("Invalid block size detected during AES encrypting", e,
          message);
    } catch (final BadPaddingException e) {
      return handleException("Invalid padding detected during AES encrypting", e, message);
    } catch (final InvalidAlgorithmParameterException e) {
      return handleException("Invalid parameter spec during AES encrypting", e, message);
    }
  }

  public String decrypt(final SecretKey aesKey, final String message) {
    try {
      final byte[] encryptedMessageBytes = message.getBytes(StandardCharsets.UTF_8);
      final byte[] decodedMessageBytes = Base64.getDecoder().decode(encryptedMessageBytes);

      final byte[] iv = getIvFromMessage(decodedMessageBytes);

      final AlgorithmParameterSpec paramSpec = new GCMParameterSpec(GCM_SPEC_SIZE, iv);
      cipher.init(Cipher.DECRYPT_MODE, aesKey, paramSpec);

      final byte[] decryptedMessageBytes = cipher.doFinal(decodedMessageBytes,
          GCM_INIT_VECTOR_SIZE, decodedMessageBytes.length - GCM_INIT_VECTOR_SIZE);

      return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
    } catch (final InvalidKeyException e) {
      return handleException("Invalid key used for AES decrypting", e, message);
    } catch (final IllegalBlockSizeException e) {
      return handleException("Invalid block size detected during AES decrypting", e,
          message);
    } catch (final BadPaddingException e) {
      return handleException("Invalid padding detected during AES decrypting", e, message);
    } catch (final InvalidAlgorithmParameterException e) {
      return handleException("Invalid parameter spec during AES decrypting", e, message);
    }
  }

  private <T> T handleException(final String errorWarning, final Throwable ex,
      final T returnMessage) {
    LOGGER.error(errorWarning, ex);
    return returnMessage;
  }

  private byte[] getIvFromMessage(final byte[] messageBytes) {
    final byte[] iv = new byte[GCM_INIT_VECTOR_SIZE];
    System.arraycopy(messageBytes, 0, iv, 0, iv.length);
    return iv;
  }

  private byte[] createIv() {
    final byte[] iv = new byte[GCM_INIT_VECTOR_SIZE];
    random.nextBytes(iv);
    return iv;
  }

  private byte[] addIvToMessage(final byte[] iv, final byte[] messageBytes) {
    final byte[] result = new byte[iv.length + messageBytes.length];
    System.arraycopy(iv, 0, result, 0, iv.length);
    System.arraycopy(messageBytes, 0, result, iv.length, messageBytes.length);
    return result;
  }

  public SecretKey getKey() {
    return key;
  }


}
