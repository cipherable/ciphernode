package io.cipherable.server.aws;

import io.cipherable.server.CipherableException;

public class KmsOperationException extends CipherableException {

  public KmsOperationException(Throwable cause) {
    super(cause);
  }

  public KmsOperationException(String message) {
    super(message);
  }
}
