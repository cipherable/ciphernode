package io.cipherable.server.crypto;

import com.google.common.primitives.Ints;
import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.AEAD;
import io.cipherable.server.models.CipherText;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SodiumCryptographyService {
  private final LazySodiumJava lazySodium;

  public SodiumCryptographyService() {
    lazySodium = new LazySodiumJava(new SodiumJava());
  }


  public CipherText encrypt(byte[] messageBytes, byte[] dataKeyBytes, int nonce)
      throws CryptographyOperationException {
    try {
      byte[] nonceBytes = Ints.toByteArray(nonce);
      byte[] cipherBytes = new byte[messageBytes.length + AEAD.AES256GCM_ABYTES];
      if (lazySodium.cryptoAeadAES256GCMEncrypt(
          cipherBytes,
          null,
          messageBytes,
          messageBytes.length,
          null,
          0,
          null,
          nonceBytes,
          dataKeyBytes)) {
        return new CipherText(nonceBytes, cipherBytes);
      } else {
        throw new Exception();
      }
    } catch (Exception ex) {
      throw new CryptographyOperationException("unable to perform encryption", ex);
    }
  }


  public byte[] decrypt(byte[] cipherBytes, byte[] key, byte[] nonce)
      throws CryptographyOperationException {
    try {
      byte[] messageBytes = new byte[cipherBytes.length - AEAD.AES256GCM_ABYTES];
      if (lazySodium.cryptoAeadAES256GCMDecrypt(
          messageBytes,
          null,
          null,
          cipherBytes,
          cipherBytes.length,
          null,
          0,
          nonce,
          key)) {
        return messageBytes;
      } else {
        throw new Exception();
      }
    } catch (Exception ex) {
      throw new CryptographyOperationException("encryption exception", ex);
    }
  }
}
