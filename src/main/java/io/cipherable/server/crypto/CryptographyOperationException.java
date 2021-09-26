package io.cipherable.server.crypto;

import io.cipherable.server.CipherableException;

public class CryptographyOperationException extends CipherableException {

  public CryptographyOperationException(String message) {
    super(message);
  }

  public CryptographyOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  public CryptographyOperationException(Throwable cause) {
    super(cause);
  }
}
