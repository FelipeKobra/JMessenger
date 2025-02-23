package org.gladiator.util.crypto;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.gladiator.exception.EndApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RsaKeysManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(RsaKeysManager.class);

  private static final String ALGORITHM = "RSA";
  private static final String PADDING = "/ECB/OAEPWithSHA-512AndMGF1Padding";
  private static final int KEY_SIZE = 2048;

  private final PrivateKey privateKey;
  private final PublicKey publicKey;
  private final Cipher cipher;

  private RsaKeysManager(final PrivateKey privateKey, final PublicKey publicKey,
      final Cipher cipher) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.cipher = cipher;
  }

  static RsaKeysManager create() throws EndApplicationException {
    final KeyPairGenerator kpg;
    try {
      kpg = KeyPairGenerator.getInstance(ALGORITHM);
      kpg.initialize(KEY_SIZE);
      final KeyPair kp = kpg.generateKeyPair();
      final Cipher cipher = Cipher.getInstance(ALGORITHM + PADDING);
      return new RsaKeysManager(kp.getPrivate(), kp.getPublic(), cipher);
    } catch (final NoSuchAlgorithmException e) {
      throw new EndApplicationException(
          "The algorithm: " + ALGORITHM + "is not valid.", e);
    } catch (final NoSuchPaddingException e) {
      throw new EndApplicationException("Error during cipher padding creation", e);
    }
  }


  public PublicKey getPublicKey() {
    return publicKey;
  }


  public String encrypt(final PublicKey publicKey, final Key key) {
    final byte[] keyBytes = key.getEncoded();
    final String notEncryptedKeyString = Base64.getEncoder().encodeToString(keyBytes);
    try {
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      final byte[] encryptedKeyBytes = cipher.doFinal(keyBytes);
      return Base64.getEncoder().encodeToString(encryptedKeyBytes);
    } catch (final InvalidKeyException e) {
      return handleException("Invalid key used for RSA encrypting", e, notEncryptedKeyString);
    } catch (final IllegalBlockSizeException e) {
      return handleException("Invalid block size detected during RSA encrypting", e,
          notEncryptedKeyString);
    } catch (final BadPaddingException e) {
      return handleException("Invalid padding detected during RSA encrypting", e,
          notEncryptedKeyString);
    }
  }


  public SecretKey decrypt(final String keyAsString) {
    final byte[] decodedKeyBytes = Base64.getDecoder().decode(keyAsString);
    final SecretKey encryptedKey = new SecretKeySpec(decodedKeyBytes, "AES");
    try {
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
      final byte[] decryptedKeyBytes = cipher.doFinal(decodedKeyBytes);
      return new SecretKeySpec(decryptedKeyBytes, "AES");
    } catch (final InvalidKeyException e) {
      return handleException("Invalid keyAsString used for RSA decrypting", e, encryptedKey);
    } catch (final IllegalBlockSizeException e) {
      return handleException("Invalid block size detected during RSA decrypting", e,
          encryptedKey);
    } catch (final BadPaddingException e) {
      return handleException("Invalid padding detected during RSA decrypting", e,
          encryptedKey);
    }
  }

  private <T> T handleException(final String errorWarning, final Throwable ex,
      final T returnMessage) {
    LOGGER.error(errorWarning, ex);
    return returnMessage;
  }

}
