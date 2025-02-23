package org.gladiator.util.crypto;

import java.security.Key;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import org.gladiator.exception.EndApplicationException;

public final class CryptographyManager {

  private final AesKeyManager aesKeyManager;
  private final RsaKeysManager rsaKeysManager;

  private CryptographyManager(final AesKeyManager aesKeyManager,
      final RsaKeysManager rsaKeysManager) {
    this.aesKeyManager = aesKeyManager;
    this.rsaKeysManager = rsaKeysManager;
  }

  public static CryptographyManager create()
      throws EndApplicationException {
    final AesKeyManager aesKeyManager = AesKeyManager.create();
    final RsaKeysManager rsaKeysManager = RsaKeysManager.create();

    return new CryptographyManager(aesKeyManager, rsaKeysManager);
  }


  public String encrypt(final SecretKey aesKey, final String message) {
    return aesKeyManager.encrypt(aesKey, message);
  }

  public String decrypt(final SecretKey aesKey, final String message) {
    return aesKeyManager.decrypt(aesKey, message);
  }

  public String encryptRsa(final PublicKey publicKey, final Key aesKey) {
    return rsaKeysManager.encrypt(publicKey, aesKey);
  }

  public SecretKey decryptRsa(final String aesKeyAsString) {
    return rsaKeysManager.decrypt(aesKeyAsString);
  }


  public SecretKey getAesKey() {
    return aesKeyManager.getKey();
  }

  public Key getRsaPublicKey() {
    return rsaKeysManager.getPublicKey();
  }

}
