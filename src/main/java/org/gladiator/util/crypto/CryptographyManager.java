package org.gladiator.util.crypto;

import java.security.Key;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import org.gladiator.exception.EndApplicationException;

/**
 * Manages cryptographic operations using AES and RSA algorithms.
 */
public final class CryptographyManager {

  private final AesKeyManager aesKeyManager;
  private final RsaKeysManager rsaKeysManager;

  /**
   * Constructs a CryptographyManager with the specified AES and RSA key managers.
   *
   * @param aesKeyManager  The AES key manager.
   * @param rsaKeysManager The RSA key manager.
   */
  private CryptographyManager(final AesKeyManager aesKeyManager,
      final RsaKeysManager rsaKeysManager) {
    this.aesKeyManager = aesKeyManager;
    this.rsaKeysManager = rsaKeysManager;
  }

  /**
   * Creates a new CryptographyManager instance.
   *
   * @return A new CryptographyManager instance.
   * @throws EndApplicationException If an error occurs during creation.
   */
  public static CryptographyManager create()
      throws EndApplicationException {
    final AesKeyManager aesKeyManager = AesKeyManager.create();
    final RsaKeysManager rsaKeysManager = RsaKeysManager.create();

    return new CryptographyManager(aesKeyManager, rsaKeysManager);
  }

  /**
   * Encrypts the given message using the specified AES key.
   *
   * @param aesKey  The AES secret key.
   * @param message The message to be encrypted.
   * @return The encrypted message as a Base64 encoded string.
   */
  public String encrypt(final SecretKey aesKey, final String message) {
    return aesKeyManager.encrypt(aesKey, message);
  }

  /**
   * Decrypts the given message using the specified AES key.
   *
   * @param aesKey  The AES secret key.
   * @param message The message to be decrypted.
   * @return The decrypted message as a string.
   */
  public String decrypt(final SecretKey aesKey, final String message) {
    return aesKeyManager.decrypt(aesKey, message);
  }

  /**
   * Encrypts the given AES key using the specified RSA public key.
   *
   * @param publicKey The RSA public key.
   * @param aesKey    The AES secret key.
   * @return The encrypted AES key as a Base64 encoded string.
   */
  public String encryptRsa(final PublicKey publicKey, final Key aesKey) {
    return rsaKeysManager.encrypt(publicKey, aesKey);
  }

  /**
   * Decrypts the given AES key string with an RSA private key.
   *
   * @param aesKeyAsString The AES key as a Base64 encoded string.
   * @return The decrypted AES secret key.
   */
  public SecretKey decryptRsa(final String aesKeyAsString) {
    return rsaKeysManager.decrypt(aesKeyAsString);
  }

  /**
   * Gets the AES secret key.
   *
   * @return The AES secret key.
   */
  public SecretKey getAesKey() {
    return aesKeyManager.getKey();
  }

  /**
   * Gets the RSA public key.
   *
   * @return The RSA public key.
   */
  public Key getRsaPublicKey() {
    return rsaKeysManager.getPublicKey();
  }

}